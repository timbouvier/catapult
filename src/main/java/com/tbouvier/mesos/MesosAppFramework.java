package com.tbouvier.mesos;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.tbouvier.mesos.app.scheduler.AppModule;
import com.tbouvier.mesos.app.scheduler.DeployableApp;
import com.tbouvier.mesos.db.DBModule;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.SchedulerModule;
import com.tbouvier.mesos.util.ErrorTrace;
import com.tbouvier.mesos.util.Syslog;
import com.tbouvier.mesos.util.UtilityModule;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bouviti on 4/25/17.
 */
public class MesosAppFramework implements Runnable{

    private Protos.SchedulerConfig config;
    private Injector injector;
    private ApplicationFramework application;
    private MesosAppListener listener;

    public MesosAppFramework(Protos.SchedulerConfig config, MesosAppListener listener)
    {
        this.config = config;
        this.listener = listener;

        //initialize syslog and errortrace
        ErrorTrace.createNewErrorTrace();

        List<Module> modules = new LinkedList<>();

        modules.add( new UtilityModule( config) );
        modules.add( new DBModule( config) );
        modules.add( new SchedulerModule());
        modules.add( new AppModule( this.config.getPollThreads() ));


        injector = Guice.createInjector(modules);

        //instantiation of ApplicationFramework will start the main thread
        application = injector.getInstance(ApplicationFramework.class);

        //pass injector to app for any additional dependencies
        application.setInjector( injector );
        application.setListener( listener );


        //start the app thread
        //FIXME: come back to this
       // new Thread(application).start();

        //org.apache.mesos.
    }

    @Override
    public void run()
    {
        this.application.run();
    }

}
