package com.tbouvier.mesos.util;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tbouvier.mesos.scheduler.Protos;

/**
 * Created by bouviti on 4/22/17.
 */
public class UtilityModule extends AbstractModule {

    private Protos.SchedulerConfig schedulerConfig;

    public UtilityModule(Protos.SchedulerConfig schedulerConfig)
    {
        this.schedulerConfig = schedulerConfig;
    }

    @Override
    protected void configure(){
        bind(Locker.class).to(UtilLock.class);
    }

    @Provides
    Protos.SchedulerConfig provideSchedulerConfig()
    {
        return this.schedulerConfig;
    }
}
