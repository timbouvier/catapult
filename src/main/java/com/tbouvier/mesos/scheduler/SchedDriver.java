package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.AppFramework;
import com.tbouvier.mesos.MesosAppListener;
import com.tbouvier.mesos.app.scheduler.AppMon;
import com.tbouvier.mesos.app.scheduler.DeployableNode;
import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos.FrameworkMessage;
import org.apache.mesos.*;
import org.apache.mesos.Protos;

/**
 * Created by bouviti on 4/24/17.
 */
public interface SchedDriver extends org.apache.mesos.Scheduler {

    boolean isLeader();
    boolean initialize();
    String getNewAppID();
    SchedQueue getQueue();
    DatabaseClient getDbClient();
    void setListener(MesosAppListener listener);
    void setAppFramework(AppFramework mesosApp);
    void abort(boolean failover);
    Protos.FrameworkID getFrameworkId();
    boolean hasFrameworkId();
    void  setAppMon(AppMon appMon);
    void sendFrameworkMessage(DeployableNode node, FrameworkMessage message);
    void kickReconciler();

}
