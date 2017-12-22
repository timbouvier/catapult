package com.tbouvier.mesos.db;

import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.Protos.AppID;
import com.tbouvier.mesos.scheduler.Protos.SchedulerAppInfo;

import java.util.List;

/**
 * Created by bouviti on 4/22/17.
 */
public interface DatabaseClient {

    //create interface for storing and retreiving framework objects + data
    //create interface for leader election..

    boolean initialize();
    boolean isLeader();

    String getNewNodeID(String appId);
    String getNewAppID();
    String getNewTaskID(String appId);
    boolean addApp(String appId);
    Protos.SchedulerAppInfo getApp(AppID appID);
    public List<AppID> getApps();
    Integer getAndIncrement(String configNode);
    boolean insertTask(Protos.SchedulerTask task);
    boolean setFrameworkId(String frameworkId);
    String getFrameworkId();
    Protos.SchedulerTask getTask(String taskId);
    boolean updateApp(String appId, byte[] data);
    public boolean updateTask(Protos.SchedulerTask task);
    public List<Protos.SchedulerTask> getTasks();

}
