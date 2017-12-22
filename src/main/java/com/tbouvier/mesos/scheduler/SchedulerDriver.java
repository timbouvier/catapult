package com.tbouvier.mesos.scheduler;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tbouvier.mesos.AppFramework;
import com.tbouvier.mesos.MesosAppListener;
import com.tbouvier.mesos.app.scheduler.AppMon;
import com.tbouvier.mesos.app.scheduler.ApplicationEvent;
import com.tbouvier.mesos.app.scheduler.DeployableNode;
import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.MasterInfo;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import com.tbouvier.mesos.scheduler.Protos.FrameworkMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bouviti on 4/22/17.
 */
class SchedulerDriver implements SchedDriver, org.apache.mesos.Scheduler, Runnable {


    enum OFFER_STATE {
        UKNOWN,
        ON,
        OFF
    }
    
    private final SysLogger LOGGER = Syslog.getSysLogger(SchedulerDriver.class);

    private static final long RECONCILE_INTERVAL = 30000;

    private DatabaseClient dbClient;
    private SchedQueue queue;
    private MesosAppListener listener;
    private AppMon appMon;
    private String frameworkId;
    private boolean die;

    //interface to pass back to user interface callbacks
    private AppFramework appFramework;

    private org.apache.mesos.SchedulerDriver driver;
    private OFFER_STATE offerState;
    private long lastReconcile;

    @Inject
    public SchedulerDriver(DatabaseClient dbClient, SchedQueue queue)
    {
        this.dbClient = dbClient;
        this.queue = queue;
        this.queue.setDatabase( dbClient );
        this.lastReconcile = 0;
        this.offerState = OFFER_STATE.ON;
    }

    public void kill()
    {
        this.die = true;
    }

    public boolean hasFrameworkId()
    {
        //if we don't have it see if we can get it
        if( this.frameworkId == null)
        {
            this.frameworkId = this.dbClient.getFrameworkId();
        }

        return (this.frameworkId != null);
    }

    public FrameworkID getFrameworkId()
    {
        //if we don't have it see if we can get it
        if( this.frameworkId == null)
        {
            this.frameworkId = this.dbClient.getFrameworkId();
        }

        return FrameworkID.newBuilder()
                .setValue( this.frameworkId )
                .build();

    }

    public boolean initialize()
    {
        //add other init routines here
        return this.dbClient.initialize();
    }

    public boolean isLeader()
    {
        return this.dbClient.isLeader();
    }

    public String getNewAppID()
    {
        return this.dbClient.getNewAppID();
    }

    public SchedQueue getQueue()
    {
        return this.queue;
    }

    public DatabaseClient getDbClient()
    {
        return this.dbClient;
    }

    public void setListener(MesosAppListener listener)
    {
        this.listener = listener;
    }

    public void setAppFramework(AppFramework app)
    {
        this.appFramework = app;
    }

    public void setAppMon(AppMon appMon)
    {
        this.appMon = appMon;
    }

    public void abort(boolean failover)
    {
        //FIXME: if not connected yet, set flag to immediately abort on registration?
        if( this.driver == null )
        {
            return;
        }

        this.driver.stop(failover);
    }

    public void sendFrameworkMessage(DeployableNode node, FrameworkMessage message)
    {
        try{
            if( node.hasExecutorId() && node.hasSlaveId())
            {
                this.driver.sendFrameworkMessage(node.getExecutorId(), node.getSlaveId(),
                        message.toByteArray());
            }
        }catch(Exception e){
            LOGGER.error( "Caught exception in sendFramework: "+e);
        }
    }

    public void kickReconciler()
    {
        synchronized (this)
        {
            this.lastReconcile = 0;
        }
    }


    @Override
    public void registered(org.apache.mesos.SchedulerDriver schedulerDriver,
                           FrameworkID frameworkID, MasterInfo masterInfo) {

        this.driver = schedulerDriver;

        //write frameworkId to disk for reregistration after failover
        if( !this.dbClient.setFrameworkId( frameworkID.getValue() ) )
        {
            LOGGER.error( "Failed to write frameworkId to persistent storage");
            schedulerDriver.stop();
            return;
        }

        //start thread to manage offer state + task reconcile
        new Thread(this).start();

        this.listener.connected(this.appFramework);
    }

    @Override
    public void reregistered(org.apache.mesos.SchedulerDriver schedulerDriver,
                             MasterInfo masterInfo) {
        //is this ever called?

    }

    private void launchTasks(org.apache.mesos.SchedulerDriver schedulerDriver,
                             List<SchedulerTask> tasks)
    {
        Collection<Protos.TaskInfo> taskInfos = new ArrayList<Protos.TaskInfo>();
        Collection<Protos.OfferID> offerIDs = new ArrayList<Protos.OfferID>();

        for(SchedulerTask task : tasks)
        {
            taskInfos.add( task.getTaskInfo());
            offerIDs.add(task.getOffer().getId());

            LOGGER.debug(
                    "Adding task: "+task.getTaskInfo().getTaskId().getValue()+" for offerid: "+task.getOffer().getId()+" slave: "+task.getTaskInfo().getSlaveId());
        }

        schedulerDriver.launchTasks(offerIDs, taskInfos);
    }

    private void launchTask(org.apache.mesos.SchedulerDriver schedulerDriver,
                            SchedulerTask task)
    {
        List<SchedulerTask> tasks = new LinkedList<>();
        tasks.add( task );

        this.launchTasks(schedulerDriver, tasks);
    }

    private void reconcileTasks()
    {
        List<SchedulerTask> tasks = this.dbClient.getTasks();
        List<Protos.TaskStatus> statuses = new LinkedList<>();
        Protos.TaskStatus status;

        if( tasks != null )
        {
               for( SchedulerTask task : tasks)
               {
                   if( task.hasStatus()
                       && task.getStatus().getState().equals(Protos.TaskState.TASK_RUNNING))
                   {
                       LOGGER.debug("Adding task to reconcile list: "+task.getTaskInfo().getTaskId().getValue());
                       statuses.add(task.getStatus());
                   }
               }

               this.driver.reconcileTasks(statuses);
        }
    }

    //FIXME: figure out why sending launchTasks with mutliple tasks reports that tasks are bound to the wrong agent id??
    @Override
    public void resourceOffers(org.apache.mesos.SchedulerDriver schedulerDriver,
                               List<Protos.Offer> offers) {
        try{

            List<SchedulerTask> tasks = new LinkedList<>();
            SchedulerTask task;

            for(Protos.Offer offer : offers)
            {
                if (this.queue.hasRunnableTasks(offer))
                {
                    task = this.queue.getNextRunnableTask(offer);

                    //keep track of offer that was used for this task
                    task = task.toBuilder().setOffer( offer ).build();

                    //assign slaveId to task FIXME: figure out a better way to do this. hold builder somewhere?
                    task = task.toBuilder()
                            .setTaskInfo( task.getTaskInfo().toBuilder()
                                    .setSlaveId( offer.getSlaveId() )
                                    .build())
                            .build();

                    if( !this.dbClient.updateTask(task) )
                    {
                        LOGGER.error("Failed to write task to db before launchTask.");
                        this.queue.insertTask(task);
                        continue;
                    }

                    LOGGER.info(
                            "Luanching task "+task.getTaskInfo().getTaskId().getValue()+" on host: "+offer.getHostname());
                   // tasks.add( task );
                    this.launchTask(schedulerDriver, task);
                }
                else
                {
                    schedulerDriver.declineOffer(offer.getId());
                }
            }

            if( !tasks.isEmpty() ) {
                //this.launchTasks(schedulerDriver, tasks);
            }
        }catch (Exception e){
            //stop driver?
        }
    }

    @Override
    public void offerRescinded(org.apache.mesos.SchedulerDriver schedulerDriver,
                               Protos.OfferID offerID) {

        LOGGER.info( "Offer rescinded: "+offerID.getValue());
    }

    @Override
    public void statusUpdate(org.apache.mesos.SchedulerDriver schedulerDriver,
                             Protos.TaskStatus taskStatus) {
        try {

            SchedulerTask task = this.dbClient.getTask(taskStatus.getTaskId().getValue());

            if (task == null) {
                //error
                return;
            }

            if( !taskStatus.getState().equals(Protos.TaskState.TASK_RUNNING) ) {
                LOGGER.info(
                        "Status update from task: " + task.getTaskInfo().getTaskId().getValue() + " status: " + taskStatus.getState().toString());
            }
            this.appMon.addEvent(task, taskStatus);
        }catch(Exception e)
        {
            LOGGER.error( "Caught exception in driver thread: "+e);
            e.printStackTrace();
        }

    }

    @Override
    public void frameworkMessage(org.apache.mesos.SchedulerDriver schedulerDriver,
                                 Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {
        SchedulerTask task;
        FrameworkMessage message;

        try {

            try {
                message = FrameworkMessage.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error( "caught exception in protobuffer deserialization");
                return;
            }

            if ((task = this.dbClient.getTask(message.getTaskId().getValue())) == null) {
                LOGGER.error( "Failed to lookup task in framework message");
            }

            this.appMon.addEvent(task, message.getData().toByteArray());
        }catch(Exception e)
        {
            LOGGER.error("Caught exception in driver thread: "+e);
            e.printStackTrace();
        }

    }

    @Override
    public void disconnected(org.apache.mesos.SchedulerDriver schedulerDriver) {

        this.listener.disconnected(this.appFramework);
        this.die = true;
        this.driver.stop(true);
    }


    @Override
    public void slaveLost(org.apache.mesos.SchedulerDriver schedulerDriver,
                          Protos.SlaveID slaveID) {
        //anything to do here? / we will still get lost executor if it happens
    }

    @Override
    public void executorLost(org.apache.mesos.SchedulerDriver schedulerDriver,
                             Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {
        List<SchedulerTask> tasks;

        if( (tasks = this.dbClient.getTasks()) != null)
        {
            for( SchedulerTask task : tasks)
            {
                if( task.getTaskInfo().hasExecutor()
                    && task.getTaskInfo().getExecutor().getExecutorId().getValue().equals(executorID.getValue())
                    && task.getTaskInfo().getSlaveId().getValue().equals(slaveID.getValue()))
                {
                    LOGGER.info("Lost executor for task: "+task.getTaskInfo().getTaskId());
                    this.appMon.addEvent(task, ApplicationEvent.TYPE.LOST_EXECUTOR);
                }
            }
        }
    }


    @Override
    public void error(org.apache.mesos.SchedulerDriver schedulerDriver, String s) {
        LOGGER.error("Scheduler driver reports error: "+s);
    }


    private void handleOfferState()
    {
        if( this.offerState.equals(OFFER_STATE.OFF) && this.queue.hasTasks() )
        {
            LOGGER.info("Setting offer state to: ON");
            this.driver.reviveOffers();
            this.offerState = OFFER_STATE.ON;
        }
        else if( this.offerState.equals(OFFER_STATE.ON) && !this.queue.hasTasks())
        {
            LOGGER.info("Setting offer state to: OFF");
            this.driver.suppressOffers();
            this.offerState = OFFER_STATE.OFF;
        }

        //otherwise leave offer state alone
    }

    private void handleReconcileTasks(long interval)
    {
        //sync on the interval computation
        synchronized (this) {
            if ((System.currentTimeMillis() - this.lastReconcile) > interval) {
                this.lastReconcile = System.currentTimeMillis();
            } else {
                return;
            }
        }

        this.reconcileTasks();
    }

    @Override
    public void run()
    {
        while( !this.die )
        {
            this.handleOfferState();
            this.handleReconcileTasks( SchedulerDriver.RECONCILE_INTERVAL );

            try{
                Thread.sleep(50);
            }catch (Exception e){}
        }
    }

}
