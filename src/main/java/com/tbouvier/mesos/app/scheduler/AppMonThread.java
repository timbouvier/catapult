package com.tbouvier.mesos.app.scheduler;

/**
 * Created by bouviti on 4/24/17.
 */
interface AppMonThread extends Runnable{

    void setAppMon(AppMon appMon);
    void shutdown();

}
