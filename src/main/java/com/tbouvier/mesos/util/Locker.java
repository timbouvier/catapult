package com.tbouvier.mesos.util;

/**
 * Created by bouviti on 4/24/17.
 */
public interface Locker {

    void forceUnlock();
    boolean getLockExclusiveTm(long tmUsecs);
    public void releaseExclusiveLock();

}
