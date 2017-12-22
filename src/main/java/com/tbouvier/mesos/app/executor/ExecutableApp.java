package com.tbouvier.mesos.app.executor;

/**
 * Created by bouviti on 5/5/17.
 */
public interface ExecutableApp extends Killable{

    void restart();
    int getExitStatus();
    void message(AppExecutor appExecutor, byte[] message);
    void run(AppExecutor executor);
}
