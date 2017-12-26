package com.tbouvier.mesos.api;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by bouviti on 12/26/17.
 */
public class Application {

    @NotEmpty
    private int numNodes;

    public Application(){

    }

    public int getNumNodes(){
        return this.numNodes;
    }

    public void setNumNodes(int numNodes){
        this.numNodes = numNodes;
    }
}
