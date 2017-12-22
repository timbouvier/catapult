package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.Protos.AppID;
import org.apache.mesos.Protos.TaskID;

import javax.xml.crypto.Data;
import javax.xml.soap.Node;
import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */
public interface DeployableApp {

    void initialized(AppDriver appDriver, AppID appID);
    void initFailed();
    void restart(AppDriver appDriver);
    void nodeFailed(AppDriver appDriver, Protos.NodeID nodeId);
    void message(AppDriver appDriver, Protos.NodeID nodeID, byte[] message);
    void nodeFinished(AppDriver appDriver, Protos.NodeID nodeId);
    void nodeKilled(AppDriver appDriver, Protos.NodeID nodeId);
    void nodeRunning(AppDriver appDriver, Protos.SchedulerTask task);
    TaskID getNewTaskId();
    void reregisterNode(AppDriver appDriver, Protos.SchedulerNodeInfo schedulerNodeInfo);

    //these methods should only be called by ApplicationDriver
    void setDbClient(DatabaseClient dbClient);
    void setAppId(AppID appId);
    AppID getAppId();
    void setSchedulerAppInfo(Protos.SchedulerAppInfo schedulerAppInfo);
    Protos.SchedulerAppInfo getSchedulerAppInfo();
    void removeNode(DeployableNode node);
    void addNode(DeployableNode node);
    //void updateNodeInfo(DeployableNode node);
    void setSchedulerConfig(Protos.SchedulerConfig config);
    void updateTaskInfo(Protos.SchedulerTask task);
    List<Protos.SchedulerTask> getTasks();
    void reregisterNodeInternal(DeployableNode node);
    byte[] getData();
    SchedulerNode.State getState(Protos.NodeID nodeID);

}
