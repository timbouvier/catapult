package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import org.apache.mesos.Protos.Offer;

/**
 * Created by bouviti on 4/24/17.
 */
public interface SchedQueue {

    public SchedulerTask getNextRunnableTask(Offer offer);
    public void setDatabase(DatabaseClient dbClient);
    public boolean hasRunnableTasks(Offer offer);
    public boolean insertTask(SchedulerTask task);
    public boolean hasTasks();
}
