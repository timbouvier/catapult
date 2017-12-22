package com.tbouvier.mesos;

import com.tbouvier.mesos.app.scheduler.DeployableApp;
import com.tbouvier.mesos.scheduler.Protos;

import java.util.List;

/**
 * Created by bouviti on 4/25/17.
 */
public interface AppFramework {

    List<Protos.AppID> getApps();
    void register(DeployableApp app);
    void reregister(Protos.AppID appID, DeployableApp app);
    void abort(boolean failover);
}
