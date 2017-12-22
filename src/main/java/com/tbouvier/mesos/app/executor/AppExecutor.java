package com.tbouvier.mesos.app.executor;

/**
 * Created by bouviti on 5/5/17.
 */
public interface AppExecutor {

    void message(byte[] message);
    void shutdown();
}
