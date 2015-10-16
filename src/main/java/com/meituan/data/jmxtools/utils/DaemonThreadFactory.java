package com.meituan.data.jmxtools.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {
    public static DaemonThreadFactory instance = new DaemonThreadFactory();

    private DaemonThreadFactory() {}

    public Thread newThread(Runnable r) {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    }
}
