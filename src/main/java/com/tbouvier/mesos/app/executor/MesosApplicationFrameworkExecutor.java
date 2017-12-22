package com.tbouvier.mesos.app.executor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tbouvier.mesos.util.ErrorTrace;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;

/**
 * Created by bouviti on 5/5/17.
 */
public class MesosApplicationFrameworkExecutor implements Runnable {

    private ExecutableApp app;

    public MesosApplicationFrameworkExecutor(ExecutableApp app)
    {
        this.app = app;
    }

    @Override
    public void run()
    {
        ErrorTrace.createNewErrorTrace();

        //wire up dependencies
        Injector injector = Guice.createInjector( new ExecutorModule() );

        AppFrameworkExecutor executor = injector.getInstance(AppFrameworkExecutor.class);

        //set the app that will be run
        executor.setExecutableApp( this.app );

        MesosExecutorDriver driver = new MesosExecutorDriver(executor);

        //blocking call to driver run
        Protos.Status status = driver.run();

        System.exit( status.getNumber() );

    }
}
