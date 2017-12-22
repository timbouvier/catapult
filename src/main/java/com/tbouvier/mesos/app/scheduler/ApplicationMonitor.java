package com.tbouvier.mesos.app.scheduler;

import com.google.inject.Inject;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.util.Locker;
import org.apache.mesos.Protos.TaskStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by bouviti on 4/24/17.
 */

//parent of all the app mon worker threads
class ApplicationMonitor implements Runnable, AppMon{

    private final List<AppMonThread> workers;
    private final List<Thread> threads;
    private final Queue<AppEvent> events;

    //list of created apps that need to be polled
    private final List<Pollable> apps;

    private boolean shutdown;

    @Inject
    public ApplicationMonitor(List<AppMonThread> workers, Locker lock)
    {
        this.workers = workers;
        this.threads = new LinkedList<>();
        this.apps = new LinkedList<>();
        this.events = new LinkedList<>();
    }

    public void addApp(Pollable app)
    {
        synchronized (this.apps) {
            this.apps.add(app);
            this.addEvent(ApplicationEvent.TYPE.APP_INIT, app.getAppId());
        }
    }

    //remove this?
    private void removeApp(Pollable app)
    {
        synchronized (this.apps) {
            this.apps.remove(app);
        }
    }

    public Pollable getApp(int index)
    {
        synchronized (this.apps){

            if( index >= this.apps.size())
            {
                return null;
            }

            if( this.apps.get( index ).removed() || this.apps.get( index ).aborted() )
            {
                this.apps.remove( index );
                return null;
            }

            return this.apps.get(index);
        }
    }

    public Pollable getApp(AppEvent event)
    {
        synchronized (this.apps)
        {
            for(Pollable app : this.apps)
            {
                if( app.getAppId().getValue().equals( event.getAppId().getValue() ) )
                {
                    return app;
                }
            }
        }

        return null;
    }

    public void addEvent(Protos.SchedulerTask task, TaskStatus taskStatus)
    {
        synchronized ( this.events ){
            this.events.add( new ApplicationEvent(task, taskStatus) );
        }

        synchronized (this){
            this.notify();
        }
    }

    public void addEvent(ApplicationEvent.TYPE type, Protos.AppID appID)
    {
        synchronized (this.events){
            this.events.add( new ApplicationEvent(type, appID));
        }

        synchronized (this){
            this.notify();
        }
    }

    public void addEvent(Protos.SchedulerTask task, byte[] data)
    {
        synchronized (this.events){
            this.events.add(new ApplicationEvent(task, data));
        }

        synchronized (this)
        {
            this.notify();
        }
    }

    public void addEvent(Protos.SchedulerTask task, ApplicationEvent.TYPE type)
    {
        synchronized (this.events)
        {
            this.events.add(new ApplicationEvent(task, type));
        }

        synchronized (this)
        {
            this.notify();
        }
    }

    public AppEvent getEvent()
    {
        synchronized (this.events){
            return this.events.poll();
        }
    }

    public List<Pollable> getApps()
    {
        return this.apps;
    }

    @Override
    public void run()
    {
        for(AppMonThread worker : this.workers)
        {
            worker.setAppMon( this );
            this.threads.add( new Thread( worker ));
        }

        //kick off all the worker threads
        for(Thread thread : this.threads)
        {
            thread.start();
        }

        //monitor the health of the threads / restart them..
        while( !shutdown )
        {
            for(Thread thread : this.threads)
            {
                if( !thread.isAlive() )
                {
                    //thread.start(); create new thread using same worker obj?
                }
            }

            //quickly proccess any app removals or abortions
            synchronized (this.apps) {
                for (Pollable app : this.apps) {
                    if (app.removed() || app.aborted()) {
                        this.removeApp(app);

                        if(app.removed())
                        {
                            //remove persistent storage
                        }
                    }
                }
            }

            try {
                synchronized (this) {
                   // System.out.println("Beginning thread wait...");
                    this.wait();
                }
            }catch(InterruptedException e){
                //?
            }

            //wakeup all the worker threads
            //FIXME: add better logic to wakeup as many threads as we need to process the event(s)
            for(AppMonThread worker : this.workers)
            {
                synchronized (worker) {
                    worker.notify();
                }
            }
        }

        //shutdown all the worker threads
        for(AppMonThread worker : this.workers)
        {
            worker.shutdown();
            worker.notify();
        }

    }
}
