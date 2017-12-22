package com.tbouvier.mesos.util;

import com.google.inject.Inject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UtilLock implements Locker {
    
    private final SysLogger LOGGER = Syslog.getSysLogger(UtilLock.class);

    private ReentrantReadWriteLock lock;
    private Lock writeLock;
    private Lock readLock;
    private String name;
    private boolean logging;

    @Inject
    public UtilLock()
    {
        this.lock = new ReentrantReadWriteLock();
        this.writeLock = this.lock.writeLock();
        this.readLock = this.lock.readLock();
        this.name = "default";
        this.logging = true;
    }

    public void disableLogging()
    {
        this.logging = false;
    }

    public void enableLogging()
    {
        this.logging = true;
    }

    public void forceUnlock()
    {
        this.lock = new ReentrantReadWriteLock();
        this.writeLock = this.lock.writeLock();
        this.readLock = this.lock.readLock();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean getLockExclusiveTm(long tmUsecs)
    {
        long t0, t1;
        boolean aquired;

        t0 = 0;
        t1 = 0;
        aquired = false;

        try{
            t0 = System.nanoTime();
            aquired = this.writeLock.tryLock(tmUsecs, TimeUnit.MICROSECONDS );
            t1 = System.nanoTime();
        }catch(Exception e )
        {
            if( this.logging )
            {
                LOGGER.error(
                        ""+this.name+": caught exception while pending on writeLock "+e);
            }
        }

        if( aquired )
        {
            //this.setLockOwner();

            if( this.logging )
            {
                LOGGER.debug(
                        ""+this.name+": acquired writeLock in "+(t1 - t0)+" nanos");
            }
        }

        return aquired;
    }

    public void releaseExclusiveLock()
    {
        try {
            this.writeLock.unlock();
        }catch(IllegalMonitorStateException e)
        {
            if( this.logging )
            {
                LOGGER.error(
                        ""+this.name+": tried to unlock but thread does not hold lock!");
            }
        }

        if( this.logging )
        {
            LOGGER.debug( ""+this.name+": unlocking");
        }
    }

}
