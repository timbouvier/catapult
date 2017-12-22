package com.tbouvier.mesos.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by bouviti on 5/6/17.
 */
public class RandomUtils {

    public static String getNew32ByteString()
    {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }
}
