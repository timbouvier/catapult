package com.tbouvier.mesos.app.scheduler;

import org.apache.mesos.Protos;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import com.tbouvier.mesos.scheduler.Protos.AppID;

/**
 * Created by bouviti on 5/2/17.
 */
public class ApplicationEvent implements AppEvent {


    public enum TYPE {
        UNKNOWN,
        APP_INIT,
        FRAMEWORK_MESSAGE,
        STATUS_UPDATE,
        LOST_EXECUTOR
    }

    private SchedulerTask task;

    private Protos.TaskStatus taskStatus;

    private boolean isFrameworkMessage;
    private byte[] frameworkMessageData;
    private AppID appID;
    private TYPE type;

    ApplicationEvent(SchedulerTask task, Protos.TaskStatus taskStatus)
    {
        this.task = task;
        this.taskStatus = taskStatus;
        this.appID = this.task.getAppId();
        this.type = TYPE.STATUS_UPDATE;
    }

    ApplicationEvent(SchedulerTask task, byte[] frameworkMessageData)
    {
        this.task = task;
        this.appID = this.task.getAppId();
        this.frameworkMessageData = frameworkMessageData;
        this.type = TYPE.FRAMEWORK_MESSAGE;
    }

    ApplicationEvent(SchedulerTask task, TYPE type)
    {
        this.task = task;
        this.appID = task.getAppId();
        this.type = type;
    }

    ApplicationEvent(TYPE type, AppID appID)
    {
        this.type = type;
        this.appID = appID;
    }

    public TYPE getType()
    {
        return this.type;
    }

    public AppID getAppId()
    {
        return this.appID;
    }

    public SchedulerTask getTask()
    {
        return this.task;
    }

    public Protos.TaskStatus getTaskStatus()
    {
        return this.taskStatus;
    }

    public boolean isFrameworkMessage()
    {
        return this.isFrameworkMessage;
    }

    public byte[] getFrameworkMessageData()
    {
        return this.frameworkMessageData;
    }
}
