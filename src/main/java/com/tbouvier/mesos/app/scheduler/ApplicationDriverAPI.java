package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.SchedDriver;

/**
 * Created by bouviti on 4/25/17.
 */
public interface ApplicationDriverAPI {

     SchedDriver getSchedulerDriver();
     Protos.SchedulerConfig getSchedulerConfig();

}
