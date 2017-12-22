package com.tbouvier.mesos.db;

import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

class DataMonitor implements Watcher, StatCallback {

    private final SysLogger LOGGER = Syslog.getSysLogger(DataMonitor.class);
    
    private boolean alive;
    private DataMonitorListener listener;

    public DataMonitor(DataMonitorListener listener)
    {
        this.alive = true;
        this.listener = listener;
    }

    public void process(WatchedEvent event)
    {
        String path;
        /**
         **FIXME: this is where we will get the watch event if a leader / standby
         **       framework instance fails. When that happens we need to look at the election zNode
         **       children and decide if we are the leader now based on the sequence number on the
         **       ephemeral sequential zNodes
         **/
        path = event.getPath();
        LOGGER.info( "Got watchedEven for znode: "+path);

        if( (this.listener.getElectionNode().equals( path ))
                && (event.getType() == Watcher.Event.EventType.NodeChildrenChanged))
        {
            this.listener.evaluateLeader();
        }

        if( event.getType() == Watcher.Event.EventType.None )
        {
            switch( event.getState() )
            {
                case SyncConnected:
                    LOGGER.info( "Recieved SyncConnected event from zookeeper");
                    this.syncConnected();
                    break;
                case Expired:
                    LOGGER.error( "Received Expired event from zookeeper");
                    //FIXME: clear syncConnected flag and block zk writes?
                    this.terminate();
                    break;
                case AuthFailed:
                    LOGGER.error( "Received AuthFailed event from zookeeper");
                    this.terminate();
                    break;
                case Disconnected:
                    LOGGER.error( "Received disconnected event from zookeeper");
                    //FIXME: clear syncConnected flag and block zk writes?
                    break;
                default:
                    break;

            }
        }
    }

    private void terminate()
    {
        this.listener.closing( -1 );
        this.alive = false;
    }

    private void syncConnected()
    {
        if( this.listener != null )
        {
            this.listener.setSyncConnected();
        }
    }

    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
        //should really implement the completion path and block writes against completion from zk api
    }

    public boolean isAlive()
    {
        return this.alive;
    }

}
