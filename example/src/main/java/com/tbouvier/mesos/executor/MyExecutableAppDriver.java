package com.tbouvier.mesos.executor;

import com.tbouvier.mesos.app.executor.MesosApplicationFrameworkExecutor;

/**
 * Created by bouviti on 12/27/17.
 */
public class MyExecutableAppDriver {
    public static void main(String[] args){
        new MesosApplicationFrameworkExecutor(new MyExecutableApp()).run();
    }
}
