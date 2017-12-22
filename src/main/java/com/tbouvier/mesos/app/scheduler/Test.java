package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos;

/**
 * Created by bouviti on 5/2/17.
 */
public class Test extends SchedulerApp {

    @Override
    public void setAppId(Protos.AppID appId)
    {

    }




    public static void main(String[] args)
    {
        DeployableApp app = new Test();
    }
}
