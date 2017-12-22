package com.tbouvier.mesos.app.scheduler;

import org.apache.mesos.Protos;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import com.tbouvier.mesos.scheduler.Protos.AppID;

/**
 * Created by bouviti on 5/2/17.
 */
interface AppEvent {

    Protos.TaskStatus getTaskStatus();
    SchedulerTask getTask();
    boolean isFrameworkMessage();
    byte[] getFrameworkMessageData();
    ApplicationEvent.TYPE getType();
    AppID getAppId();
}
