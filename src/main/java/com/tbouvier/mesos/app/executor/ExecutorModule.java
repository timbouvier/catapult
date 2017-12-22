package com.tbouvier.mesos.app.executor;

import com.google.inject.AbstractModule;

/**
 * Created by bouviti on 5/5/17.
 */
public class ExecutorModule extends AbstractModule {

    @Override
    protected void configure()
    {
        bind(AppFrameworkExecutor.class).to(ApplicationFrameworkExecutor.class);
    }
}
