package com.tbouvier.mesos.util;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class ErrorTrace {

    private static final String zkErrorId = "current-error-id";

    private List<TraceEntry> buff;
    private int lockTmout;
    //private ZkClient zooKeeper;

    private static ErrorTrace errorTrace;

    public ErrorTrace()
    {
        //this.zooKeeper = zooKeeper;
        this.buff = new LinkedList<TraceEntry>();
    }

    public static void createNewErrorTrace()
    {
        ErrorTrace.errorTrace = new ErrorTrace();
    }

    private String getTimeOfDay()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        String formattedDate = sdf.format(date);

        return formattedDate;
    }

/*
    private int getNewErrorId()
    {
        Integer ret;

        if( this.zooKeeper == null )
        {
            return -1;
        }

        if( !this.zooKeeper.configExists( this.zkErrorId ) )
        {
            if( !this.zooKeeper.createConfig( this.zkErrorId, 0) )
            {
                return -1;
            }
        }

        if( (ret = this.zooKeeper.getInt( this.zkErrorId )) == null )
        {
            return -1;
        }


        if( !this.zooKeeper.setInt(this.zkErrorId, new Integer( ret.intValue() + 1)) )
        {
            return -1;
        }

        return ret.intValue();
    }

    private int getCurrentErrorId()
    {
        Integer ret;

        if( this.zooKeeper == null )
        {
            return -1;
        }

        if( (ret = this.zooKeeper.getInt( this.zkErrorId )) == null )
        {
            return -1;
        }

        return ret.intValue();
    }

    private String getEventId()
    {
        String ret;
        int id;

        if( (id = this.getNewErrorId()) < 0 )
        {
            return null;
        }

        ret = "error-"+id; //""+id
        return ret;
    }

    private String createErrorId(int id)
    {
        return "error-"+id;
    }*/

    public String addTraceEntry(TraceEntry entry)
    {
        String ret;

        synchronized (this) {
            this.buff.add(entry);
        }

        //sync getting zk error id..
        //optionally post event to monitor service

        return "";
    }

    public String trace(int sev, int category, int error, String data)
    {
        if( this.buff == null )
        {
            //Syslog.log(Syslog.LEVEL_ERROR, "trace buffer is null");
            return null;
        }

        synchronized (this) {
            /**FIXME: come back to this
             **cap it at 1024 entries
             **/
            if (this.buff.size() > 1024) {
                this.buff.remove(0);
            }

            //Syslog.log(Syslog.LEVEL_ERROR, data);

            return this.addTraceEntry(new TraceEntry(data, this.getTimeOfDay(), sev, category, error));
        }
    }

   /* public void setZkClient(ZkClient zooKeeper)
    {
        this.zooKeeper = zooKeeper;
    }*/

    /**
     **Returns the eventId of the minty event that is generated (if any)
     **/
    public static String Trace(int sev, int category, int error, String data)
    {
        return ErrorTrace.errorTrace.trace(sev, category, error, data);
    }
}
