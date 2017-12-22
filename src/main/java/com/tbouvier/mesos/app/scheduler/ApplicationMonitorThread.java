package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */

//worker thread to process app events
class ApplicationMonitorThread implements Runnable, AppMonThread {

    private final SysLogger LOGGER = Syslog.getSysLogger(ApplicationMonitorThread.class);

    private AppMon appMon;
    private boolean shutdown;
    private boolean running;

    public void setAppMon(AppMon appMon) {
        this.appMon = appMon;
    }

    public void shutdown()
    {
        this.shutdown = true;
    }



    @Override
    public void run()
    {

        while (!this.shutdown)
        {
            Pollable app;
            AppEvent event = this.appMon.getEvent();

            try {

                if (event != null) {
                    if ((app = this.appMon.getApp(event)) != null) {
                        //check if the app has been initialized if its not an APP_INIT event
                        if ((event.getType().equals(ApplicationEvent.TYPE.APP_INIT))
                                || (app.isInitialized())) {
                            app.poll(event);
                        }
                    }
                } else {
                    try {
                        synchronized (this) {
                            //System.out.println("Beginning thread wait...");
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        //?
                    }
                }
            }catch (Exception e)
            {
                LOGGER.error("Caught exception in appmonthread: "+e);
                e.printStackTrace();
            }
        }
    }
}
