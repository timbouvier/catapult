package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos;
import org.apache.mesos.Protos.TaskStatus;

import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */
public interface AppMon extends Runnable {

    List<Pollable> getApps();
    void addApp(Pollable app);
    Pollable getApp(int index);
    void addEvent(Protos.SchedulerTask task, TaskStatus taskStatus);
    void addEvent(Protos.SchedulerTask task, byte[] data);
    void addEvent(Protos.SchedulerTask task, ApplicationEvent.TYPE type);
    public AppEvent getEvent();
    public Pollable getApp(AppEvent event);

}
