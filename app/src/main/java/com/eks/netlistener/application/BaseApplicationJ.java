package com.eks.netlistener.application;

import android.app.Application;

/**
 * Created by Riggs on 2020/1/30
 */
public class BaseApplicationJ extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        SocketAddressForTesting[] socketAddressForTestings = {
//                new SocketAddressForTesting("www.google.com", 80)
//                , new SocketAddressForTesting("www.bilibili.com", 80)
//        };
//        NetworkInterceptManager.INSTANCE.setCustomServers(socketAddressForTestings);
    }
}
