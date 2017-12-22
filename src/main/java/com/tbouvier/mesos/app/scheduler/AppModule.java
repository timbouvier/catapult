package com.tbouvier.mesos.app.scheduler;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tbouvier.mesos.app.scheduler.AppMon;
import com.tbouvier.mesos.app.scheduler.AppMonThread;
import com.tbouvier.mesos.app.scheduler.ApplicationMonitor;
import com.tbouvier.mesos.app.scheduler.ApplicationMonitorThread;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */
public class AppModule extends AbstractModule {

    private int numThreads;

    public AppModule(int numThreads)
    {
        this.numThreads = numThreads;
    }

    @Override
    protected void configure()
    {
        bind(AppMon.class).to(ApplicationMonitor.class);
        //bind(AppMonThread.class).to(ApplicationMonitorThread.class);
    }

    @Provides
    List<AppMonThread> provideAppMonThreads()
    {
        List<AppMonThread> threads = new LinkedList<>();

        for(int i = 0 ; i < this.numThreads ; i++ )
        {
            threads.add( new ApplicationMonitorThread() );
        }

        return threads;
    }
}
