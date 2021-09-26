package com.showtime;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Track;
import showtime.ZstComponent;

public class DeviceWrapper extends ZstComponent {
    public static String COMPONENT_TYPE = "DEVICE";

    private ShowtimeBitwigExtension mDriver;
    public ZstComponent parameters;

    public DeviceWrapper(ShowtimeBitwigExtension driver, Device proxyDevice) {
        super(COMPONENT_TYPE, proxyDevice.name().get());

        mDriver = driver;
        this.parameters = new ZstComponent("parameters");
    }

    @Override
    public void on_registered() {
        super.on_registered();
        this.add_child(this.parameters);
    }
}
