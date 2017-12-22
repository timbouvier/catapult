package com.tbouvier.mesos.util;

import java.time.LocalDateTime;

/**
 * Created by bouviti on 6/1/17.
 */
public class Logger {

    enum Level{
        UNKNOWN,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        TRACE,
        MAX
    }


    private Class clazz;
    private final boolean[] levels = new boolean[Level.MAX.ordinal()];

    public Logger(Class clazz)
    {
        this.clazz = clazz;

        Level[] setLevels = Level.values();
        for(int i = 0 ; i < Level.MAX.ordinal() ; i++)
        {
            if( setLevels[i].equals(Level.DEBUG) || (setLevels.equals(Level.TRACE)) )
            {
                this.levels[i] = false;
            }
            else
            {
                this.levels[i] = true;
            }
        }
    }

    private boolean enabled(Level level)
    {
        return this.levels[level.ordinal()];
    }


    private String prefix(Level level)
    {
        return level.toString()+"  ["+ LocalDateTime.now()+"] "+this.clazz.getCanonicalName()+": ";
    }

    private String clean(String msg)
    {
        return msg.replace('\n',' ');
    }

    public void debug(String msg)
    {
        if( this.enabled(Level.DEBUG)) {
            System.out.println(this.prefix(Level.DEBUG) + this.clean(msg));
        }
    }

    public void info(String msg)
    {
        if( this.enabled(Level.INFO)) {
            System.out.println(this.prefix(Level.INFO) + this.clean(msg));
        }
    }

    public void warn(String msg)
    {
        if( this.enabled(Level.WARN)) {
            System.out.println(this.prefix(Level.WARN) + this.clean(msg));
        }
    }

    public void error(String msg)
    {
        if(this.enabled(Level.ERROR)) {
            System.out.println(this.prefix(Level.ERROR) + this.clean(msg));
        }
    }

    public void trace(String msg)
    {
        if( this.enabled(Level.TRACE)) {
            System.out.println(this.prefix(Level.TRACE) + this.clean(msg));
        }
    }
}
