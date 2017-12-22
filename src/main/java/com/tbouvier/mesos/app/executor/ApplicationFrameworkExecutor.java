package com.tbouvier.mesos.app.executor;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;

import com.tbouvier.mesos.scheduler.Protos.FrameworkMessage;

import java.net.InetAddress;

/**
 * Created by bouviti on 5/5/17.
 */
public class ApplicationFrameworkExecutor implements AppFrameworkExecutor, AppExecutor {

    private final SysLogger LOGGER = Syslog.getSysLogger(ApplicationFrameworkExecutor.class);

    private ExecutorDriver driver;
    private Protos.FrameworkInfo frameworkInfo;
    private Protos.SlaveInfo slaveInfo;
    private Protos.ExecutorInfo executorInfo;
    private Protos.TaskInfo taskInfo;
    private Protos.TaskStatus taskStatus;
    private String ipAddress;

    //the app that will be run
    private ExecutableApp app;
    private AppThreadWrapper appThreadWrapper;
    private Thread appThread;

    @Override
    public void registered(ExecutorDriver executorDriver,
                           Protos.ExecutorInfo executorInfo,
                           Protos.FrameworkInfo frameworkInfo, Protos.SlaveInfo slaveInfo) {
        LOGGER.info(
                "Executor: "+executorInfo.getExecutorId().getValue()+"registered on slave: "+slaveInfo.getId().getValue());

        this.driver = executorDriver;
        this.frameworkInfo = frameworkInfo;
        this.slaveInfo = slaveInfo;
        this.executorInfo = executorInfo;
        this.ipAddress = this.getIpAddress();
        LOGGER.info("detected container ip: "+this.ipAddress);
    }

    private String getIpAddress()
    {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch(Exception e)
        {
            LOGGER.error("Caught exception during getIpaddress: "+e);
            e.printStackTrace();
        }

        return null;//throw exception?
    }

    private void initializeTaskStatus() {

        if( this.taskStatus != null)
        {
            LOGGER.info("Init called but taskstatus is not null...");
            return;
        }
        LOGGER.info("Initializing task status with ip: "+this.ipAddress);

        Protos.TaskStatus.Builder builder = Protos.TaskStatus.newBuilder();

        builder
                .setSlaveId(this.slaveInfo.getId())
                .setExecutorId(this.executorInfo.getExecutorId())
                .setTaskId(this.taskInfo.getTaskId());

        //add ip of container (
        //FIXME: figure this out from schedulerconfig?
        builder.setContainerStatus(Protos.ContainerStatus.newBuilder()
                .addNetworkInfos(Protos.NetworkInfo.newBuilder()
                        .addIpAddresses(Protos.NetworkInfo.IPAddress.newBuilder()
                                .setIpAddress(this.ipAddress)
                                .setProtocol(Protos.NetworkInfo.Protocol.IPv4)
                                .build())
                        .build())
                .build());

        /*if( this.taskInfo.hasContainer()
                && this.taskInfo.getContainer().hasDocker())
        {*/
        //FIXME: figure this out from schedulerconfig?
        builder.setLabels(Protos.Labels.newBuilder()
                .addLabels(Protos.Label.newBuilder()
                        .setKey("Docker.NetworkSettings.IPAddress")
                        .setValue(this.ipAddress)
                        .build())
                .build());

        builder.setState(Protos.TaskState.TASK_STARTING);

        this.taskStatus = builder.build();
    }

    private void sendFrameworkMessage(FrameworkMessage message)
    {
        try {
            this.driver.sendFrameworkMessage(message.toByteArray());
        }catch (Exception e)
        {
            LOGGER.error( "Caught exception during fw message send: "+e);
            e.printStackTrace();
        }
    }


    public void setExecutableApp(ExecutableApp app)
    {
        this.app = app;
    }

    public synchronized void updateTaskState(Protos.TaskState state)
    {
        if( this.taskInfo == null)
        {
            return;
        }

        try{
            this.taskStatus = this.taskStatus.toBuilder()
                    .setState(state)
                    .build();
            LOGGER.info("updating taskstatus with lables size: "+this.taskStatus.getLabels().getLabelsList().size());
            this.driver.sendStatusUpdate(this.taskStatus);
        }catch (Exception e){
            LOGGER.error("Caught exception in updateTaskStatus");
            e.printStackTrace();
        }
    }

    @Override
    public void reregistered(ExecutorDriver executorDriver,
                             Protos.SlaveInfo slaveInfo) {
        LOGGER.info( "Executor reregistered");
    }

    @Override
    public void disconnected(ExecutorDriver executorDriver) {
        LOGGER.info( "Executor disconnected");
        //FIXME: notify app here? maybe shutdown?
    }

    @Override
    public void launchTask(ExecutorDriver executorDriver,
                           Protos.TaskInfo taskInfo) {
        LOGGER.info( "Launching task: "+taskInfo.getTaskId().getValue());

        //setup taskStatus
        this.taskInfo = taskInfo;
        this.initializeTaskStatus();

        try {
            //start the app
            this.appThreadWrapper = new AppThreadWrapper(this.app, this, this);
            this.appThread = new Thread(this.appThreadWrapper);
            this.appThread.start();

            this.updateTaskState(Protos.TaskState.TASK_RUNNING);
        }catch (Exception e)
        {
            this.updateTaskState(Protos.TaskState.TASK_FAILED);
        }
    }

    @Override
    public void killTask(ExecutorDriver executorDriver, Protos.TaskID taskID) {

        try{
            this.app.kill();
        }catch (Exception e)
        {
            LOGGER.error("Caught exception during kill task");
            e.printStackTrace();
        }
    }

    @Override
    public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {

        FrameworkMessage message;

        try{
            message = FrameworkMessage.parseFrom( ByteString.copyFrom(bytes));
        }catch (InvalidProtocolBufferException e)
        {
            LOGGER.error("Failed to parse fw message body: "+e);
            e.printStackTrace();
            return;
        }

        try {
            this.app.message(this, message.getData().toByteArray());
        }catch (Exception e)
        {
            LOGGER.error("caught exception in app message callback");
            return;
        }
    }

    @Override
    public void shutdown(ExecutorDriver executorDriver) {
        LOGGER.error("Executor asked to shutdown");

        try {
            this.app.kill();
            this.driver.stop();
        }
        catch (Exception e)
        {
            LOGGER.error("caught exception: "+e);
            e.printStackTrace();
        }

        LOGGER.info("Driver stop called. Executor terminating");
    }

    @Override
    public void error(ExecutorDriver executorDriver, String s) {
        LOGGER.error("Executor driver reported error: "+s);
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    //////////////////////// AppExecutor Interface //////////////////////////////
    ////////////////////////                       //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    public void message(byte[] data)
    {
        try {
            FrameworkMessage message = FrameworkMessage.newBuilder()
                    .setTaskId(this.taskInfo.getTaskId())
                    .setData(ByteString.copyFrom(data))
                    .build();
            this.sendFrameworkMessage( message );
        }catch (Exception e)
        {
            LOGGER.error("Caught exception in message call");
        }
    }

    public void shutdown()
    {
        this.shutdown(this.driver);
    }

}
