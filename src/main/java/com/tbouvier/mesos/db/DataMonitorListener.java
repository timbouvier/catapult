package com.tbouvier.mesos.db;

interface DataMonitorListener {

    public void exists(byte[] data);

    public  void closing(int rc);

    public String getElectionNode();
    public void evaluateLeader();
    public void setSyncConnected();

}
