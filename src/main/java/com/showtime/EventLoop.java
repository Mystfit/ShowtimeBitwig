package com.showtime;

import com.bitwig.extension.api.graphics.Bitmap;
import showtime.ShowtimeClient;

public class EventLoop {
    private final ShowtimeBitwigExtension mDriver;
    private final ShowtimeClient mClient;

    public EventLoop(ShowtimeClient client, ShowtimeBitwigExtension driver){
        mClient = client;
        mDriver = driver;
    }
    public void run(){
        mDriver.getHost().println("Event loop start");
        if(mClient != null){
            mDriver.getHost().println("Pre-poll");
            mClient.poll_once();
            mDriver.getHost().scheduleTask(this::run, 5);
        }
    }
}