package com.tbouvier.mesos.app.executor;

import org.apache.mesos.Executor;
import org.apache.mesos.Protos;

/**
 * Created by bouviti on 5/5/17.
 */
public interface AppFrameworkExecutor extends Executor{

    void setExecutableApp(ExecutableApp app);
    void updateTaskState(Protos.TaskState state);
}
