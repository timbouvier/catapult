package com.tbouvier.mesos.scheduler;

import com.tbouvier.mesos.app.scheduler.AppDriver;
import com.tbouvier.mesos.app.scheduler.SchedulerNode;

/**
 * Created by bouviti on 12/26/17.
 */
public class MyNode extends SchedulerNode {

    public MyNode(Protos.SchedulerNodeInfo nodeInfo){
        super(nodeInfo);
    }

    @Override
    public void failed(AppDriver appDriver){
      /*default implementation will relaunch node*/
      System.out.println("Node failed: "+this.getNodeName());
    }

    @Override
    public void running(AppDriver appDriver){
     /*default implementation does nothing*/
     System.out.println("Node is running: "+this.getNodeName());
    }

    @Override
    public void finished(AppDriver appDriver){
    /*default implementation does nothing*/
    System.out.println("Node finished: "+this.getNodeName());
    }

    @Override
    public void killed(AppDriver appDriver){
     /*default implementation relaunches node*/
     System.out.println("Node was killed: "+this.getNodeName());
    }

    @Override
    public void message(AppDriver appDriver, byte[] data){
     /*default implementation drops message*/
     System.out.println("got message for node: "+this.getNodeName());
    }
}
