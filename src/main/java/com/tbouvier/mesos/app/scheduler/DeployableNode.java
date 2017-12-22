package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos;
 import org.apache.mesos.Protos.ExecutorID;
 import org.apache.mesos.Protos.SlaveID;

/**
 * Created by bouviti on 5/2/17.
 */
public interface DeployableNode {

    void message(AppDriver appDriver, byte[] message);
    void failed(AppDriver appDriver);
    void running(AppDriver appDriver);
    void relaunch(AppDriver appDriver);
    void finished(AppDriver appDriver);
    void killed(AppDriver appDriver);


    Protos.NodeID getNodeId();
    Protos.SchedulerNodeInfo getSchedulerNodeInfo();
    void changeState(Protos.SchedulerNodeInfo.STATE state);
    Protos.SchedulerTask getTask();
    void setNodeId(Protos.NodeID nodeId);
    void setAppId(Protos.AppID appId);
    void setParent(SchedulerApp app);
    void updateTaskInfo(Protos.SchedulerTask task);
    ExecutorID getExecutorId();
    SlaveID getSlaveId();
    boolean hasSlaveId();
    boolean hasExecutorId();
    SchedulerNode.State getState();
    void setState(SchedulerNode.State state);
    void updateAddress(Protos.SchedulerTask task);
    void updateUserData(byte[] data);

}
