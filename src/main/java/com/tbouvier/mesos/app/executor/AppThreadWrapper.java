package com.tbouvier.mesos.app.executor;

import com.tbouvier.mesos.util.SysLogger;
import com.tbouvier.mesos.util.Syslog;
import org.apache.mesos.Protos;

import static org.apache.mesos.Protos.TaskState.TASK_FAILED;
import static org.apache.mesos.Protos.TaskState.TASK_FINISHED;

/**
 * Created by bouviti on 5/5/17.
 */
public class AppThreadWrapper implements Runnable {

    private SysLogger LOGGER = Syslog.getSysLogger(AppThreadWrapper.class);

    private ExecutableApp app;
    private AppFrameworkExecutor parent;
    private AppExecutor appExecutor;

    public AppThreadWrapper(ExecutableApp app, AppFrameworkExecutor parent, AppExecutor appExecutor)
    {
        this.app = app;
        this.parent = parent;
        this.appExecutor = appExecutor;
    }

    @Override
    public void run()
    {
        try {
            this.app.run(this.appExecutor);

            if (this.app.getExitStatus() > 0) {
                this.parent.updateTaskState(TASK_FAILED);
            } else {
                this.parent.updateTaskState(TASK_FINISHED);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.parent.updateTaskState(TASK_FAILED);
        }

        //shutdown the executor?

        LOGGER.warn("AppThread terminating... Calling executor shutdown");
        this.appExecutor.shutdown();
    }
}
