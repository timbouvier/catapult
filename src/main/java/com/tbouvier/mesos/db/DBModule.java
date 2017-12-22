package com.tbouvier.mesos.db;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tbouvier.mesos.scheduler.Protos.SchedulerConfig;
import com.tbouvier.mesos.util.Syslog;

/**
 * Created by bouviti on 4/24/17.
 */
public class DBModule extends AbstractModule {

    private SchedulerConfig schedulerConfig;

    public DBModule(SchedulerConfig schedulerConfig)
    {
        this.schedulerConfig = schedulerConfig;
    }

    @Override
    protected void configure()
    {
        //...
    }

    @Provides
    DatabaseClient provideDatabaseClient(){
        return new ZkClient(this.schedulerConfig.getZooKeeperInfo().getAddress(),
                this.schedulerConfig.getZooKeeperInfo().getRootNode(), this.schedulerConfig);
    }
}
