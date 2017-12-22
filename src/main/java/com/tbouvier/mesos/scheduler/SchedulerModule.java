package com.tbouvier.mesos.scheduler;

import com.google.inject.AbstractModule;

/**
 * Created by bouviti on 4/24/17.
 */
public class SchedulerModule extends AbstractModule {

    @Override
    protected void configure()
    {
        bind(SchedDriver.class).to(SchedulerDriver.class);
        bind(SchedQueue.class).to(SchedulerQueue.class);
    }
}
