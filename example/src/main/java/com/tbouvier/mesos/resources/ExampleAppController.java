package com.tbouvier.mesos.resources;

import com.tbouvier.mesos.api.Application;
import com.tbouvier.mesos.scheduler.MyAppDeployer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by bouviti on 12/26/17.
 */
@Path("/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExampleAppController {

    private final MyAppDeployer appDeployer;

    public ExampleAppController(MyAppDeployer appDeployer){
        this.appDeployer = appDeployer;
    }

    @POST
    public Application createApp(@NotNull @Valid Application application)
    {
        this.appDeployer.create(application);
        return application;
    }
}
