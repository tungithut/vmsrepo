

package vn.com.mobifone.mtracker.common;

import android.app.Application;

public class AppSettings extends Application
{
    // ---------------------------------------------------
    // User Preferences
    // ---------------------------------------------------
    /*private static boolean useImperial = false;
    private static boolean newFileOnceADay;
    private static boolean logToKml;
    private static boolean logToGpx;
    private static boolean logToPlainText;
    */
	private static boolean preferCellTower;
	private static boolean showInNotificationBar;
    private static int minimumSeconds;
    private static boolean keepFix;
    private static int retryInterval;
    
    private static Float autoSendDelay = 0f;//value in hour. i.e: 5f = 5hour.
    private static boolean autoSendEnabled = false;
    
    /*private static String newFileCreation;
    private static boolean autoEmailEnabled = false;
    private static String smtpServer;
    private static String smtpPort;
    private static String smtpUsername;
    private static String smtpPassword;
    private static String smtpFrom;
    private static String autoEmailTargets;
    private static boolean smtpSsl;
    private static boolean debugToFile;*/
    private static int minimumDistance;
    private static int minimumAccuracy;
    /*private static boolean shouldSendZipFile;

    private static boolean LogToOpenGTS;
    private static boolean openGTSEnabled;
    private static boolean autoOpenGTSEnabled;
     */    
    private static String vmsServer;    
	private static String vmsServerPort;
    //private static String openGTSServerCommunicationMethod;
    private static String vmsServerPath;
    //private static String openGTSDeviceId;

    /*private static boolean autoFtpEnabled;
    private static String ftpServerName;
    private static int ftpPort;
    private static String ftpUsername;
    private static String ftpPassword;
    private static boolean ftpUseFtps;
    private static String ftpProtocol;
    private static boolean ftpImplicit;

    private static String staticFileName;
    private static boolean isStaticFile;
     */

    /**
     * @return the useImperial
     */
    /*public static boolean shouldUseImperial()
    {
        return useImperial;
    }

    *//**
     * @param useImperial the useImperial to set
     *//*
    static void setUseImperial(boolean useImperial)
    {
        AppSettings.useImperial = useImperial;
    }

    *//**
     * @return the newFileOnceADay
     *//*
    public static boolean shouldCreateNewFileOnceADay()
    {
        return newFileOnceADay;
    }

    *//**
     * @param newFileOnceADay the newFileOnceADay to set
     *//*
    static void setNewFileOnceADay(boolean newFileOnceADay)
    {
        AppSettings.newFileOnceADay = newFileOnceADay;
    }

    

    *//**
     * @return the logToKml
     *//*
    public static boolean shouldLogToKml()
    {
        return logToKml;
    }

    *//**
     * @param logToKml the logToKml to set
     *//*
    static void setLogToKml(boolean logToKml)
    {
        AppSettings.logToKml = logToKml;
    }

    *//**
     * @return the logToGpx
     *//*
    public static boolean shouldLogToGpx()
    {
        return logToGpx;
    }

    *//**
     * @param logToGpx the logToGpx to set
     *//*
    static void setLogToGpx(boolean logToGpx)
    {
        AppSettings.logToGpx = logToGpx;
    }

    public static boolean shouldLogToPlainText()
    {
        return logToPlainText;
    }

    static void setLogToPlainText(boolean logToPlainText)
    {
        AppSettings.logToPlainText = logToPlainText;
    }
*/
    
   public static boolean shouldPreferCellTower()
   {
       return preferCellTower;
   }

   public static void setPreferCellTower(boolean preferCellTower)
   {
       AppSettings.preferCellTower = preferCellTower;
   }

    
    /**
     * @return the showInNotificationBar
     */
    public static boolean shouldShowInNotificationBar()
    {
        return showInNotificationBar;
    }

    /**
     * @param showInNotificationBar the showInNotificationBar to set
     */
    static void setShowInNotificationBar(boolean showInNotificationBar)
    {
        AppSettings.showInNotificationBar = showInNotificationBar;
    }


    /**
     * @return the minimumSeconds
     */
    public static int getMinimumSeconds()
    {
        return minimumSeconds;
    }

    /**
     * @param minimumSeconds the minimumSeconds to set
     */
    static void setMinimumSeconds(int minimumSeconds)
    {
        AppSettings.minimumSeconds = minimumSeconds;
    }


    /**
     * @return the keepFix
     */
    public static boolean shouldkeepFix()
    {
        return keepFix;
    }

    /**
     * @param keepFix the keepFix to set
     */
    public static void setKeepFix(boolean keepFix)
    {
        AppSettings.keepFix = keepFix;
    }
    
          /**
     * @return the retryInterval
     */
    public static int getRetryInterval()
    {
        return retryInterval;
    }

    /**
     * @param retryInterval the retryInterval to set
     */
    public static void setRetryInterval(int retryInterval)
    {
        AppSettings.retryInterval = retryInterval;
    }


    /**
     * @return the minimumDistance
     */
    public static int getMinimumDistanceInMeters()
    {
        return minimumDistance;
    }

    /**
     * @param minimumDistance the minimumDistance to set
     */
    static void setMinimumDistanceInMeters(int minimumDistance)
    {
        AppSettings.minimumDistance = minimumDistance;
    }

         /**
     * @return the minimumAccuracy
     */
    public static int getMinimumAccuracyInMeters()
    {
        return minimumAccuracy;
    }

    /**
     * @param minimumAccuracy the minimumAccuracy to set
     */
    public static void setMinimumAccuracyInMeters(int minimumAccuracy)
    {
        AppSettings.minimumAccuracy = minimumAccuracy;
    }


    /**
     * @return the newFileCreation
     */
   /* static String getNewFileCreation()
    {
        return newFileCreation;
    }

    *//**
     * @param newFileCreation the newFileCreation to set
     *//*
    static void setNewFileCreation(String newFileCreation)
    {
        AppSettings.newFileCreation = newFileCreation;
    }


    

    *//**
     * @return the autoEmailEnabled
     *//*
    public static boolean isAutoEmailEnabled()
    {
        return autoEmailEnabled;
    }

    *//**
     * @param autoEmailEnabled the autoEmailEnabled to set
     *//*
    static void setAutoEmailEnabled(boolean autoEmailEnabled)
    {
        AppSettings.autoEmailEnabled = autoEmailEnabled;
    }


    static void setSmtpServer(String smtpServer)
    {
        AppSettings.smtpServer = smtpServer;
    }

    public static String getSmtpServer()
    {
        return smtpServer;
    }

    static void setSmtpPort(String smtpPort)
    {
        AppSettings.smtpPort = smtpPort;
    }

    public static String getSmtpPort()
    {
        return smtpPort;
    }

    static void setSmtpUsername(String smtpUsername)
    {
        AppSettings.smtpUsername = smtpUsername;
    }

    public static String getSmtpUsername()
    {
        return smtpUsername;
    }


    static void setSmtpPassword(String smtpPassword)
    {
        AppSettings.smtpPassword = smtpPassword;
    }

    public static String getSmtpPassword()
    {
        return smtpPassword;
    }

    static void setSmtpSsl(boolean smtpSsl)
    {
        AppSettings.smtpSsl = smtpSsl;
    }

    public static boolean isSmtpSsl()
    {
        return smtpSsl;
    }

    static void setAutoEmailTargets(String autoEmailTargets)
    {
        AppSettings.autoEmailTargets = autoEmailTargets;
    }

    public static String getAutoEmailTargets()
    {
        return autoEmailTargets;
    }

    public static boolean isDebugToFile()
    {
        return debugToFile;
    }

    public static void setDebugToFile(boolean debugToFile)
    {
        AppSettings.debugToFile = debugToFile;
    }


    public static boolean shouldSendZipFile()
    {
        return shouldSendZipFile;
    }

    public static void setShouldSendZipFile(boolean shouldSendZipFile)
    {
        AppSettings.shouldSendZipFile = shouldSendZipFile;
    }

    private static String getSmtpFrom()
    {
        return smtpFrom;
    }

    public static void setSmtpFrom(String smtpFrom)
    {
        AppSettings.smtpFrom = smtpFrom;
    }

    *//**
     * Returns the from value to use when sending an email
     *
     * @return
     *//*
    public static String getSenderAddress()
    {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0)
        {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }

    

    public static boolean shouldLogToOpenGTS()
    {
        return LogToOpenGTS;
    }

    public static void setLogToOpenGTS(boolean logToOpenGTS)
    {
        AppSettings.LogToOpenGTS = logToOpenGTS;
    }

    public static boolean isOpenGTSEnabled()
    {
        return openGTSEnabled;
    }

    public static void setOpenGTSEnabled(boolean openGTSEnabled)
    {
        AppSettings.openGTSEnabled = openGTSEnabled;
    }

    public static boolean isAutoOpenGTSEnabled()
    {
        return autoOpenGTSEnabled;
    }

    public static void setAutoOpenGTSEnabled(boolean autoOpenGTSEnabled)
    {
        AppSettings.autoOpenGTSEnabled = autoOpenGTSEnabled;
    }*/

    

    /*public static String getOpenGTSServerCommunicationMethod()
    {
        return openGTSServerCommunicationMethod;
    }

    public static void setOpenGTSServerCommunicationMethod(String openGTSServerCommunicationMethod)
    {
        AppSettings.openGTSServerCommunicationMethod = openGTSServerCommunicationMethod;
    }
*/
    
    /*public static String getOpenGTSDeviceId()
    {
        return openGTSDeviceId;
    }

    public static void setOpenGTSDeviceId(String openGTSDeviceId)
    {
        AppSettings.openGTSDeviceId = openGTSDeviceId;
    }


    public static String getFtpServerName()
    {
        return ftpServerName;
    }

    public static void setFtpServerName(String ftpServerName)
    {
        AppSettings.ftpServerName = ftpServerName;
    }

    public static int getFtpPort()
    {
        return ftpPort;
    }

    public static void setFtpPort(int ftpPort)
    {
        AppSettings.ftpPort = ftpPort;
    }

    public static String getFtpUsername()
    {
        return ftpUsername;
    }

    public static void setFtpUsername(String ftpUsername)
    {
        AppSettings.ftpUsername = ftpUsername;
    }

    public static String getFtpPassword()
    {
        return ftpPassword;
    }

    public static void setFtpPassword(String ftpPassword)
    {
        AppSettings.ftpPassword = ftpPassword;
    }

    public static boolean FtpUseFtps()
    {
        return ftpUseFtps;
    }

    public static void setFtpUseFtps(boolean ftpUseFtps)
    {
        AppSettings.ftpUseFtps = ftpUseFtps;
    }

    public static String getFtpProtocol()
    {
        return ftpProtocol;
    }

    public static void setFtpProtocol(String ftpProtocol)
    {
        AppSettings.ftpProtocol = ftpProtocol;
    }

    public static boolean FtpImplicit()
    {
        return ftpImplicit;
    }

    public static void setFtpImplicit(boolean ftpImplicit)
    {
        AppSettings.ftpImplicit = ftpImplicit;
    }

    public static boolean isAutoFtpEnabled()
    {
        return autoFtpEnabled;
    }

    public static void setAutoFtpEnabled(boolean autoFtpEnabled)
    {
        AppSettings.autoFtpEnabled = autoFtpEnabled;
    }

    public static String getStaticFileName()
    {
        return staticFileName;
    }

    public static void setStaticFileName(String staticFileName)
    {
        AppSettings.staticFileName = staticFileName;
    }

    public static boolean isStaticFile()
    {
        return isStaticFile;
    }

    public static void setStaticFile(boolean staticFile)
    {
        AppSettings.isStaticFile = staticFile;
    }*/

    public static String getVmsServer() {
		return vmsServer;
	}

	public static void setVmsServer(String vmsServer) {
		AppSettings.vmsServer = vmsServer;
	}

	public static String getVmsServerPort() {
		return vmsServerPort;
	}

	public static void setVmsServerPort(String vmsServerPort) {
		AppSettings.vmsServerPort = vmsServerPort;
	}

	public static String getVmsServerPath() {
		return vmsServerPath;
	}

	public static void setVmsServerPath(String vmsServerPath) {
		AppSettings.vmsServerPath = vmsServerPath;
	}
	

   public static Float getAutoSendDelay()
   {
       if (autoSendDelay >= 8f)
       {
           return 8f;
       }
       else
       {
           return autoSendDelay;
       }
   }

   
   static void setAutoSendDelay(Float autoSendDelay)
   {

       if (autoSendDelay >= 8f)
       {
           AppSettings.autoSendDelay = 8f;
       }
       else
       {
           AppSettings.autoSendDelay = autoSendDelay;
       }
   }
   
   public static boolean isAutoSendEnabled()
   {
       return autoSendEnabled;
   }

   public static void setAutoSendEnabled(boolean autoSendEnabled)
   {
       AppSettings.autoSendEnabled = autoSendEnabled;
   }

    
}
