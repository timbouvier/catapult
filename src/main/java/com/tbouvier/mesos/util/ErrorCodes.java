package com.tbouvier.mesos.util;

public class ErrorCodes {

    /**
     **Categories
     **/
    public static final int SCHED                         = 1;
    public static final int ZOOKEEPER                     = 2;
    public static final int APPMON                        = 3;
    public static final int MARIADB                       = 4;
    public static final int TASK                          = 5;
    public static final int APP_MON                       = 6;
    public static final int SCHED_APP                     = 7;
    public static final int MONGODB                       = 8;

    /**
     **Error code
     **/
    public static final int INVALID_ARG                       = 1;
    public static final int INFORMATION                       = 2;
    public static final int DATABASE_FAILURE                  = 3;
    public static final int LOCK_FAILURE                      = 4;
    public static final int THREAD_FAILURE                    = 5;
    public static final int QUEUE_FAILURE                     = 6;
    public static final int TASK_FAILURE                      = 7;
    public static final int BUILD_FAILURE                     = 8;
    public static final int UNEXPECTED_STATE                  = 9;
    public static final int NODE_FAILURE                      = 10;
    public static final int USER_ERROR                        = 11;
    public static final int APP_ERROR                         = 12;

    /**
     **Sevs
     **/
    public static final int SEV_INFO                    = 1;
    public static final int SEV_LOW                     = 2;
    public static final int SEV_NOTICE                  = 3;
    public static final int SEV_MEDIUM                  = 4;
    public static final int SEV_HIGH                    = 5;
    public static final int SEV_CRITICAL                = 6;

    public static String getSevString(int sev)
    {
        switch( sev )
        {
            case ErrorCodes.SEV_INFO:                 return "INFO";
            case ErrorCodes.SEV_LOW:                  return "LOW";
            case ErrorCodes.SEV_NOTICE:               return "NOTICE";
            case ErrorCodes.SEV_MEDIUM:               return "MEDIUM";
            case ErrorCodes.SEV_HIGH:                 return "HIGH";
            case ErrorCodes.SEV_CRITICAL:             return "CRITICAL";
            default:                                  return "???";
        }
    }

    public static String getMintySevString(int sev)
    {
        switch( sev )
        {
            case ErrorCodes.SEV_INFO:                 return "Informational";
            case ErrorCodes.SEV_LOW:                  return "Warning";
            case ErrorCodes.SEV_NOTICE:               return "Minor";
            case ErrorCodes.SEV_MEDIUM:               return "Minor";
            case ErrorCodes.SEV_HIGH:                 return "Minor";
            case ErrorCodes.SEV_CRITICAL:             return "CRITICAL";
            default:                                  return "???";
        }
    }

    public static String getCategoryString(int cat)
    {
        switch( cat )
        {
            case ErrorCodes.SCHED:                    return "scheduler";
            case ErrorCodes.ZOOKEEPER:                return "zookeeper";
            case ErrorCodes.APPMON:                   return "appmon";
            case ErrorCodes.MARIADB:                  return "mariadb";
            case ErrorCodes.TASK:                     return "task";
            case ErrorCodes.APP_MON:                  return "appmon";
            case ErrorCodes.SCHED_APP:                return "schedulerApp";
            case ErrorCodes.MONGODB:                  return "mongodb";
            default:                                  return "???";
        }
    }

    public static String getErrorString(int error)
    {
        switch( error )
        {
            case ErrorCodes.INVALID_ARG:            return "invalid argument";
            case ErrorCodes.INFORMATION:            return "information";
            case ErrorCodes.DATABASE_FAILURE:       return "database error";
            case ErrorCodes.THREAD_FAILURE:         return "thread failure";
            case ErrorCodes.TASK_FAILURE:           return "task failure";
            case ErrorCodes.QUEUE_FAILURE:          return "queue failure";
            case ErrorCodes.UNEXPECTED_STATE:       return "unexpected state";
            case ErrorCodes.NODE_FAILURE:           return "node failure";
            case ErrorCodes.USER_ERROR:             return "user error";
            case ErrorCodes.APP_ERROR:              return "application error";
            default:                                return "???";
        }
    }

}
