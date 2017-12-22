package com.tbouvier.mesos.db;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tbouvier.mesos.scheduler.Protos;
import com.tbouvier.mesos.scheduler.Protos.AppID;
import com.tbouvier.mesos.scheduler.Protos.SchedulerConfig;
import com.tbouvier.mesos.util.Syslog;
import com.tbouvier.mesos.scheduler.Protos.SchedulerTask;
import com.tbouvier.mesos.util.*;

import java.util.LinkedList;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;


class ZkClient implements DatabaseClient, Watcher, Runnable, DataMonitorListener {

    
    private final SysLogger LOGGER = Syslog.getSysLogger(ZkClient.class);
    
    
    public static final String DEFAULT_FRAMEWORK_SANDBOX = "/appframework";
    public static final String DEFAULT_LEADER_ELECTION   = "/election";
    public static final String DEFAULT_LEADER_NODE       = "/leader-";
    public static final String DEFAULT_TASK_LIBRARY      = "/tasklibrary";
    public static final String DEFAULT_FRAMEWORK_CONFIG  = "/config";
    public static final String DEFAULT_FRAMEWORK_ID      = "/framework-id";
    public static final String DEFAULT_APP_LIBRARY       = "/apps";

    public static final long CONNECTION_TMOUT = 10000;

    private ZooKeeper zk;
    private DataMonitor dm;
    private Syslog syslog;
    private String zkAddress;
    private String appFrameworkSandbox;
    private String leaderElection;
    private String taskLibrary;
    private String configLibrary;
    private String appLibrary;
    private String frameworkId;
    private String leaderNode;
    private String currentAppId;
    private String currentNodeId;
    private String currentTaskId;
    private boolean isLeader;
    private int leaderId;
    private boolean initialized;
    private boolean syncConnected;
    private SchedulerConfig config;

     //leader election node
    private String zkLeaderNode;

    public ZkClient(String zkAddress, String rootNode, SchedulerConfig config)
    {
        this.config = config;
        this.appFrameworkSandbox = rootNode;
        this.leaderElection = this.appFrameworkSandbox+ZkClient.DEFAULT_LEADER_ELECTION;
        this.leaderNode = this.leaderElection+ZkClient.DEFAULT_LEADER_NODE;
        this.taskLibrary = this.appFrameworkSandbox+ZkClient.DEFAULT_TASK_LIBRARY;
        this.configLibrary = this.appFrameworkSandbox+ZkClient.DEFAULT_FRAMEWORK_CONFIG;
        this.appLibrary = this.appFrameworkSandbox+ZkClient.DEFAULT_APP_LIBRARY;
        this.frameworkId = this.appFrameworkSandbox+ZkClient.DEFAULT_FRAMEWORK_ID;
        this.currentAppId = "current-app-id";
        this.currentTaskId = "current-task-id";
        this.currentNodeId = "current-node-id";
        this.zkAddress = zkAddress;
        this.initialized = false;
    }


    //Returns true unless the znode didnt exist and creating it failed
    private boolean createIfNotExist(String path)
    {
        if( this.exists(path, false) == null )
        {

            if( this.create(path, null) == null )
            {
                ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                        "Failed to create sandbox directory");
                LOGGER.error( "Failed to create sandbox directory");
                return false;
            }
        }

        return true;
    }

    private void setupSandbox()
    {

         //Create initial sandbox directory
        if( !this.createIfNotExist(this.appFrameworkSandbox) )
        {
            return;
        }


         //Create leader election directory
        if( !this.createIfNotExist(this.leaderElection) )
        {
            return;
        }


         //Create task library
        if( !this.createIfNotExist(this.taskLibrary) )
        {
            return;
        }


         //Create app library
        if( !this.createIfNotExist(this.appLibrary) )
        {
            return;
        }

         //Create config dir
        if( !this.createIfNotExist(this.configLibrary) )
        {
            return;
        }
    }

    private String electLeader()
    {
        List<String> children;
        String ret;
        Integer id, myId;

        if( ( ret = this.create(this.leaderNode, this.config.getHost().getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL) ) == null )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_CRITICAL, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to create leader znode");
            LOGGER.error( "Failed to create leader znode for election. Aborting framework..");
            return null;
        }


         //gets children and places a watch on the election directory in case the leader failes.
        if( (children = this.getChildren(this.leaderElection, true) ) == null )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to get children of election node");
            LOGGER.error( "Failed to get children of election node");
            return null;
        }

        //FIXME: add try/catch block here in case sequence number fails to get assigned
        myId = new Integer( ret.substring( ret.lastIndexOf('-')+1 ) );
        this.leaderId = myId.intValue();

        for(String child : children)
        {
            id = new Integer( child.substring( child.lastIndexOf('-')+1 ) );

            if( id.intValue() < this.leaderId )
            {
                this.isLeader = false;
                //FIXME: take watch off of entire election dir and put it on the node one lower than this one's id
                //       this will help mitigate the 'herd' phenomenon when a leader fails

                return ret;
            }
        }

        this.isLeader = true;
        return ret;
    }

    private String constructTaskKey(SchedulerTask task)
    {
        return this.taskLibrary+"/task_"+task.getTaskInfo().getTaskId().getValue();
    }

    private String constructTaskKey(String taskId)
    {
        return this.taskLibrary+"/task_"+taskId;
    }

    private String constructConfigKey(String name)
    {
        return this.configLibrary+"/"+name;
    }

    private String constructAppKey(String appId)
    {
        return this.appLibrary+"/app-"+appId;
    }

    private String deconstructAppKey(String appKey)
    {
        return appKey.substring(4);//FIXME
    }

    private Stat exists(String path, boolean watcher)
    {
        Stat ret;

        try{
            ret = this.zk.exists(path, watcher);
        }catch(Exception e)
        {

            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exception during exists: "+e);
            return null;
        }

        return ret;
    }

    private List<String> getChildren(String path, boolean watch)
    {
        List<String> ret;

        if( path == null )
        {
            return null;
        }

        try{
            ret = this.zk.getChildren(path, watch);
        }catch(Exception e)
        {

            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exceptio during getchildren()");
            return null;
        }

        return ret;
    }

    private Stat setData(String path, byte[] data, int version)
    {
        Stat ret;

        if( path == null )
        {
            return null;
        }

        try{
            ret = this.zk.setData(path, data, version);
        }catch(Exception e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exception during setdata"+e);
            return null;
        }

        return ret;
    }

    private String create(String path, byte[] data)
    {
        String ret;

        if( path == null )
        {
            return null;
        }

        //FIXME: use permissions?
        return this.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    private String create(String path, byte[] data, List<ACL> acl, CreateMode mode )
    {
        String ret;

        try{
            ret = this.zk.create(path, data, acl, mode);
        }catch( Exception e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exceptio during create");
            return null;
        }

        return ret;
    }

    private boolean delete(String path, int version)
    {
        try{
            this.zk.delete(path, version);
        }catch(Exception e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exceptio during delete()");
            return false;
        }

        return true;
    }

    private byte[] getData(String key, boolean watch, Stat stat)
    {
        byte[] data;

        try{
            data = this.zk.getData(key, watch, stat);
        }catch(Exception e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exceptio during getdata()");
            return null;
        }

        return data;
    }

    private byte[] getData(String key)
    {
        Stat stat;
        byte[] data;

        if( ( stat = this.exists(key, false)) == null )
        {
            return null;
        }

        if( (data = this.getData(key, false, stat) ) == null )
        {
            return null;
        }

        return data;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////    Public Methods         //////////////////////////////////////
    //////////////////////////////////////                           //////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean initialize()
    {
        long t0;

        if( (this.zkAddress == null) || this.initialized )
        {
            return false;
        }

        LOGGER.info( "[initialize] STARTING ZOOKEEPER INIT");


         //setup dm before we register with zk
        this.dm = new DataMonitor( this );

        try{
            this.zk = new ZooKeeper( zkAddress, 3000 /*FIXME*/, this);
        }catch(IOException e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_CRITICAL, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "caught exceptio during initialize()");
            LOGGER.error( "Caught exception while instantiating zookeeper object..");
            this.zk = null;
            return false;
        }


         //Wait for syncConnected event
        //FIXME: wait() notify()
        t0 = System.currentTimeMillis();
        while( (System.currentTimeMillis() - t0) < ZkClient.CONNECTION_TMOUT )
        {
            if( this.syncConnected )
            {
                LOGGER.info( "[initialize] received syncConnect....breaking wait loop");
                break;
            }
        }
        //if were not connected at this point then leader election will fail and fw will be aborted

        this.setupSandbox();


         //If we fail to create our leader node then the framework needs to be aborted
        if( (this.zkLeaderNode = this.electLeader()) == null )
        {
            return false;
        }

        this.initialized = true;

        LOGGER.info(
                "Successfully initialized zookeeper: ["+this.zkAddress+"] leader: ["+this.zkLeaderNode+"]");
        return true;
    }

    public void process(WatchedEvent event)
    {
        if( this.dm == null )
        {
            LOGGER.error(
                    "Data monitor is null during process");
            return;
        }

        this.dm.process( event );
    }

    public void setSyncConnected()
    {
        this.syncConnected = true;
    }

    public void clearSyncConnected()
    {
        this.syncConnected = false;
    }

    public String getElectionNode()
    {
        return this.leaderElection;
    }

    public void evaluateLeader()
    {
        List<String> children;
        String ret;
        Integer id, myId;


         //gets children and places a watch on the election directory in case the leader failes.
        if( (children = this.getChildren(this.leaderElection, true) ) == null )
        {
            LOGGER.error( "Failed to get children of election node");
            return;
        }

        for(String child : children)
        {
            id = new Integer( child.substring( child.lastIndexOf('-')+1 ) );

            if( id.intValue() < this.leaderId )
            {
                this.isLeader = false;
                //FIXME: take watch off of entire election dir and put it on the node one lower than this one's id
                //       this will help mitigate the 'herd' phenomenon when a leader fails
                return;
            }
        }

        LOGGER.warn( "This instance elected as new leader");
        this.isLeader = true;
    }

    public void closing(int rc)
    {
        //FIXME: if this happens then prevent further tasks from being launched from this instance
        //and possible spins trying to reconnect to zookeeper?
        LOGGER.error( "Zookeeper closing connection");
    }

    public void exists(byte[] data)
    {
        //NYI
    }

    public String getNewAppID()
    {
        return RandomUtils.getNew32ByteString();
    }

    public String getNewTaskID(String appId)
    {
        return appId+"-"+this.getAndIncrement( this.currentTaskId );
    }

    public String getNewNodeID(String appId)
    {
        return appId+"-"+this.getAndIncrement( this.currentNodeId );
    }

    public synchronized Integer getAndIncrement(String configNode)
    {
        Integer ret;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( !this.configExists( configNode ) )
        {
            if( !this.createConfig(configNode, 0) )
            {
                ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                        "Failed to create config node: "+configNode);
                return null;
            }
        }

        if( (ret = this.getInt(configNode)) == null)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to get config node: "+configNode);
            return null;
        }

        //increment value
        ret++;

        //write incremented value back to zk
        if( !this.setInt(configNode, ret) )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to increment config node: "+configNode);
            return null;
        }

        return ret;
    }

    public boolean insertTask(SchedulerTask task)
    {
        byte[] data;
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (key = this.constructTaskKey( task ) ) == null )
        {
            return false;
        }

        //serialize protobuf
        data = task.toByteArray();


        //Insert task data into zookeeper taskLibrary synchronously
        if( this.create(key, data ) == null )
        {
            return false;
        }

        return true;
    }

    public boolean updateTask(SchedulerTask task)
    {
        byte[] data;
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (key = this.constructTaskKey( task ) ) == null )
        {
            return false;
        }


        data = task.toByteArray();

        if( this.setData(key, data, -1) == null )
        {
            return false;
        }

        return true;
    }

    public SchedulerTask getTask(String taskId)
    {
        String key;
        byte[] data;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( taskId == null )
        {
            return null;
        }

        if( (key = this.constructTaskKey( taskId ) ) == null )
        {
            return null;
        }

        if( (data = this.getData( key )) == null )
        {
            return null;
        }

        try{
            return SchedulerTask.parseFrom( data );
        }catch(InvalidProtocolBufferException e){
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to build schedulertask: "+key+" from protobuf");
            return null;
        }
    }

    public List<SchedulerTask> getTasks()
    {
        SchedulerTask task;
        String taskId;
        List<String> children;
        List<SchedulerTask> tasks = new LinkedList<>();

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( (children = this.getChildren(this.taskLibrary, false)) == null )
        {
            LOGGER.error( "getChildren returned null");
            return null;
        }

        for(String child : children)
        {
            taskId = child.substring( child.lastIndexOf('_') + 1);

            if( (task = this.getTask( taskId )) != null )
            {
                tasks.add( task );
            }
        }

        return tasks;
    }


    public boolean taskExists(String taskId)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( taskId == null)
        {
            return false;
        }

        if( (key = this.constructTaskKey( taskId ) ) == null )
        {
            return false;
        }

        if( this.exists( key, false) == null )
        {
            return false;
        }

        return true;
    }

    public boolean configExists(String varName)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( varName == null )
        {
            return false;
        }

        if( (key = this.constructConfigKey(varName)) == null )
        {
            return false;
        }

        if( this.exists(key, false) == null )
        {
            return false;
        }

        return true;
    }


    public boolean deleteTask(SchedulerTask task)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( task == null )
        {
            return false;
        }

        if( (key = this.constructTaskKey( task ) ) == null )
        {
            return false;
        }

        LOGGER.warn("DELETING task: "+key);
        return this.delete(key, -1);
    }

    //FIXME: remove this at some point
    public void cleanup()
    {
        for(String child : this.getChildren(this.taskLibrary, false))
        {
            LOGGER.warn( "Deleting child task znode: "+child);
            this.delete(this.taskLibrary+"/"+child, -1);
        }

        for(String child : this.getChildren(this.appLibrary, false))
        {
            LOGGER.warn( "Deleting child APP znode: "+child);
            this.delete(this.appLibrary+"/"+child, -1);
        }

        for(String child : this.getChildren(this.configLibrary, false))
        {
            LOGGER.warn( "Deleting child CONFIG znode: "+child);
            this.delete(this.configLibrary+"/"+child, -1);
        }
    }

    public boolean deleteTask(String task)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( task == null )
        {
            return false;
        }

        if( (key = this.constructTaskKey( task ) ) == null )
        {
            return false;
        }

        return this.delete(key, -1);
    }

    public boolean deleteApp(String app)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( app == null )
        {
            return false;
        }

        if( (key = this.constructAppKey( app ) ) == null )
        {
            return false;
        }

        return this.delete(key, -1);
    }

    public boolean isLeader()
    {
        return this.isLeader;
    }

    public Integer getInt(String name)
    {
        Integer ret;
        String key;
        byte[] value;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( name == null )
        {
            return null;
        }

        if( (key = this.constructConfigKey(name) ) == null )
        {
            return null;
        }

        if( (value = this.getData( key )) == null )
        {
            return null;
        }

        try
        {
            ret = new Integer( new String(value) );
        }
        catch(Exception e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_MEDIUM, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Caught exception processing getInt(): "+e);
            return null;
        }

        return ret;
    }

    public boolean setInt(String name, Integer value)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (name == null) || (value == null) )
        {
            return false;
        }

        if( (key = this.constructConfigKey(name) ) == null )
        {
            return false;
        }

        if( this.setData(key, value.toString().getBytes(), -1) == null )
        {
            return false;
        }

        return true;
    }

    public boolean createConfig(String name, int initialVal)
    {
        String key;
        Integer value = initialVal;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( name == null )
        {
            return false;
        }

        if( (key = this.constructConfigKey(name)) == null )
        {
            return false;
        }

        if( this.create(key, value.toString().getBytes()) == null )
        {
            return false;
        }

        return true;
    }

    public boolean addApp(String appId)
    {
        String key;
        Integer value;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (key = this.constructAppKey(appId)) == null )
        {
            return false;
        }

        if( this.create(key, null) == null )
        {
            return false;
        }

        return true;
    }

    public boolean updateApp(String appId, byte[] data)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (key = this.constructAppKey(appId)) == null )
        {
            return false;
        }

        if( this.setData(key, data, -1) == null )
        {
            return false;
        }

        return true;
    }

    public Protos.SchedulerAppInfo getApp(AppID appID)
    {
        String key;
        byte[] data;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( appID == null)
        {
            return null;
        }

        if( (key = this.constructAppKey(appID.getValue())) == null )
        {
            return null;
        }

        if( (data = this.getData( key )) == null )
        {
            return null;
        }

        try{
            return Protos.SchedulerAppInfo.parseFrom( data );
        }catch(InvalidProtocolBufferException e) {
            ErrorTrace.Trace(ErrorCodes.SEV_HIGH, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Failed to deserialize schedulerApp: "+appID.getValue());
        }

        return null;
    }

    public List<AppID> getApps()
    {
        List<AppID> apps = new LinkedList<>();

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        for(String child : this.getChildren(this.appLibrary, false))
        {
            apps.add( AppID.newBuilder()
                    .setValue( this.deconstructAppKey( child ) )
                    .build() );
        }

        return apps;
    }

    public boolean setFrameworkId(String fid)
    {
        String key;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return false;
        }

        if( (key = this.frameworkId) == null )
        {
            return false;
        }


        //if its not there create it else
        //overwrite it
        if( this.exists(key, false) == null )
        {
            if( this.create(key, fid.getBytes()) == null )
            {
                return false;
            }
        }
        else
        {
            if( this.setData(key, fid.getBytes(), -1) == null )
            {
                return false;
            }
        }

        return true;
    }

    public String getFrameworkId()
    {
        String key;
        byte[] value;

        if( !this.initialized || !this.isLeader )
        {
            ErrorTrace.Trace(ErrorCodes.SEV_LOW, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "not leader or not initialized");
            return null;
        }

        if( (key = this.frameworkId) == null )
        {
            return null;
        }

        if( this.exists(key, false) == null )
        {
            return null;
        }

        if( (value = this.getData( key )) == null )
        {
            return null;
        }

        return new String( value );
    }


    public void run()
    {
        try {

            while( this.dm.isAlive() )
            {
                Thread.sleep( 1 );
            }
        }catch( InterruptedException e)
        {
            ErrorTrace.Trace(ErrorCodes.SEV_CRITICAL, ErrorCodes.ZOOKEEPER, ErrorCodes.DATABASE_FAILURE,
                    "Datamonitor died. disconnecting from zookeeper. Stepping down from leader position");
            this.isLeader = false;

            //schedule event to abort driver before we disconnect from zookeeper**/
            LOGGER.error("DataMonitor died!");
            //FIXME: if not already done by app disconnect from zookeeper here so another leader
            //can be elected if nesessary

        }
    }
}
