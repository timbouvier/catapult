package com.tbouvier.mesos;

import com.tbouvier.mesos.scheduler.Protos;

/**
 * Created by bouviti on 4/26/17.
 */
public interface MesosAppListener {

    void disconnected(AppFramework app);
    void connected(AppFramework app);
    void applicationFailed(Protos.AppID appID);
}
