package com.showtime;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import showtime.ShowtimeClient;

import java.util.ArrayList;
import java.util.HashMap;

public class ShowtimeBitwigExtension extends ControllerExtension
{
   // Showtime fields
   private ShowtimeClient client;

   // Bitwig API objects
   private Preferences mHostPreferences = null;

   // Bitwig proxy objects
   private Transport mTransport;
   private TrackBank mTrackBank;
   private SceneBank mSceneBank;
   private CursorTrack mCursorTrack;

   private HashMap<Track, TrackWrapper> mTrackWrappers;
   private HashMap<Device, DeviceWrapper> mDeviceWrappers;
   private HashMap<Parameter, ParameterWrapper> mDeviceParameterWrappers;

   private EventLoop mLoop;

   // Constants
   public static int MAX_HARDWARE_ITEMS = 64;

   protected ShowtimeBitwigExtension(final ShowtimeBitwigExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
      mTrackWrappers = new HashMap<Track, TrackWrapper>();
      mDeviceWrappers = new HashMap<Device, DeviceWrapper>();
      mDeviceParameterWrappers = new HashMap<Parameter, ParameterWrapper>();
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();      

      mTransport = host.createTransport();
      mHostPreferences = host.getPreferences();

      //host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      //host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));
      
      // Settings
      final SettableStringValue serverAddress = mHostPreferences.getStringSetting("Server name to join", "Performance", 255, "LiveBridgeServer");
      final SettableStringValue clientName = mHostPreferences.getStringSetting("Client name", "Performance", 255, "ShowtimeBitwig");
      final SettableBooleanValue autoJoin = mHostPreferences.getBooleanSetting("Auto connect", "Performance", true);

      // Connect to the performance
      if(autoJoin.get()) {
         host.showPopupNotification("Attempting to connect to Showtime network");
         client = new ShowtimeClient();
         mLoop = new EventLoop(client, this);
         getHost().scheduleTask(mLoop::run, 5);

         client.init(clientName.get(), true);
         client.auto_join_by_name(serverAddress.get());
         host.showPopupNotification("Connected to server " + serverAddress.get() + ": " + client.is_connected());
      }

      // TODO: Perform your driver initialization here.
//      mCursorTrack = host.createCursorTrack(8, 0);
//      mCursorTrack.color().markInterested();
//      mCursorTrack.hasPrevious().markInterested();
//      mCursorTrack.hasNext().markInterested();
//      mCursorTrack.playingNotes().markInterested();
      createTrackBank();
      createSceneBank();

      // For now just show a popup notification for verification that it is running.
      //host.showPopupNotification("ShowtimeBitwig Initialized");
      //System.out.println("Showtime println from bitwig");
   }

   @Override
   public void exit()
   {
      //mLoop.interrupt();
//      try {
//         mLoop.join(10);
//      } catch (InterruptedException e) {
//         getHost().println(e.toString());
//      }

      if(client != null) {
         client.leave();
         client.destroy();
      }

      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("ShowtimeBitwig Exited");
   }

   @Override
   public void flush()
   {
      // TODO Send any updates you need here.
   }

   public void createTrackBank(){
      mTrackBank = getHost().createTrackBank(MAX_HARDWARE_ITEMS, MAX_HARDWARE_ITEMS, MAX_HARDWARE_ITEMS, true);
      for(int idx = 0; idx < mTrackBank.getSizeOfBank(); ++idx){
         Track proxyTrack = mTrackBank.getItemAt(idx);
         proxyTrack.name().markInterested();
         proxyTrack.exists().markInterested();
         createDeviceBank(proxyTrack);
      }

      mTrackBank.itemCount().addValueObserver(v -> {
         mTrackBank.setSizeOfBank(v);
         trackListChanged(mTrackBank);
      });
   }

   private void createSceneBank() {
      mSceneBank = mTrackBank.sceneBank();
      mSceneBank.itemCount().markInterested();
      for(int idx = 0; idx < mSceneBank.getSizeOfBank(); ++idx){
         Scene proxyScene = mSceneBank.getItemAt(idx);
         proxyScene.name().markInterested();
         proxyScene.exists().markInterested();
      }
      mSceneBank.itemCount().addValueObserver(newValue -> {
         sceneListChanged(mSceneBank);
      });
   }

   private void createDeviceBank(Track proxyTrack) {
      // Create bank to hold devices
      DeviceBank deviceBank = proxyTrack.createDeviceBank(MAX_HARDWARE_ITEMS);
      deviceBank.exists().markInterested();
      deviceBank.itemCount().markInterested();

      for(int idx = 0; idx < deviceBank.getSizeOfBank(); ++idx){
         Device proxyDevice = deviceBank.getDevice(idx);
         proxyDevice.name().markInterested();
         proxyDevice.deviceType().markInterested();

         createDeviceParameterPage(proxyDevice);
      }
      deviceBank.itemCount().addValueObserver(newValue -> deviceListChanged(proxyTrack, deviceBank));
   }

   private void createDeviceParameterPage(Device proxyDevice) {

      CursorRemoteControlsPage proxyDeviceParameters = proxyDevice.createCursorRemoteControlsPage("params1", MAX_HARDWARE_ITEMS, "");
      proxyDeviceParameters.pageCount().markInterested();
      proxyDeviceParameters.pageNames().markInterested();
      proxyDeviceParameters.selectedPageIndex().markInterested();
      proxyDeviceParameters.hasNext().markInterested();
      proxyDeviceParameters.hasPrevious().markInterested();
      proxyDevice.addDirectParameterIdObserver(params -> this.parameterListChanged(proxyDevice, proxyDeviceParameters, params));

      for(int idx = 0; idx < proxyDeviceParameters.getParameterCount(); ++idx){
         RemoteControl proxyParameter = proxyDeviceParameters.getParameter(idx);
         proxyParameter.name().markInterested();
         proxyParameter.exists().markInterested();
         proxyParameter.displayedValue().markInterested();
         proxyParameter.modulatedValue().markInterested();
         proxyParameter.value().markInterested();
         proxyParameter.value().addValueObserver(newValue -> parameterValueChanged(proxyDevice, proxyParameter));
      }
   }

   private void parameterListChanged(Device proxyDevice, CursorRemoteControlsPage proxyDeviceParameters, String[] params) {
//      for (final String pname : params) {
//         getHost().println("Direct param: " + pname);
//      }

      ArrayList<Parameter> currentParameters = new ArrayList<Parameter>();

      for(int idx = 0; idx < params.length; ++idx){
         Parameter param = proxyDeviceParameters.getParameter(idx);
         if(param.name().get().isEmpty())
            continue;

         if(!mDeviceParameterWrappers.containsKey(param)){
            // Create wrapper
            ParameterWrapper paramWrapper = new ParameterWrapper(this, param);
            mDeviceParameterWrappers.put(param, paramWrapper);

            // Add wrapper to showtime performance
            if(this.client != null){
               DeviceWrapper deviceWrapper = mDeviceWrappers.get(proxyDevice);
               if(deviceWrapper != null){
                  deviceWrapper.parameters.add_child(paramWrapper);
               }
            }

            currentParameters.add(param);
            getHost().println("Parameter wrapper '" + param.name().get() + "' added");
         }

         // Remove track wrappers that no longer have associated tracks
//         mDeviceWrappers.forEach((existingDevice, deviceWrapper) -> {
//            if(!currentDevices.contains(existingDevice)){
//               // Track has left
//               getHost().println("Device wrapper '" + existingDevice.name().get() + "' removed");
//               //mTrackWrappers.remove(existingDevice);
//               //deviceWrapper = null;
//            }
//         });
      }
   }

   private void parameterValueChanged(Device proxyDevice, RemoteControl proxyParameter) {
      getHost().println(proxyParameter.name().get() + " changed to " + proxyParameter.value().get());
      ParameterWrapper paramWrapper = mDeviceParameterWrappers.get(proxyParameter);
      if (paramWrapper != null){
         paramWrapper.output.append_float((float)proxyParameter.value().get());
         paramWrapper.output.fire();
      } else {
         getHost().println("Could't find wrapper for parameter " + proxyParameter.name().get());
      }
   }

   private void deviceListChanged(Track proxyTrack, DeviceBank bank) {
      ArrayList<Device> currentDevices = new ArrayList<Device>();

      for(int idx = 0; idx < bank.itemCount().getAsInt(); ++idx){
         Device device = bank.getItemAt(idx);

         if(!mDeviceWrappers.containsKey(device)){
            // Create wrapper
            DeviceWrapper deviceWrapper = new DeviceWrapper(this, device);
            mDeviceWrappers.put(device, deviceWrapper);

            // Add wrapper to showtime performance
            if(this.client != null){
               TrackWrapper trackWrapper = mTrackWrappers.get(proxyTrack);
               if(trackWrapper != null){
                  trackWrapper.devices.add_child(deviceWrapper);
               }
            }

            currentDevices.add(device);
            getHost().println("Track wrapper '" + device.name().get() + "' added");
         }

         // Remove track wrappers that no longer have associated tracks
//         mDeviceWrappers.forEach((existingDevice, deviceWrapper) -> {
//            if(!currentDevices.contains(existingDevice)){
//               // Track has left
//               getHost().println("Device wrapper '" + existingDevice.name().get() + "' removed");
//               //mTrackWrappers.remove(existingDevice);
//               //deviceWrapper = null;
//            }
//         });
      }
   }

   private void sceneListChanged(SceneBank bank) {
      for(int idx = 0; idx < bank.itemCount().getAsInt(); ++idx){
         Scene scene = bank.getItemAt(idx);
         getHost().println("Scene name is: " + scene.name().get());
      }
   }

   private void trackListChanged(TrackBank bank) {
      ArrayList<Track> currentTracks = new ArrayList<Track>();

      for(int idx = 0; idx < bank.itemCount().getAsInt(); ++idx){
         Track track = bank.getItemAt(idx);

         if(!mTrackWrappers.containsKey(track)){
            // Create wrapper
            TrackWrapper trackWrapper = new TrackWrapper(this, track);
            mTrackWrappers.put(track, trackWrapper);

            // Add wrapper to showtime performance
            if(this.client != null){
               this.client.get_root().add_child(trackWrapper);
            }

            currentTracks.add(track);
            getHost().println("Track wrapper '" + track.name().get() + "' added");
         }

         // Remove track wrappers that no longer have associated tracks
//         mTrackWrappers.forEach((existingTrack, trackWrapper) -> {
//            if(!currentTracks.contains(existingTrack)){
//               // Track has left
//               getHost().println("Track wrapper '" + existingTrack.name().get() + "' removed");
//               //mTrackWrappers.remove(trackWrapper);
//               //trackWrapper = null;
//            }
//         });

         getHost().println("Track name is: " + track.name().get());
      }
   }


   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg) 
   {
      // TODO: Implement your MIDI input handling code here.
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data) 
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
            mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
            mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
            mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
            mTransport.play();
      else if (data.equals("f07f7f0606f7"))
            mTransport.record();
   }
}
