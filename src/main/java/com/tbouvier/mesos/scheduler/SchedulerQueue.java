package com.tbouvier.mesos.scheduler;

import java.util.LinkedList;
import java.util.List;

import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import com.tbouvier.mesos.util.SchedulerConstraintUtils;
import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Protos.Offer;

/**
 * Created by bouviti on 4/24/17.
 */
class SchedulerQueue implements SchedQueue {

    enum STATE {
        UNKNOWN,
        STOP,
        RUN
    }
    
    private final SysLogger LOGGER = Syslog.getSysLogger(SchedulerQueue.class);

    private static final long LOCK_TMOUT = 5000;

    private DatabaseClient dbClient;
    private final List<SchedulerTask> queue;
    private final STATE state;
    private long tasksIn;
    private long tasksOut;

    public SchedulerQueue()
    {
        this.queue = new LinkedList<>();
        this.state = STATE.RUN;
        this.tasksIn = 0;
        this.tasksOut = 0;
    }

    public void setDatabase(DatabaseClient dbClient)
    {
        this.dbClient = dbClient;
    }



    private boolean isTaskRunnable(Offer offer, SchedulerTask task)
    {
       return SchedulerConstraintUtils.isRunnable(offer, task);
    }

    public SchedulerTask getNextRunnableTask(Offer offer)
    {
        synchronized (this.queue)
        {
            for(SchedulerTask task : this.queue)
            {
                if( this.isTaskRunnable(offer, task))
                {
                    this.queue.remove( task );
                    this.tasksOut++;
                    return task;
                }
            }
        }

        //exception?
        return null;
    }

    public boolean hasRunnableTasks(Offer offer)
    {
        synchronized (this.queue){
            for(SchedulerTask task : this.queue)
            {
                if( this.isTaskRunnable(offer, task))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean insertTask(SchedulerTask task)
    {
        LOGGER.info("Inserting new task: "+task.getTaskInfo().getTaskId());

        //write task to persistent storage before continuing
        if( !this.dbClient.insertTask( task) )
        {
            LOGGER.error("Failed to write task to zookeeper");
            return false;
        }

        synchronized (this.queue){
            this.tasksIn++;
            this.queue.add( task );
        }

        LOGGER.info("successfully inserted new task: "+task.getTaskInfo().getTaskId());
        return true;
    }

    public Integer size()
    {
        synchronized (this.queue){
            return this.queue.size();
        }
    }

    public boolean hasTasks()
    {
        return (this.queue.size() != 0);
    }

    public STATE getState()
    {
        return this.state;
    }

    public long getTasksIn()
    {
        return this.tasksIn;
    }

    public long getTasksOut()
    {
        return tasksOut;
    }
}
