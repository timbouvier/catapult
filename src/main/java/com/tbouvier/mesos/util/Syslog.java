package com.tbouvier.mesos.util;

/**
 * Created by bouviti on 4/21/17.
 */

public class Syslog implements SysLogger {

    private Class<?> clazz;
    private  Logger logger;

    public Syslog(Class<?> clazz)
    {
        this.clazz = clazz;
        this.logger = new Logger(clazz);
      //  this.logger = LoggerFactory.getLogger(this.clazz);
    }

    public static SysLogger getSysLogger(Class<?> clazz)
    {
        return new Syslog(clazz);
    }

    public void trace(String msg)
    {
        this.logger.trace( msg );
    }

    public void debug(String msg)
    {
        this.logger.debug(msg);
    }

    public void info(String msg)
    {
        this.logger.info(msg);
    }

    public void warn(String msg)
    {
        this.logger.warn(msg);
    }

    public void error(String msg)
    {
        this.logger.error(msg);
    }

    public void critical(String msg)
    {
        //tbd
    }
}
