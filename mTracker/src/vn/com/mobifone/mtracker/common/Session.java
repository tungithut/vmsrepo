
package vn.com.mobifone.mtracker.common;


import android.app.Application;
import android.location.Location;

public class Session extends Application
{
	// For debugging purpose:
	private static boolean debugEnabled = true;
	
    public static boolean isDebugEnabled() {
		return debugEnabled;
	}

	public static void setDebugEnabled(boolean debugEnabled) {
		Session.debugEnabled = debugEnabled;
	}

	// ---------------------------------------------------
    // Session values - updated as the app runs
    // ---------------------------------------------------
    private static boolean towerEnabled;
    private static boolean gpsEnabled;
    private static boolean isStarted;
    private static boolean isUsingGps;
    private static String currentFileName;
    private static int satellites;
    private static boolean notificationVisible;
    private static float autoSendDelay;
    private static long latestTimeStamp;
    private static boolean addNewTrackSegment = true;
    private static Location currentLocationInfo;
    private static Location previousLocationInfo;
    private static double totalTravelled;
    private static int numLegs;
    private static boolean isBound;
    private static boolean readyToBeAutoSent = false;
    private static boolean allowDescription = true;
    //private static boolean isSinglePointMode = false;
    private static int retryTimeout=0;
    private static String imei;
    
    //new for mTracker:
    private static boolean isCheckin = false;
    private static String locStatus = "";//default empty string ""
    private static boolean isStartStop = false;
    private static boolean checkinWithoutRoute = false;//'true' if user click on 'checkin' without any 'start' action. Means that this 'checkin' point not belong to any route.
    private static boolean isLaunched = false;//default: not launched yet, for animation on splash screen determination
    private static String appInfo = "Version %s 2013.\n Developed by VMS IT Department";
    
    /*public static boolean isSinglePointMode()
    {
        return isSinglePointMode;
    }

    public static void setSinglePointMode(boolean singlePointMode)
    {
        isSinglePointMode = singlePointMode;
    }*/

    // ---------------------------------------------------

    public static boolean isLaunched() {
		return isLaunched;
	}

	public static String getAppInfo() {
		return appInfo;
	}

	public static void setAppInfo(String appInfo) {
		Session.appInfo = appInfo;
	}

	public static void setLaunched(boolean isLaunched) {
		Session.isLaunched = isLaunched;
	}

	public static boolean isCheckinWithoutRoute() {
		return checkinWithoutRoute;
	}

	public static void setCheckinWithoutRoute(boolean checkinWithoutRoute) {
		Session.checkinWithoutRoute = checkinWithoutRoute;
	}

	public static boolean isStartStop() {
		return isStartStop;
	}

	public static void setStartStop(boolean isStartStop) {
		Session.isStartStop = isStartStop;
	}

	public static String getLocStatus() {
		return locStatus;
	}

	public static void setLocStatus(String locStatus) {
		Session.locStatus = locStatus;
	}

	public static boolean isCheckin() {
		return isCheckin;
	}

	public static void setCheckin(boolean isCheckin) {
		Session.isCheckin = isCheckin;
	}

	public static String getImei() {
		return imei;
	}

	public static void setImei(String imei) {
		Session.imei = imei;
	}

	/**
     * @return whether GPS (tower) is enabled
     */
    public static boolean isTowerEnabled()
    {
        return towerEnabled;
    }

    /**
     * @param towerEnabled set whether GPS (tower) is enabled
     */
    public static void setTowerEnabled(boolean towerEnabled)
    {
        Session.towerEnabled = towerEnabled;
    }

    /**
     * @return whether GPS (satellite) is enabled
     */
    public static boolean isGpsEnabled()
    {
        return gpsEnabled;
    }

    /**
     * @param gpsEnabled set whether GPS (satellite) is enabled
     */
    public static void setGpsEnabled(boolean gpsEnabled)
    {
        Session.gpsEnabled = gpsEnabled;
    }

    /**
     * @return whether logging has started
     */
    public static boolean isStarted()
    {
        return isStarted;
    }

    /**
     * @param isStarted set whether logging has started
     */
    public static void setStarted(boolean isStarted)
    {
        Session.isStarted = isStarted;

    }

    /**
     * @return the isUsingGps
     */
    public static boolean isUsingGps()
    {
        return isUsingGps;
    }

    /**
     * @param isUsingGps the isUsingGps to set
     */
    public static void setUsingGps(boolean isUsingGps)
    {
        Session.isUsingGps = isUsingGps;
    }

    /**
     * @return the currentFileName (without extension)
     */
    public static String getCurrentFileName()
    {
        return currentFileName;
    }

    /**
     * @param currentFileName the currentFileName to set
     */
    public static void setCurrentFileName(String currentFileName)
    {
        Utilities.LogInfo("Setting file name - " + currentFileName);
        Session.currentFileName = currentFileName;
    }

    /**
     * @return the number of satellites visible
     */
    public static int getSatelliteCount()
    {
        return satellites;
    }

    /**
     * @param satellites sets the number of visible satellites
     */
    public static void setSatelliteCount(int satellites)
    {
        Session.satellites = satellites;
    }

           /**
     * @return the retryTimeout
     */
    public static int getRetryTimeout()
    {
        return retryTimeout;
    }

    /**
     * @param retryTimeout sets the retryTimeout
     *
     * */
    public static void setRetryTimeout(int retryTimeout)
    {
        Session.retryTimeout = retryTimeout;
    }

    /**
     * @return the notificationVisible
     */
    public static boolean isNotificationVisible()
    {
        return notificationVisible;
    }

    /**
     * @param notificationVisible the notificationVisible to set
     */
    public static void setNotificationVisible(boolean notificationVisible)
    {
        Session.notificationVisible = notificationVisible;
    }

    /**
     * @return the currentLatitude
     */
    public static double getCurrentLatitude()
    {
        if (getCurrentLocationInfo() != null)
        {
            return getCurrentLocationInfo().getLatitude();
        }
        else
        {
            return 0;
        }
    }

    public static double getPreviousLatitude()
    {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLatitude() : 0;
    }

    public static double getPreviousLongitude()
    {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLongitude() : 0;
    }

    public static double getTotalTravelled()
    {
        return totalTravelled;
    }

    public static int getNumLegs()
    {
        return numLegs;
    }

    public static void setTotalTravelled(double totalTravelled)
    {
        if (totalTravelled == 0)
        {
            Session.numLegs = 0;
        }
        else
        {
            Session.numLegs++;
        }
        Session.totalTravelled = totalTravelled;
    }

    public static Location getPreviousLocationInfo()
    {
        return previousLocationInfo;
    }

    public static void setPreviousLocationInfo(Location previousLocationInfo)
    {
        Session.previousLocationInfo = previousLocationInfo;
    }


    /**
     * Determines whether a valid location is available
     */
    public static boolean hasValidLocation()
    {
        return (getCurrentLocationInfo() != null && getCurrentLatitude() != 0 && getCurrentLongitude() != 0);
    }

    /**
     * @return the currentLongitude
     */
    public static double getCurrentLongitude()
    {
        if (getCurrentLocationInfo() != null)
        {
            return getCurrentLocationInfo().getLongitude();
        }
        else
        {
            return 0;
        }
    }

    /**
     * @return the latestTimeStamp (for location info)
     */
    public static long getLatestTimeStamp()
    {
        return latestTimeStamp;
    }

    /**
     * @param latestTimeStamp the latestTimeStamp (for location info) to set
     */
    public static void setLatestTimeStamp(long latestTimeStamp)
    {
        Session.latestTimeStamp = latestTimeStamp;
    }

    /**
     * @return whether to create a new track segment
     */
    public static boolean shouldAddNewTrackSegment()
    {
        return addNewTrackSegment;
    }

    /**
     * @param addNewTrackSegment set whether to create a new track segment
     */
    public static void setAddNewTrackSegment(boolean addNewTrackSegment)
    {
        Session.addNewTrackSegment = addNewTrackSegment;
    }

    /**
     * @param autoSendDelay the autoSendDelay to set
     */
    public static void setAutoSendDelay(float autoSendDelay)
    {
        Session.autoSendDelay = autoSendDelay;
    }

    /**
     * @return the autoSendDelay to use for the timer
     */
    public static float getAutoSendDelay()
    {
        return autoSendDelay;
    }

    /**
     * @param currentLocationInfo the latest Location class
     */
    public static void setCurrentLocationInfo(Location currentLocationInfo)
    {
        Session.currentLocationInfo = currentLocationInfo;
    }

    /**
     * @return the Location class containing latest lat-long information
     */
    public static Location getCurrentLocationInfo()
    {
        return currentLocationInfo;
    }

    /**
     * @param isBound set whether the activity is bound to the VMSLoggingService
     */
    public static void setBoundToService(boolean isBound)
    {
        Session.isBound = isBound;
    }

    /**
     * @return whether the activity is bound to the VMSLoggingService
     */
    public static boolean isBoundToService()
    {
        return isBound;
    }

    /**
     * Sets whether an email is ready to be sent
     *
     * @param readyToBeAutoSent
     */
    public static void setReadyToBeAutoSent(boolean readyToBeAutoSent)
    {
        Session.readyToBeAutoSent = readyToBeAutoSent;
    }

    /**
     * Gets whether an email is waiting to be sent
     *
     * @return
     */
    public static boolean isReadyToBeAutoSent()
    {
        return readyToBeAutoSent;
    }

    public static boolean shoulAllowDescription()
    {
        return allowDescription;
    }

    public static void setAllowDescription(boolean allowDescription)
    {
        Session.allowDescription = allowDescription;
    }

}
