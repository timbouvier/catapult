package com.tbouvier.mesos.util;

public class TraceEntry {

    private String data;
    private String tstamp;
    private int sev;
    private int category;
    private int error;

    TraceEntry(String data, String tstamp, int sev, int category, int error)
    {
        this.data = data;
        this.tstamp = tstamp;
        this.sev = sev;
        this.error = error;
        this.category = category;
    }

    public String getData()
    {
        return this.data;
    }

    public String getTsamp()
    {
        return this.tstamp;
    }

    public int getSev()
    {
        return this.sev;
    }

    public int getError()
    {
        return this.error;
    }

    public int getCategory()
    {
        return this.category;
    }
}
