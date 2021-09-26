package com.showtime;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Parameter;
import showtime.ZstComponent;
import showtime.ZstInputPlug;
import showtime.ZstOutputPlug;
import showtime.ZstValueType;

public class ParameterWrapper extends ZstComponent {
    public static String COMPONENT_TYPE = "PARAMETER";
    public ZstInputPlug input;
    public ZstOutputPlug output;

    private ShowtimeBitwigExtension mDriver;


    public ParameterWrapper(ShowtimeBitwigExtension driver, Parameter proxyParameter) {
        super(COMPONENT_TYPE, proxyParameter.name().get());

        mDriver = driver;
        this.input = new ZstInputPlug("in", ZstValueType.FloatList);
        this.output = new ZstOutputPlug("out", ZstValueType.FloatList);
    }

    @Override
    public void on_registered() {
        super.on_registered();
        this.add_child(this.input);
        this.add_child(this.output);
    }
}
