package com.showtime;

import com.bitwig.extension.controller.api.Track;
import showtime.ZstComponent;

public class TrackWrapper  extends showtime.ZstComponent {

    public static String COMPONENT_TYPE = "TRACK";

    private ShowtimeBitwigExtension mDriver;
    public ZstComponent devices;
    public ZstComponent clipslots;

    public TrackWrapper(ShowtimeBitwigExtension driver, Track proxyTrack) {
        super(COMPONENT_TYPE, proxyTrack.name().get());

        mDriver = driver;
        this.devices = new ZstComponent("devices");
        this.clipslots = new ZstComponent("clipslots");
    }

    @Override
    public void on_registered() {
        super.on_registered();
        this.add_child(this.devices);
        this.add_child(this.clipslots);
    }
}
