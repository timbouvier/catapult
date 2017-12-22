package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;

import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */
interface AppNode
{

    void launchTask(SchedulerTask task);
    void launchTasks(List<SchedulerTask> task);

}
