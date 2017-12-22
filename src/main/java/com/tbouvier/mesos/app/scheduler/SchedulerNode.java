package com.tbouvier.mesos.app.scheduler;

import com.google.protobuf.ByteString;
import com.google.protobuf.UninitializedMessageException;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Executor;
import org.apache.mesos.Protos.Volume;


import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.ContainerInfo;
import org.apache.mesos.Protos.Parameter;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;

import java.util.LinkedList;
import java.util.List;

import static org.apache.mesos.Protos.Value.Type.SCALAR;

/**
 * Created by bouviti on 5/2/17.
 */
public class SchedulerNode implements DeployableNode {

    public enum State{
        UNKNOWN,
        INITIALIZING,
        RUNNING,
        FAILED,
        KILLED,
        FINISHED,
        RELAUNCH,
        REREGISTERED
    }
    
    private final SysLogger LOGGER = Syslog.getSysLogger(SchedulerNode.class);

    private SchedulerApp parent;
    private Protos.SchedulerNodeInfo schedulerNodeInfo;
    private State state;

    public SchedulerNode(Protos.SchedulerNodeInfo schedulerNodeInfo)
    {
        this.schedulerNodeInfo = schedulerNodeInfo;
        this.state = State.INITIALIZING;

        if( !schedulerNodeInfo.hasSchedulerContainer() && !schedulerNodeInfo.hasSchedulerTask() )
        {
            //just report containerInfo if this happens
            //probably advanced users will be using SchedulerTask
            List<String> missing = new LinkedList<>();
            missing.add("SchedulerContainerInfo");
            throw new UninitializedMessageException(missing);
        }

    }

    public void setSchedulerNodeInfo(Protos.SchedulerNodeInfo schedulerNodeInfo)
    {
        this.schedulerNodeInfo = schedulerNodeInfo;
    }

    public void message(AppDriver appDriver, byte[] message)
    {

    }

    public void failed(AppDriver appDriver)
    {
        LOGGER.info("Node failed. issuing relaunch for node: "+this.getNodeId().getValue());
        appDriver.relaunchNode( this.parent.getNode(this.getNodeId()) );
    }

    public void running(AppDriver appDriver)
    {
        System.out.println("this is bad");
    }

    public void relaunch(AppDriver appDriver)
    {

    }

    public void finished(AppDriver appDriver)
    {
        System.out.println("this is ok");
    }

    public void killed(AppDriver appDriver)
    {
        this.failed(appDriver);
    }

    private List<Volume> getVolumeList(List<Protos.SchedulerVolume> volumes)
    {
        List<Volume> ret = new LinkedList<>();

        for(Protos.SchedulerVolume volume : volumes)
        {
            if( volume.hasHostMountPoint())
            {
                ret.add(Volume.newBuilder()
                        .setContainerPath(volume.getContainerMountPoint())
                        .setHostPath(volume.getHostMountPoint())
                        .setMode(volume.getMode())
                        .build());
            }
            else {//for volumes with volume driver -- these are added as docker params
                /*ret.add(Volume.newBuilder()
                        .setContainerPath(volume.getContainerMountPoint())
                        .setMode(volume.getMode())
                        .build());*/
            }
        }

        return ret;
    }

    private List<Resource> getResourceList(Protos.SchedulerContainer container)
    {
        List<Resource> ret = new LinkedList<>();

        ret.add( Resource.newBuilder()
                .setName("cpus")
                .setType( SCALAR )
                .setScalar(Scalar.newBuilder()
                        .setValue( container.getCpus())
                        .build())
                .build());

        ret.add( Resource.newBuilder()
                .setName("mem")
                .setType( SCALAR )
                .setScalar(Scalar.newBuilder()
                        .setValue( container.getMemory() )
                        .build())
                .build());

        return ret;
    }

    public String getNodeName()
    {
        if( this.schedulerNodeInfo.hasSchedulerTask())
        {
            return this.schedulerNodeInfo.getSchedulerTask().getTaskInfo().getName();
        }

        return this.schedulerNodeInfo.getSchedulerContainer().getName();
    }

    public double getCpus()
    {
        if( this.parent == null)
        {
            if( this.schedulerNodeInfo.hasSchedulerContainer())
            {
                return this.schedulerNodeInfo.getSchedulerContainer().getCpus();
            }
        }


        Protos.SchedulerTask task = this.getTask();

        for(Resource resource : task.getTaskInfo().getResourcesList())
        {
            if( resource.getName().equals("cpus") )
            {
                return resource.getScalar().getValue();
            }
        }

        return 0;
    }

    public double getMem()
    {
        if( this.parent == null)
        {
            if( this.schedulerNodeInfo.hasSchedulerContainer())
            {
                return this.schedulerNodeInfo.getSchedulerContainer().getMemory();
            }
        }

        Protos.SchedulerTask task = this.getTask();

        for(Resource resource : task.getTaskInfo().getResourcesList())
        {
            if(resource.getName().equals("mem") )
            {
                return resource.getScalar().getValue();
            }
        }

        return 0;
    }


    private void convertContainerInfoToTaskInfo()
    {
        if( !this.schedulerNodeInfo.hasSchedulerContainer() || (this.parent == null))
        {
            return;
        }

        Protos.SchedulerContainer container = this.schedulerNodeInfo.getSchedulerContainer();
        Protos.SchedulerConfig schedulerConfig = this.parent.getSchedulerConfig();
        List<CommandInfo.URI> uris = new LinkedList<>();
        TaskInfo.Builder taskInfo = TaskInfo.newBuilder();
        List<Protos.SchedulerConstraint> constraints = new LinkedList<>();


        //add uris from scheduler config
        if( schedulerConfig.hasDockerRegistryInfo() )
        {
            for(CommandInfo.URI uri : schedulerConfig.getDockerRegistryInfo().getUrisList())
            {
                uris.add(uri);
            }
        }

        //add additional URI if specified in SchedulerContainer
        for(CommandInfo.URI uri : container.getUrisList())
        {
            uris.add(uri);
        }

        synchronized ( this ) {

            //clear the task info if it exists
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .clearSchedulerTask()
                    .build();

            if (container.hasContainerInfo())
            {
                if( container.hasExecutor())
                {
                    taskInfo
                            .setExecutor(ExecutorInfo.newBuilder()
                                    .setCommand(CommandInfo.newBuilder()
                                            .setShell( false )
                                            .addAllUris( uris )
                                            .build())
                                    .setContainer( container.getContainerInfo() )
                                    .addAllResources(this.getResourceList(container))
                                    .setName(this.getNodeName())
                                    .setExecutorId(ExecutorID.newBuilder()
                                            .setValue(this.getNodeName())
                                            .build())
                                    .build())
                            .setSlaveId(SlaveID.newBuilder()
                                    .setValue("notarealslave")
                                    .build())
                            .setTaskId(this.parent.getNewTaskId())
                            .setName(this.getNodeName());
                }
                else
                {
                    taskInfo
                            .setCommand(CommandInfo.newBuilder()
                                    .setShell(false)
                                    .addAllUris(uris)
                                    .build())
                            .setContainer(container.getContainerInfo())
                            .addAllResources(this.getResourceList(container))
                            .setName(this.getNodeName())
                            .setSlaveId(SlaveID.newBuilder()
                                    .setValue("notarealslave")
                                    .build())
                            .setTaskId(this.parent.getNewTaskId());

                }
            }
            else if( container.hasContainerLiteInfo() )
            {
                ContainerInfo.DockerInfo.Builder dockerInfo = ContainerInfo.DockerInfo.newBuilder()
                        .setForcePullImage(true)
                        .setImage(container.getContainerLiteInfo().getDockerImage())
                        .setNetwork(org.apache.mesos.Protos.ContainerInfo.DockerInfo.Network.BRIDGE);

                if( container.getContainerLiteInfo().getContstraintsCount() > 0)
                {
                    constraints.addAll(container.getContainerLiteInfo().getContstraintsList());
                }

                for(Protos.SchedulerVolume volume : container.getContainerLiteInfo().getVolumesList())
                {
                    if( !volume.hasHostMountPoint()) {
                        dockerInfo.addParameters(Parameter.newBuilder()
                                .setKey("volume")
                                .setValue(volume.getName() + ":" + volume.getContainerMountPoint())
                                .build());
                    }

                    //FIXME: can you use more than one volume driver per container instance?
                    if( volume.hasVolumeDriver() )
                    {
                        dockerInfo.addParameters(Parameter.newBuilder()
                                .setKey("volume-driver")
                                .setValue(volume.getVolumeDriver())
                                .build());
                    }

                    if( volume.getConstraintsCount() > 0 )
                    {
                       for(Protos.SchedulerConstraint constraint : volume.getConstraintsList())
                       {
                           constraints.add(constraint);
                       }
                    }

                }

                ContainerInfo.Builder containerInfo = ContainerInfo.newBuilder()
                        .setDocker( dockerInfo.build() )
                        .setType( ContainerInfo.Type.DOCKER );

                if( container.getContainerLiteInfo().getVolumesCount() != 0)
                {
                    containerInfo.addAllVolumes( this.getVolumeList( container.getContainerLiteInfo().getVolumesList()));
                }

                if( container.hasExecutor() )
                {
                    taskInfo
                            .setExecutor(ExecutorInfo.newBuilder()
                                    .setContainer( containerInfo.build() )
                                    .setCommand(CommandInfo.newBuilder()
                                        .setShell(false)
                                        .addAllUris(uris)
                                        .build())
                                    .setName(this.getNodeName())
                                    .setExecutorId(ExecutorID.newBuilder()
                                        .setValue(this.getNodeName()) //FIXME: use something else for executorId?
                                        .build())
                                    .addAllResources(this.getResourceList(container))
                                    .build())
                            .setName( this.getNodeName() )
                            .setSlaveId(SlaveID.newBuilder()
                                    .setValue("notarealslave")
                                    .build())
                            .setTaskId( this.parent.getNewTaskId())
                            .setName( this.getNodeName() )
                            .addAllResources(this.getResourceList(container));
                }
                else
                {
                    taskInfo.setCommand(CommandInfo.newBuilder()
                            .setShell(false)
                            .addAllUris(uris)
                            .build())
                            .setContainer(containerInfo.build())
                            .addAllResources(this.getResourceList(container))
                            .setName(this.getNodeName())
                            .setTaskId( this.parent.getNewTaskId() )
                            .setSlaveId(SlaveID.newBuilder()
                                    .setValue("notarealslaveid")//FIXME: maybe hold the builder somewhere instead?
                                    .build());
                }

            }
            else
            {
                return;
            }


            //set the schedulerTask
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setSchedulerTask(Protos.SchedulerTask.newBuilder()
                            .setTaskInfo(taskInfo.build())
                            .setAppId(this.getAppId())
                            .setNodeId(this.getNodeId())
                            .build())
                    .build();

            //add constraints
            if( constraints.size() > 0)
            {
                this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                        .setSchedulerTask( this.schedulerNodeInfo.getSchedulerTask().toBuilder()
                                .addAllConstraints(constraints)
                                .build())
                        .build();
            }
        }

    }

    public void setState(State state)
    {
        synchronized (this.state)
        {
            this.state = state;
        }
    }

    public State getState()
    {
        return this.state;
    }

    public Protos.NodeID getNodeId()
    {
        return this.schedulerNodeInfo.getNodeId();
    }

    public Protos.AppID getAppId()
    {
        return this.schedulerNodeInfo.getAppId();
    }

    public Protos.SchedulerNodeInfo getSchedulerNodeInfo()
    {
        return this.schedulerNodeInfo;
    }

    public void changeState(Protos.SchedulerNodeInfo.STATE state)
    {
        synchronized (this){
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setState( state )
                    .build();
        }
    }

    public synchronized Protos.SchedulerTask getTask()
    {
        //if we are relaunching the node then it needs a new mesos taskId
        if( this.getState().equals(State.RELAUNCH) )
        {
               this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                        .setSchedulerTask( this.schedulerNodeInfo.getSchedulerTask().toBuilder()
                                .setTaskInfo( this.schedulerNodeInfo.getSchedulerTask().getTaskInfo().toBuilder()
                                        .setTaskId(this.parent.getNewTaskId())
                                        .build())
                                .clearStatus()//so we process status update if its the same as the pervious one
                                .build())
                        .build();

                //set state to initializing so this can't be accidently called twice
                this.setState(State.INITIALIZING);
                return this.schedulerNodeInfo.getSchedulerTask();
        }

        if( this.schedulerNodeInfo.hasSchedulerTask() )
        {
            return this.schedulerNodeInfo.getSchedulerTask();
        }

        //convert SchedulerContainer into schedulerTask
        this.convertContainerInfoToTaskInfo();
        return this.schedulerNodeInfo.getSchedulerTask();
    }


    public void setParent(SchedulerApp app)
    {
        this.parent = app;
    }

    public void setAppId(Protos.AppID appId)
    {
        synchronized (this){
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setAppId(appId)
                    .build();
        }
    }

    public void setNodeId(Protos.NodeID nodeId)
    {
        synchronized (this){
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setNodeId(nodeId)
                    .build();
        }
    }

    public void updateTaskInfo(Protos.SchedulerTask task)
    {
        synchronized (this){
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setSchedulerTask( task )
                    .build();
        }
    }

    public SlaveID getSlaveId()
    {
        if( this.getTask().hasStatus()
            && this.getTask().getStatus().hasSlaveId())
        {
            return this.getTask().getStatus().getSlaveId();
        }

        return null;
    }

    public ExecutorID getExecutorId()
    {
        if( this.getTask().hasStatus()
            && this.getTask().getStatus().hasExecutorId())
        {
            return this.getTask().getStatus().getExecutorId();
        }
        else if( this.getTask().hasStatus()
            && this.getTask().getTaskInfo().hasExecutor()
            && this.getTask().getTaskInfo().getExecutor().hasExecutorId())
        {
            return this.getTask().getTaskInfo().getExecutor().getExecutorId();
        }

        return null;
    }

    public void updateUserData(byte[] data)
    {
        synchronized (this)
        {
            this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                    .setUserData(ByteString.copyFrom(data))
                    .build();
        }
    }

    public boolean hasUserData()
    {
        return this.schedulerNodeInfo.hasUserData();
    }

    public byte[] getUserData()
    {
        return this.schedulerNodeInfo.getUserData().toByteArray();
    }

    //prioritize dns over ip
    public void updateAddress(Protos.SchedulerTask task)
    {
        if( task.getTaskInfo().hasDiscovery() )
        {
            synchronized (this) {
                this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                        .setAddress(task.getTaskInfo().getDiscovery().getName())
                        .build();
            }
        }
        else if( task.hasStatus() && task.getStatus().hasLabels())
        {
            for(org.apache.mesos.Protos.Label label : task.getStatus().getLabels().getLabelsList())
            {
                if( label.hasKey() && label.getKey().equals("Docker.NetworkSettings.IPAddress") )
                {
                    synchronized (this)
                    {
                        this.schedulerNodeInfo = this.schedulerNodeInfo.toBuilder()
                                .setAddress(label.getValue())
                                .build();
                    }
                }
            }
        }
    }

    public boolean hasAddress()
    {
        return this.schedulerNodeInfo.hasAddress();
    }

    public String getAddress()throws Exception
    {
        if( !this.hasAddress() )
        {
            throw new Exception("Address not found");
        }

        return this.schedulerNodeInfo.getAddress();
    }

    public boolean hasSlaveId()
    {
        return ( this.getSlaveId()!= null );
    }

    public boolean hasExecutorId()
    {
        return ( this.getExecutorId() != null );
    }

}
