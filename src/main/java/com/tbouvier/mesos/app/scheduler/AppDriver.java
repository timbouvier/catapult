package com.tbouvier.mesos.app.scheduler;


/**
 * Created by bouviti on 4/24/17.
 */
public interface AppDriver {

    void launchNode(DeployableNode node);
    void sendMessage(DeployableNode node, byte[] data);
    void reregisterNode(DeployableNode node);
    void relaunchNode(DeployableNode node);
    void storeData(DeployableNode node, byte[] data);
    void abort();
}
