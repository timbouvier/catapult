package com.tbouvier.mesos.app.scheduler;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.Protos.AppID;
import com.tbouvier.mesos.scheduler.SchedDriver;
import com.tbouvier.mesos.scheduler.SchedQueue;

import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Protos.TaskInfo;

import java.rmi.UnexpectedException;
import java.util.List;

/**
 * Created by bouviti on 4/24/17.
 */
public class ApplicationDriver implements AppDriver, Pollable {

    private final SysLogger LOGGER = Syslog.getSysLogger(ApplicationDriver.class);
    
    //implement all appdriver methods

    //callback interface
    private DeployableApp app;

    //driver interfaces
    private ApplicationDriverAPI appFramework;
    private SchedDriver scheduler;
    private SchedQueue taskQueue;
    private DatabaseClient dbClient;

    //client state commands
    private boolean abort;//to stop the driver
    private boolean removed;//tells poll threads that this app has been removed (in case they still have ref)
    private boolean initialized;

    //no guice injection here so we can accept many different types
    //of applications
    public ApplicationDriver(ApplicationDriverAPI applicationFramework, DeployableApp app)
    throws ExceptionInInitializerError
    {
        this.app = app;
        this.appFramework = applicationFramework;
        this.scheduler = this.appFramework.getSchedulerDriver();
        this.taskQueue = this.scheduler.getQueue();
        this.dbClient = this.scheduler.getDbClient();
        this.app.setAppId( AppID.newBuilder()
                .setValue( this.dbClient.getNewAppID() )
                .build());
        if( !this.dbClient.addApp(this.app.getAppId().getValue()) )
        {
            throw new ExceptionInInitializerError("Failed to create app znode");
        }
        this.app.setSchedulerConfig(this.appFramework.getSchedulerConfig());
        this.app.setDbClient( this.dbClient );
    }

    //constructor that is called after a failover...
    public ApplicationDriver(ApplicationDriverAPI applicationFramework, DeployableApp app, AppID appID)
    throws IllegalArgumentException
    {
        this.app = app;
        this.appFramework = applicationFramework;
        this.scheduler = this.appFramework.getSchedulerDriver();
        this.taskQueue = this.scheduler.getQueue();
        this.dbClient = this.scheduler.getDbClient();

        Protos.SchedulerAppInfo schedulerAppInfo;
        if( (schedulerAppInfo = this.dbClient.getApp( appID )) == null )
        {
            throw new IllegalArgumentException("Failed to find app: "+appID.getValue());
        }

        this.app.setSchedulerAppInfo( schedulerAppInfo );

        if( schedulerAppInfo.hasAppId() ) {
            LOGGER.info( "HAS APP ID");
            LOGGER.info("appid is: "+schedulerAppInfo.getAppId().getValue());
        }
        LOGGER.info( "NUMBER OF NODES ON REBUILD IS: "+schedulerAppInfo.getNodesList().size());

        this.app.setSchedulerConfig(this.appFramework.getSchedulerConfig());
        this.app.setDbClient( this.dbClient );
    }

    //NOTE: reregistration of node NEEDS to happen before this is called! otherwise it will overwrite
    //the app nodes with an empty protobuf list
    private boolean flush()
    {
        List<Protos.SchedulerTask> tasks = this.app.getTasks();

        //write all tasks to db
        for(Protos.SchedulerTask task : tasks)
        {
            if( !this.dbClient.updateTask(task) )
            {
                LOGGER.error("Failed to flush task to persistent storage");
                return false;
            }
        }

        if( !this.dbClient.updateApp(this.getAppId().getValue(), this.app.getData()) )
        {
            LOGGER.error("Failed to flush app to persistent storage");
            return false;
        }

        return true;
    }

    private void updateTaskInfo(Protos.SchedulerTask task, TaskStatus status)
    {
        Protos.SchedulerTask udpate = task.toBuilder()
                .setStatus( status )
                .build();

        this.app.updateTaskInfo( udpate );
    }

    private void handleStatusUpdate(AppEvent event)
    {
        //if we're already in this state then don't send user update
        //if the node was reregistered then send the update regardless
        //FIXME: check if the update is equal to all the fallthrough cases?
        if( ( !event.getTask().hasStatus() )
            || ( !event.getTask().getStatus().getState().equals( event.getTaskStatus().getState() ))
            || ( this.app.getState(event.getTask().getNodeId()).equals(SchedulerNode.State.REREGISTERED) ))
            {
                //do info update first in case user issues a AppDriver call that depends on it
                this.updateTaskInfo(event.getTask(), event.getTaskStatus());

                SchedulerNode.State state = this.app.getState(event.getTask().getNodeId());

                switch (event.getTaskStatus().getState())
                {
                    //case TASK_DROPPED:
                    case TASK_ERROR:
                    case TASK_FAILED:
                        //case TASK_GONE:
                    case TASK_LOST:
                        //case TASK_UNKNOWN:
                        // case TASK_GONE_BY_OPERATOR:
                        //case TASK_UNREACHABLE:
                        if( !state.equals(SchedulerNode.State.FAILED)
                            && !state.equals(SchedulerNode.State.FINISHED)
                            && !state.equals(SchedulerNode.State.KILLED)) {
                            this.app.nodeFailed(this, event.getTask().getNodeId());
                        }
                        break;
                    case TASK_FINISHED:
                        if( !state.equals(SchedulerNode.State.FAILED)
                                && !state.equals(SchedulerNode.State.FINISHED)
                                && !state.equals(SchedulerNode.State.KILLED)) {
                            this.app.nodeFinished(this, event.getTask().getNodeId());
                        }
                        break;
                    case TASK_RUNNING:
                        this.app.nodeRunning(this, event.getTask().toBuilder().setStatus(event.getTaskStatus()).build());
                        break;
                    case TASK_KILLED:
                        if( !state.equals(SchedulerNode.State.FAILED)
                                && !state.equals(SchedulerNode.State.FINISHED)
                                && !state.equals(SchedulerNode.State.KILLED)) {
                            this.app.nodeKilled(this, event.getTask().getNodeId());
                        }
                        break;
                    default:
                }
            }

        this.updateTaskInfo(event.getTask(), event.getTaskStatus());
    }

    //do we have to handle this? task to executor is a one to one mapping so we should always get the
    //task failed as well
    private void handleLostExecutor(Protos.SchedulerTask task)
    {
        SchedulerNode.State state = this.app.getState(task.getNodeId());

        //dont deliver the failure if it was already sent in some form
        if( state.equals(SchedulerNode.State.FAILED)
            || state.equals(SchedulerNode.State.FINISHED)
            || state.equals(SchedulerNode.State.KILLED))
        {
            return;
        }

        //this.app.nodeFailed(this, task.getNodeId());
    }

    //reregister all the user nodes to get reference to the schedulerApp override object (if any)
    //NOTE: not using reflection here to give user ability to change node object after failover / restart
    private void reregisterNodes()
    {
        for(Protos.SchedulerNodeInfo nodeInfo : this.app.getSchedulerAppInfo().getNodesList())
        {
            this.app.reregisterNode(this, nodeInfo);
        }

        //tell scheduler to do a reconcile
        //FIXME: wait until all apps reregistered then do this?
        LOGGER.debug("kicking task reconciler...");
        this.scheduler.kickReconciler();
    }

    private void setInitialized()
    {
        this.initialized = true;
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    //////////////////////// AppDriver Callins ////////////////////////////////////
    ////////////////////////                   ////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public void launchNode(DeployableNode node)
    {
        this.app.addNode( node );
        this.taskQueue.insertTask( node.getTask() );

        //flush changes to persistent storage
        if( !this.flush() )
        {
            LOGGER.error("failed to flush to persistent storage");
        }
    }

    public void relaunchNode(DeployableNode node)
    {
        LOGGER.info("Relaunching node: "+node.getNodeId());
        node.setState( SchedulerNode.State.RELAUNCH );
        this.taskQueue.insertTask( node.getTask() );

        if( !this.flush() )
        {
            LOGGER.error("Failed to flush to persistent storage");
        }
    }

    public void removeNode(DeployableNode node)
    {
        node.changeState(Protos.SchedulerNodeInfo.STATE.WAITING_REMOVE);

        //FIXME: schedule event to cleanly shutdown node


        //flush changes to persistent storage
        //FIXME: make sure tasks are removed
        this.flush();
    }

    public void sendMessage(DeployableNode node, byte[] data)
    {
        //send message via scheduler
        try{
            Protos.FrameworkMessage message = Protos.FrameworkMessage.newBuilder()
                    .setTaskId(node.getTask().getTaskInfo().getTaskId())
                    .setData(ByteString.copyFrom( data ) )
                    .build();

            this.scheduler.sendFrameworkMessage( node, message );
        }catch (Exception e)
        {
            LOGGER.error( "Caught exception in sendMessage: "+e);
            e.printStackTrace();
        }
    }

    public void reregisterNode(DeployableNode node)
    {
        node.setState(SchedulerNode.State.REREGISTERED);
        this.app.reregisterNodeInternal( node );

    }

    public void storeData(DeployableNode node, byte[] data)
    {
        node.updateUserData(data);
        this.flush();
    }

    public void abort()
    {
        this.abort = true;
    }

    public void remove()
    {
        this.removed = true;
    }


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    //////////////////////// Pollable Callins  ////////////////////////////////////
    ////////////////////////                   ////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public void updateTaskInfo(Protos.SchedulerTask task)
    {
        this.app.updateTaskInfo( task );
    }

    public AppID getAppId()
    {

        return this.app.getAppId();
    }

    public synchronized void poll(AppEvent event)
    {
        switch( event.getType() )
        {
            case APP_INIT:
                this.reregisterNodes();
                this.app.initialized(this, this.getAppId());
                this.setInitialized();
                break;
            case FRAMEWORK_MESSAGE:
                this.app.message(this, event.getTask().getNodeId(), event.getFrameworkMessageData());
                break;
            case STATUS_UPDATE:
                this.handleStatusUpdate(event);
                break;
            case LOST_EXECUTOR:
                this.handleLostExecutor(event.getTask());
        }

        //probably some state changed so flush to persistent storage
        if( !this.flush() )
        {
            LOGGER.info("failed to flush to persistent storage");
        }
    }

    //to stop execution and permanently remove the
    //app from persistent storage
    public boolean removed()
    {
        return this.removed;
    }

    //to stop execution
    public boolean aborted()
    {
        return this.abort;
    }
}
