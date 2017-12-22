package com.tbouvier.mesos.util;

import com.sun.org.apache.bcel.internal.classfile.Unknown;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by bouviti on 4/26/17.
 */
public class NetUtils {

    public static String getLocalHostIp() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
