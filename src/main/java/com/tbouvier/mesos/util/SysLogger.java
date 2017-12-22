package com.tbouvier.mesos.util;

/**
 * Created by bouviti on 6/1/17.
 */
public interface SysLogger {

    public void info(String msg);
    public void debug(String msg);
    public void warn(String msg);
    public void error(String msg);
    public void critical(String msg);
}
