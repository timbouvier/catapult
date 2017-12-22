package com.tbouvier.mesos.app.scheduler;

import com.tbouvier.mesos.db.DatabaseClient;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Protos.TaskID;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bouviti on 5/2/17.
 */
public class SchedulerApp implements DeployableApp {

    private final SysLogger LOGGER = Syslog.getSysLogger(SchedulerApp.class);
    
    private Protos.SchedulerAppInfo schedulerAppInfo;
    private Protos.SchedulerConfig schedulerConfig;
    private final List<DeployableNode> nodes;
    private String name;

    private DatabaseClient dbClient;

    public SchedulerApp()
    {
        this.nodes = new LinkedList<>();
    }

    public SchedulerApp(String name){
       this.name = name;
       this.nodes = new LinkedList<>();
    }

    public SchedulerApp(Protos.SchedulerAppInfo schedulerAppInfo)
    {
        this.nodes = new LinkedList<>();
        this.schedulerAppInfo = schedulerAppInfo;
        this.buildNodes();
    }

    private void buildNodes()
    {
        //populate final list with values from schedulerAppInfo
    }

    private Integer getNodeIndex(DeployableNode queryNode)
    {
        synchronized (this.nodes) {
            int count = 0;
            for (DeployableNode node : this.nodes) {
                if (queryNode.getNodeId().getValue().equals(node.getNodeId().getValue()) ) {
                    return count;
                }

                count++;
            }
        }

        return -1;
    }

    public DeployableNode getNode(Protos.NodeID nodeID)
    {
        synchronized (this.nodes) {
            for (DeployableNode node : this.nodes) {
                if (node.getNodeId().getValue().equals(nodeID.getValue()) ) {
                    return node;
                }
            }
        }

        return null;
    }

    private Protos.NodeID getNewNodeId()
    {
        return Protos.NodeID.newBuilder()
                .setValue( this.dbClient.getNewNodeID( this.getAppId().getValue() ) )
                .build();
    }


    public void initialized(AppDriver appDriver, Protos.AppID appID)
    {
        //no-op: user should always implement this routine in order to get a pointer
        //to appDriver and begin deploying nodes
    }

    public void initFailed()
    {
        //no-op: user should always implement this routine
    }

    public  void restart(AppDriver appDriver)
    {

    }

    public void nodeFailed(AppDriver appDriver, Protos.NodeID nodeId)
    {
        DeployableNode node;

        if( (node = this.getNode(nodeId)) != null )
        {
            LOGGER.info(
                    "Node failed. app: "+this.getAppId().getValue()+" node: "+node.getNodeId().getValue());
            node.setState( SchedulerNode.State.FAILED );
            node.failed(appDriver);
        }
    }

    public void message(AppDriver appDriver, Protos.NodeID nodeId, byte[] message)
    {
        DeployableNode node;

        if( (node = this.getNode(nodeId)) != null )
        {
            node.message(appDriver, message);
        }
    }

    public void nodeFinished(AppDriver appDriver, Protos.NodeID nodeId)
    {
        DeployableNode node;

        if( (node = this.getNode(nodeId)) != null )
        {
            LOGGER.info(
                    "Node finished. app: "+this.getAppId().getValue()+" node: "+node.getNodeId().getValue());
            node.setState( SchedulerNode.State.FINISHED );
            node.finished(appDriver);
        }
    }

    public void nodeKilled(AppDriver appDriver, Protos.NodeID nodeId)
    {
        DeployableNode node;

        if( (node = this.getNode(nodeId)) != null )
        {
            LOGGER.info(
                    "Node killed. app: "+this.getAppId().getValue()+" node: "+node.getNodeId().getValue());
            node.setState( SchedulerNode.State.KILLED );
            node.killed(appDriver);
        }
    }

    public void nodeRunning(AppDriver appDriver, Protos.SchedulerTask task)
    {
        DeployableNode node;

        if( (node = this.getNode(task.getNodeId())) != null )
        {
            LOGGER.info(
                    "Node running. app: "+this.getAppId().getValue()+" node: "+node.getNodeId().getValue());
            node.updateAddress( task );
            node.setState( SchedulerNode.State.RUNNING );
            node.running(appDriver);
        }
        else
        {
            System.out.println("Failed to find node in status callback...");
        }
    }

    public void reregisterNode(AppDriver appDriver, Protos.SchedulerNodeInfo nodeInfo)
    {
        LOGGER.info("reregistering default schedulerNode..");

        //default to just scheduler node instance
        appDriver.reregisterNode(new SchedulerNode(nodeInfo));
    }

    public String getAppName()
    {
        return this.schedulerAppInfo.getName();
    }


    //called by user who is using SchedulerTask interface (ie. not SchedulerContainer)
    //this is needed because
    public TaskID getNewTaskId()
    {
        TaskID taskId = TaskID.newBuilder()
                .setValue( this.dbClient.getNewTaskID(this.getAppId().getValue()) )
                .build();

        LOGGER.info("Allocating new TaskId: "+taskId);
        return taskId;
    }

    //called by driver
    public SchedulerNode.State getState(Protos.NodeID nodeID)
    {
        DeployableNode node;

        if( (node = this.getNode(nodeID)) != null)
        {
            return node.getState();
        }

        return SchedulerNode.State.UNKNOWN;
    }

    //called by driver
    public List<Protos.SchedulerTask> getTasks()
    {
        List<Protos.SchedulerTask> tasks = new LinkedList<>();

        for(DeployableNode node : this.nodes)
        {
            tasks.add(node.getTask());
        }

        return tasks;
    }

    //called by driver
    public void updateTaskInfo(Protos.SchedulerTask task)
    {
        synchronized (this.nodes){
            DeployableNode node;
            if( (node = this.getNode(task.getNodeId())) != null )
            {
                node.updateTaskInfo( task );
            }
        }
    }

    //called by driver
    public void setDbClient(DatabaseClient dbClient)
    {
        this.dbClient = dbClient;
    }

    //called by driver
    public void setAppId(Protos.AppID appID)
    {
        if( this.schedulerAppInfo == null)
        {
            this.schedulerAppInfo = Protos.SchedulerAppInfo.newBuilder()
                    .setAppId( appID )
                    .build();
            if( this.name != null)
            {
                this.schedulerAppInfo = this.schedulerAppInfo.toBuilder()
                        .setName(this.name)
                        .build();
            }
        }
        else {
            this.schedulerAppInfo = this.schedulerAppInfo.toBuilder()
                    .setAppId(appID)
                    .build();

            if( this.name != null)
            {
                this.schedulerAppInfo = this.schedulerAppInfo.toBuilder()
                        .setName(this.name)
                        .build();
            }
        }
    }

    //called by driver
    public void setSchedulerAppInfo(Protos.SchedulerAppInfo schedulerAppInfo)
    {
        this.schedulerAppInfo = schedulerAppInfo;
    }

    //called by driver
    public Protos.AppID getAppId()
    {
        return this.schedulerAppInfo.getAppId();
    }

    //called by driver
    public void removeNode(DeployableNode node)
    {
        synchronized (this.nodes){
            Integer index;
            if( (index = this.getNodeIndex(node)) < 0 )
            {
                return;
            }


            this.nodes.remove(node);
        }
    }

    //called by driver
    public void addNode(DeployableNode node)
    {
        synchronized (this.nodes){

            //set back pointer to parent instance
            node.setParent( this );
            node.setNodeId( this.getNewNodeId() );
            node.setAppId( this.getAppId() );

            //add node to local list
            this.nodes.add(node);
        }
    }

    //called by driver
  /*  public void updateNodeInfo(DeployableNode node)
    {
        synchronized (this.nodes){

            Integer index;
            if( (index = this.getNodeIndex(node)) < 0 )
            {
                return;
            }

            //update node
            this.schedulerAppInfo = this.schedulerAppInfo.toBuilder()
                    .setNodes(index, node.getSchedulerNodeInfo())
                    .build();
        }
    }*/

    public void reregisterNodeInternal(DeployableNode node)
    {
        synchronized (this.nodes)
        {
            //check if its already there
            if( this.getNode(node.getNodeId()) != null )
            {
                return;
            }

            node.setParent( this );
            this.nodes.add(node);
        }
    }

    public List<DeployableNode> getNodes()
    {
        return this.nodes;
    }

    public void setSchedulerConfig(Protos.SchedulerConfig schedulerConfig)
    {
        this.schedulerConfig = schedulerConfig;
    }

    public Protos.SchedulerAppInfo getSchedulerAppInfo()
    {
        return this.schedulerAppInfo;
    }

    public Protos.SchedulerConfig getSchedulerConfig()
    {
        return this.schedulerConfig;
    }

    public synchronized byte[] getData()
    {
        Protos.SchedulerAppInfo.Builder builder = this.schedulerAppInfo.toBuilder();

        builder.clearNodes();

        for (DeployableNode node : this.nodes)
        {
            builder.addNodes(node.getSchedulerNodeInfo());
        }

        this.schedulerAppInfo = builder.build();
        return this.schedulerAppInfo.toByteArray();
    }

}
