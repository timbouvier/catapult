package com.tbouvier.mesos.executor;

import com.tbouvier.mesos.app.executor.AppExecutor;
import com.tbouvier.mesos.app.executor.ExecutableApp;

/**
 * Created by bouviti on 12/26/17.
 */
public class MyExecutableApp implements ExecutableApp {

    private int exitStatus;

    @Override
    public void restart(){
        //restart thread or process started in run
    }

    @Override
    public int getExitStatus() {
        return this.exitStatus;
    }

    @Override
    public void message(AppExecutor appExecutor, byte[] message){
        //use google protobuf to send/recv msgs from sched to exec
    }

    @Override
    public void kill(){
        //notify a thread or process to terminate
    }

    @Override
    public void run(AppExecutor executor){

        this.exitStatus = 1;

        try{
            System.out.println("Hello World!");
            this.exitStatus = 0;
        }catch (Exception e){
            System.out.println("caught exceptione: "+e);
        }
    }
}
