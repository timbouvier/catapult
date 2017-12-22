package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.util.Locker;

/**
 * Created by bouviti on 4/26/17.
 */
interface Pollable {

    void poll(AppEvent event);
    boolean removed();
    boolean aborted();
    Protos.AppID getAppId();
    boolean isInitialized();

    //for scheduler to update information
    void updateTaskInfo(Protos.SchedulerTask task);
}
