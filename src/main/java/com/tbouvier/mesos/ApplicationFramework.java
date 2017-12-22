package com.tbouvier.mesos;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.tbouvier.mesos.app.scheduler.AppMon;
import com.tbouvier.mesos.app.scheduler.DeployableApp;
import com.tbouvier.mesos.app.scheduler.ApplicationDriver;
import com.tbouvier.mesos.app.scheduler.ApplicationDriverAPI;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.SchedDriver;
import com.tbouvier.mesos.scheduler.Protos.SchedulerConfig;
import com.tbouvier.mesos.util.*;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.Status;

import java.net.UnknownHostException;
import java.rmi.UnexpectedException;
import java.util.List;

/**
 * Created by bouviti on 4/22/17.
 */
class ApplicationFramework implements Runnable, ApplicationDriverAPI, AppFramework {
    
    private final SysLogger LOGGER = Syslog.getSysLogger(ApplicationFramework.class);

    public static final long ONE_MILISECOND = 1000;

    private SchedulerConfig schedulerConfig;
    private MesosAppListener listener;
    private Injector injector;
    private AppFramework appFramework;

    private AppMon appMon;
    private SchedDriver scheduler;
    private MesosSchedulerDriver mesosDriver;

    @Inject
    public ApplicationFramework(SchedulerConfig config, SchedDriver scheduler,
                                AppMon appMon)
    {
        this.appMon = appMon;
        this.schedulerConfig = config;
        this.scheduler = scheduler;
    }

    public void setInjector(Injector injector)
    {
        this.injector = injector;
    }

    public void setListener(MesosAppListener listener)
    {
        this.listener = listener;
    }


    private FrameworkInfo buildFrameworkInfo()
    {
        FrameworkInfo.Builder builder = FrameworkInfo.newBuilder()
                .setFailoverTimeout( this.schedulerConfig.getMesosInfo().getFailoverTimeout())
                .setHostname(this.schedulerConfig.getHost())
                .setUser("")
                .setName(this.schedulerConfig.getFrameworkName())
                .setCheckpoint(true);

        if( this.scheduler.hasFrameworkId())
        {
            builder.setId( this.scheduler.getFrameworkId());
        }

        return builder.build();
    }

    private void preprocessSchedulerConfig()
    {
        try {
            if (!this.schedulerConfig.hasHost()) {
                this.schedulerConfig = this.schedulerConfig.toBuilder()
                        .setHost(NetUtils.getLocalHostIp())
                        .build();
            }
        }
        catch(UnknownHostException e)
        {
            LOGGER.error("Unknown host: "+e);
            e.printStackTrace();
        }
    }


    /////////////////////////////////////////////////////////////////////
    /////////////////// ApplicationDriverAPI callins ////////////////////
    ///////////////////                              ////////////////////
    /////////////////////////////////////////////////////////////////////

    public SchedDriver getSchedulerDriver()
    {
        return this.scheduler;
    }

    public AppMon getAppMon(){
        return this.appMon;
    }

    public SchedulerConfig getSchedulerConfig()
    {
        return this.schedulerConfig;
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////// AppFramework callins ////////////////////////////
    ///////////////////                      ////////////////////////////
    /////////////////////////////////////////////////////////////////////

    public List<Protos.AppID> getApps()
    {
        return this.scheduler.getDbClient().getApps();
    }

    public void abort(boolean failover)
    {
        this.scheduler.abort(failover);

        //FIXME: wait for SchedulerDriver disconnected callback to fire
    }

    public void register(DeployableApp app)
    {
        try {
            this.appMon.addApp(new ApplicationDriver(this, app));
        }catch (ExceptionInInitializerError e)
        {
            LOGGER.error("Caught exception: "+e);
            e.printStackTrace();
        }
    }

    //Starts an application that has been stopped explicitly or due to framework failover
    public void reregister(Protos.AppID appID, DeployableApp app)
    {
        //FIXME: check if app is already running

        try{
            this.appMon.addApp( new ApplicationDriver(this, app, appID));
        }catch(Exception e){
            LOGGER.error( "Caught exception during app start: "+e);
            app.initFailed();
        }
    }


    //////////////////////////////////////////////////////////////////////
    /////////////////// main application deployer thread /////////////////
    ///////////////////                                  ////////////////
    /////////////////////////////////////////////////////////////////////

    @Override
    public void run()
    {

        this.preprocessSchedulerConfig();

        if( !this.scheduler.initialize() )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_CRITICAL, ErrorCodes.SCHED, ErrorCodes.DATABASE_FAILURE,
                    "Failed to initialize scheduler");
            return;
        }

        this.scheduler.setListener( this.listener );
        this.scheduler.setAppFramework( this );
        this.scheduler.setAppMon( this.appMon );

        //if we're not the leader then spin
        while( !this.scheduler.isLeader() )
        {
            try{
                Thread.sleep(ApplicationFramework.ONE_MILISECOND);
            }catch (InterruptedException e){
                //FIXME
            }
        }

        //start up appmon threads
        new Thread( this.appMon ).start();


        this.mesosDriver = new MesosSchedulerDriver(this.scheduler, this.buildFrameworkInfo(),
                this.schedulerConfig.getMesosInfo().getAddress());


        //blocking call to run driver
        Status status = this.mesosDriver.run();

        //if we are here then we got disconnected
        //check if we already called disconnected?
        this.listener.disconnected( this );
    }
}
