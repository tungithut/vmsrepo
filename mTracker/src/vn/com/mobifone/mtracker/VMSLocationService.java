package vn.com.mobifone.mtracker;





import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Random;

import vn.com.mobifone.mtracker.R;
import vn.com.mobifone.mtracker.common.AppSettings;
import vn.com.mobifone.mtracker.common.Session;
import vn.com.mobifone.mtracker.common.Utilities;
import vn.com.mobifone.mtracker.db.DatabaseHandler;
import vn.com.mobifone.mtracker.loggers.VMSLogger;
import vn.com.mobifone.mtracker.senders.AlarmReceiver;




import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class VMSLocationService extends Service {

	public LocationManager gpsLocationManager ;
	private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    private LocationManager towerLocationManager;
    
    private boolean isGpsEnabled;
    private boolean isTowerEnabled;
    
    private static long minTime = 1000;	//1000 miliseconds; should not change.
    private static float minDist = 0;	//0 meters
    
    private static NotificationManager gpsNotifyManager;
    private static int NOTIFICATION_ID = 21000508;
    private final IBinder mBinder = new ServiceBinder() ;
    private static VMSMainActivity mainServiceClient ;
    
    private Intent alarmIntent;
    AlarmManager nextPointAlarmManager;
    
    @Override
    public void onCreate()
    {
        Utilities.LogDebug("VMSLocationService.onCreate");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    	Toast.makeText(getApplicationContext(), "VMSservice.onCreate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Utilities.LogDebug("VMSLocationService.onStart");
    	Toast.makeText(getApplicationContext(), "VMSservice.onStart", Toast.LENGTH_SHORT).show();
        //vmsHandleIntent(intent);
    	HandleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Utilities.LogDebug("VMSLocationService.onStartCommand");    	
    	Toast.makeText(getApplicationContext(), "VMSservice.onStartCommand", Toast.LENGTH_SHORT).show();
        //vmsHandleIntent(intent);
    	HandleIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        Utilities.LogWarning("VMSLocationService is being destroyed by Android OS.");
    	Toast.makeText(getApplicationContext(), "VMSservice.onDestroy", Toast.LENGTH_SHORT).show();
        mainServiceClient = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        Utilities.LogWarning("Android is low on memory.");
    	Toast.makeText(getApplicationContext(), "VMSservice.onLowMemeory", Toast.LENGTH_SHORT).show();
        super.onLowMemory();
    }
    
    /**
     * Sets the activity form for this service. The activity form needs to
     * implement IGpsLoggerServiceClient.
     *
     * @param mainForm The calling client
     */
    protected static void SetServiceClient(VMSMainActivity mainActivity)
    {
        mainServiceClient = mainActivity;
    }
    
    /**
     * Handle Intents that was passed in the call to this service.
     * @param intent
     */
    private void HandleIntent(Intent intent)
    {

        Utilities.LogDebug("VMSLocationService.handleIntent");
        Toast.makeText(getApplicationContext(), "service.handleIntent", Toast.LENGTH_SHORT).show();
        
        GetPreferences();

        Utilities.LogDebug("Null intent? " + String.valueOf(intent == null));

        if (intent != null)
        {
            Bundle bundle = intent.getExtras();

            if (bundle != null)
            {
                //boolean stopRightNow = bundle.getBoolean("immediatestop");//this is shortcut stop event.
                boolean sendEmailNow = bundle.getBoolean("emailAlarm");//actually it is autosend event.
            	boolean startRightNow = bundle.getBoolean("immediate");//this is shortcut start event.
            	boolean getNextPoint = bundle.getBoolean("getnextpoint");

                Utilities.LogDebug("startRightNow - " + String.valueOf(startRightNow));
                Utilities.LogDebug("emailAlarm - " + String.valueOf(sendEmailNow));

                if (startRightNow)
                {
                    Utilities.LogInfo("Auto starting logging");
                    StartLogging();
                }

                /*if (stopRightNow)
                {
                    Utilities.LogInfo("Auto stop logging");
                    StopLogging();
                }
                */
                if (sendEmailNow)
                {
                    Utilities.LogDebug("setReadyToBeAutoSent = true");
                    Session.setReadyToBeAutoSent(true);
                    AutoSendLogFile();
                }
                 
                if (getNextPoint && Session.isStarted())
                {
                    Utilities.LogDebug("HandleIntent - getNextPoint");
                    StartGpsManager(false);
                }
            }
        }
        else
        {
            // A null intent is passed in if the service has been killed and
            // restarted.
            Utilities.LogDebug("Service restarted with null intent. Start logging.");
            StartLogging();
        }
    }
    
    /**
     * Gets preferences chosen by the user and populates the AppSettings object.
     * Also sets up email timers if required.
     */
    private void GetPreferences()
    {
        Utilities.LogDebug("VMSLocationService.GetPreferences");
        
        Utilities.PopulateAppSettings(getApplicationContext());

        //Utilities.LogDebug("Session.getAutoSendDelay: " + Session.getAutoSendDelay());
        //Utilities.LogDebug("AppSettings.getAutoSendDelay: " + AppSettings.getAutoSendDelay());

        /*if (Session.getAutoSendDelay() != AppSettings.getAutoSendDelay())
        {
            Utilities.LogDebug("Old autoSendDelay - " + String.valueOf(Session.getAutoSendDelay())
                    + "; New -" + String.valueOf(AppSettings.getAutoSendDelay()));
            Session.setAutoSendDelay(AppSettings.getAutoSendDelay());
            SetupAutoSendTimers();
        }*/

    }
    
	private void vmsHandleIntent(Intent intent) {
		
		Utilities.LogDebug("VMSLocationService.handleIntent");
		Toast.makeText(getApplicationContext(), "VMSservice.vmsHandleIntent", Toast.LENGTH_SHORT).show();
		/*	We use Service class instead of IntentService to get more controller 
		 * among client-binding classes; So this class does not have to 'onHandleIntent()'. 
		 * Instead, we use vmsHandleIntent for source code organizing structure only. 
		 *  		
		 */ 
		 //Acquire a reference to the system Location Manager
	    /*LocationManager locationManager = 
	    		(LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    
	    // Register the listener with the Location Manager to receive location updates
	    // first call GPS provider:
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, 
	    		new GeneralLocationListener(this));
	    // Second call Network provider:
	    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDist, 
	    		new GeneralLocationListener(this));
	    

	    Location lastKnownLocation = locationManager.
	    		getLastKnownLocation(LocationManager.GPS_PROVIDER);
	    
	    // In case of GPS known location not available:
	    if (lastKnownLocation==null){
	    		lastKnownLocation = locationManager.
	    				getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	    }
	    Utilities.LogDebug("VMSLocationService.handleIntent-2");*/
	    
        //GetPreferences();

        /*Utilities.LogDebug("Null intent? " + String.valueOf(intent == null));

        if (intent != null)
        {
        	
            //Never come here.
        	Bundle bundle = intent.getExtras();

            if (bundle != null)
            {
                boolean stopRightNow = bundle.getBoolean("immediatestop");
                boolean startRightNow = bundle.getBoolean("immediate");
                boolean sendEmailNow = bundle.getBoolean("emailAlarm");
                boolean getNextPoint = bundle.getBoolean("getnextpoint");

                Utilities.LogDebug("startRightNow - " + String.valueOf(startRightNow));

                Utilities.LogDebug("emailAlarm - " + String.valueOf(sendEmailNow));

                if (startRightNow)
                {
                    Utilities.LogInfo("Auto starting logging");

                    StartLogging();
                }

                if (stopRightNow)
                {
                    Utilities.LogInfo("Auto stop logging");
                    StopLogging();
                }

                if (sendEmailNow)
                {

                    Utilities.LogDebug("setReadyToBeAutoSent = true");

                    Session.setReadyToBeAutoSent(true);
                    //AutoSendLogFile();
                }

                if (getNextPoint && Session.isStarted())
                {
                    Utilities.LogDebug("HandleIntent - getNextPoint");
                    StartGpsManager();
                }

            }
        }
        else
        {
            // A null intent is passed in if the service has been killed and
            // restarted.
            Utilities.LogDebug("Service restarted with null intent. Start logging.");
            StartLogging();

        }*/
	}
	
	 /**
     * Starts the location manager. There are two location managers - GPS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GPS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    private void StartGpsManager_old(){
    	
    	Toast.makeText(getApplicationContext(), 
    			"VMSservice.StartGpsManager", Toast.LENGTH_SHORT).show();
    	//Acquire a reference to the system Location Manager
	    LocationManager locationManager = 
	    		(LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    
    	// Check whether GPS/Tower positioning function enabled in the Phone's Setting:
	    isGpsEnabled = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).
	    				isProviderEnabled(LocationManager.GPS_PROVIDER);
	    
	    isTowerEnabled = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).
				isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	    
	    // Prepare the two location lister for using later.
	    if (gpsLocationListener == null){
            gpsLocationListener = new GeneralLocationListener(this);
        }

        if (towerLocationListener == null){
            towerLocationListener = new GeneralLocationListener(this);
        }

	    
	    // Get the two location manager for using later.
	    gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
	    if (isGpsEnabled)
        {
            Log.d("mTracker","StartGpsManager:Requesting GPS location updates");
	    	// gps satellite based
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    minTime, minDist, gpsLocationListener);

            gpsLocationManager.addGpsStatusListener(gpsLocationListener);//we are using GPS now.
        }
        else if (isTowerEnabled)
        {
            Log.d("mTracker","StartGpsManager:Requesting tower location updates");
        	// Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    minTime, minDist, towerLocationListener);
        } else  {
        	Log.d("mTracker","StartGpsManager:No provider available");
        }
		return;	
    }
    
    private void StartGpsManager(boolean isCallFromStartSTop)
    {
        Utilities.LogDebug("VMSLocationService.StartGpsManager");
        Toast.makeText(getApplicationContext(), "service.startGPSManager", Toast.LENGTH_SHORT).show();

        GetPreferences();

        if (gpsLocationListener == null) {
            gpsLocationListener = new GeneralLocationListener(this);
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralLocationListener(this);
        }

        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        CheckTowerAndGpsStatus();

        if (Session.isGpsEnabled() && !AppSettings.shouldPreferCellTower())
        {
            Utilities.LogInfo("Requesting GPS location updates");
            // gps satellite based
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    minTime, minDist,  gpsLocationListener);
            gpsLocationManager.addGpsStatusListener(gpsLocationListener);
            Session.setUsingGps(true);
            
        }  else if (Session.isTowerEnabled())  {
            Utilities.LogInfo("Requesting tower location updates");
            Session.setUsingGps(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    minTime, minDist, towerLocationListener);

        }   else  {
            Utilities.LogInfo("No provider available");
            Session.setUsingGps(false);
            //SetStatus(R.string.gpsprovider_unavailable);
            //SetFatalMessage(R.string.gpsprovider_unavailable);
            StopLogging();
            return;
        }
        
        //tnt.add
        // when call from StartStop procedures, need to fast locationing the position in order to saving to the map.
        // first time using lastknown for a fast locationing
        if (false && isCallFromStartSTop){
	        LocationManager locationManager = 
		    		(LocationManager) getSystemService(Context.LOCATION_SERVICE);
	        
	        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        
	        if (lastKnownLocation==null){
	        	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        }
	        
	        // we fire the location updtae event manually for the first time.
	        if (lastKnownLocation != null){        	
	        	this.OnLocationChanged(lastKnownLocation);
	        }
        }
        //tnt.end

        //SetStatus(R.string.started);
    }
    
    
    
    /**
     * Stops the location managers. 
     * We have to remove the updates activities.
     */
    private void StopGpsManager()
    {

        Log.d("mTracker","VMSLocationService.StopGpsManager()");
        
        Toast.makeText(getApplicationContext(), 
    			"VMSservice.StopGpsManager", Toast.LENGTH_SHORT).show();

        if (towerLocationListener != null)
        {
            Log.d("mTracker","Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gpsLocationListener != null)
        {
            Log.d("mTracker","Removing gpsLocationManager updates");
            gpsLocationManager.removeUpdates(gpsLocationListener);
            gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
        }

        //SetStatus(getString(R.string.stopped));
    }
    
    /**
     * Stops location manager, then starts it.
     */
    void RestartGpsManagers()
    {
        Log.d("mTracker","VMSLocationService.RestartGpsManagers()");
        Toast.makeText(getApplicationContext(), 
    			"VMSservice.RestartGpsManagers", Toast.LENGTH_SHORT).show();
        StopGpsManager();
        StartGpsManager(false);
    }
	
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}

	/**
	 * We come here when location lister (GeneralLocationListener) got a new
	 * location fix.
	 * 
	 * @param loc
	 */
	public void OnLocationChanged(Location loc) {
		int retryTimeout = Session.getRetryTimeout();
		int MAX_RETRY_TIMES = 2;//number of retry, max.
		int USER_DEF_MIN_FIX_DURATION = 1;//5 min: more than this value is accpetable.
		int USER_DEF_MIN_ACCURACY_IN_MET = 100;//100 meters accuracy is acceptable (more is less accureacy)
		int USER_DEF_MIN_DISTANCE = 50;//over 30 meters is acceptible.
		
		USER_DEF_MIN_FIX_DURATION = AppSettings.getMinimumSeconds();
		USER_DEF_MIN_ACCURACY_IN_MET = AppSettings.getMinimumAccuracyInMeters();
		USER_DEF_MIN_DISTANCE = AppSettings.getMinimumDistanceInMeters();
				
		/*Toast.makeText(getApplicationContext(), "Service:onLocationChanged()", 
				Toast.LENGTH_SHORT).show();*/
		
		Toast.makeText(getApplicationContext(), "Service:onLocationChanged:(lat,lon)=" 
				+ String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getLongitude()), 
				Toast.LENGTH_SHORT).show();
		
		 if (!Session.isStarted()){
	            Utilities.LogDebug("OnLocationChanged called, but Session.isStarted is false");
	            StopLogging();
	            return;
	    }
		
		Utilities.LogDebug("VMSLocationService.OnLocationChanged");

		long currentTimeStamp = System.currentTimeMillis();

		// Wait some time even on 0 frequency so that the UI doesn't lock up
		// No log when previous log event just happend under 1000 miliseconds from now.
		// just return.
		if ((currentTimeStamp - Session.getLatestTimeStamp()) < 1000) {
			return;
		}

		// Don't do anything until the user-defined time has elapsed
		if ((currentTimeStamp - Session.getLatestTimeStamp()) < (USER_DEF_MIN_FIX_DURATION * 1000)) {
			return;
		}

		// Don't do anything until the user-defined location accuracy (in meters) is reached
		if (USER_DEF_MIN_ACCURACY_IN_MET > 0) {
			if (USER_DEF_MIN_ACCURACY_IN_MET < Math.abs(loc
					.getAccuracy())) {
				if (retryTimeout < MAX_RETRY_TIMES) //reduce rety times to 10 only. 
				{
					//Only accuracy of loc.getAccuracy reached
					Session.setRetryTimeout(retryTimeout + 1);
					//StopGpsManager();
					StopManagerAndResetAlarm(AppSettings.getRetryInterval());
					return;
				} else {
					Session.setRetryTimeout(0);
					//Only accuracy of loc.getAccuracy reached and timeout reached
					//StopGpsManager();
					StopManagerAndResetAlarm();
					return;
				}
			}
		}

		// Don't do anything until the user-defined distance has been traversed
		if (USER_DEF_MIN_DISTANCE > 0
				&& Session.hasValidLocation()) {

			double distanceTraveled = Utilities
					.CalculateDistance(loc.getLatitude(), loc.getLongitude(),
							Session.getCurrentLatitude(),
							Session.getCurrentLongitude());

			if (USER_DEF_MIN_DISTANCE > distanceTraveled) {				
				//Only %distanceTraveled% meters travled.
				StopManagerAndResetAlarm();
				//StopGpsManager();
				return;
			}

		}
		
		//tnt.start: we need to be sure that this is a really new location or just a cached
		// value. we use the 'time' attribute to assert it.
		/*DatabaseHandler dbh = new DatabaseHandler(getApplicationContext());
		if (dbh.isLoggedLocation(loc)){
			//this location had logged last time, just omit it.
			return;
		}*/
		
		//tnt.end
		
		Toast.makeText(getApplicationContext(), "Service:onLocationChanged:new location obtained.", 
				Toast.LENGTH_SHORT).show();
		Utilities.LogInfo("New location obtained");
		//ResetCurrentFileName(false);
		Session.setLatestTimeStamp(System.currentTimeMillis());
		Session.setCurrentLocationInfo(loc);
		SetDistanceTraveled(loc);
		Notify();
		WriteToFile(loc);
		GetPreferences();
		StopManagerAndResetAlarm();
		//StopGpsManager();
		/*if (IsMainFormVisible()) {
			mainServiceClient.OnLocationUpdate(loc);
		}*/

	}
	
	/**
     * Calls file helper to write a given location to a file.
     *
     * @param loc Location object
     */
    private void WriteToFile(Location loc)
    {
    	Utilities.LogDebug("VMSLocationService.WriteToFile");
    	Toast.makeText(getApplicationContext(), 
    			"VMSservice.WriteToFile", Toast.LENGTH_SHORT).show();
    	
    	TelephonyManager telephonyManager = (TelephonyManager)
    			getSystemService(Context.TELEPHONY_SERVICE);
    	String imei = telephonyManager.getDeviceId();
    	
    	// call VMSLogger to send location data to VMS server
    	try {
    		ContentValues cv = new ContentValues();
    		
        	cv.put("server_addr", AppSettings.getVmsServer());
        	cv.put("port", AppSettings.getVmsServerPort());
        	cv.put("path", AppSettings.getVmsServerPath());
        	cv.put("deviceId", imei);
        	
        	cv.put("checkin_status", (Session.isCheckin()? "1":"0"));
        	
        	if (Session.isCheckin()) 		//come here, we've done checkin processes.
        		Session.setCheckin(false);
    		
			new VMSLogger().Write(loc, cv, getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	/**
	 * The distance between current fix and last fix.
	 * @param loc
	 */
	private void SetDistanceTraveled(Location loc)
    {
        // Distance
        if (Session.getPreviousLocationInfo() == null)
        {
            Session.setPreviousLocationInfo(loc);
        }
        // Calculate this location and the previous location location and add to the current running total distance.
        // NOTE: Should be used in conjunction with 'distance required before logging' for more realistic values.
        double distance = Utilities.CalculateDistance(
                Session.getPreviousLatitude(),
                Session.getPreviousLongitude(),
                loc.getLatitude(),
                loc.getLongitude());
        Session.setPreviousLocationInfo(loc);
        Session.setTotalTravelled(Session.getTotalTravelled() + distance);
    }

	 private void StopAlarm()
	 {
	        Utilities.LogDebug("VMSLocationService.StopAlarm");
	        Intent i = new Intent(this, VMSLocationService.class);
	        i.putExtra("getnextpoint", true);
	        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
	        nextPointAlarmManager.cancel(pi);
	 }

	 
	/*public void StopManagerAndResetAlarm() {
		// TODO Auto-generated method stub
		Utilities.LogDebug("VMSLocationService.StopManagerAndResetAlarm");
		StopGpsManager();
	}*/
	
	/*protected void StopManagerAndResetAlarm(int retryInterval)
    {
        Utilities.LogDebug("VMSLocationService.StopManagerAndResetAlarm_retryInterval");
        StopGpsManager();
        SetAlarmForNextPoint(retryInterval);
    }*/
	
	 protected void StopManagerAndResetAlarm(int retryInterval)
	    {
	        Utilities.LogDebug("VMSLocationService.StopManagerAndResetAlarm_retryInterval");
	        if( !AppSettings.shouldkeepFix() )
	        {
	            StopGpsManager();
	        }
	        SetAlarmForNextPoint(retryInterval);
	    }
	 
	 protected void StopManagerAndResetAlarm()
	    {
	        Utilities.LogDebug("VMSLocationService.StopManagerAndResetAlarm");
	        if( !AppSettings.shouldkeepFix() )
	        {
	            StopGpsManager();
	        }
	        SetAlarmForNextPoint();
	    }
	
	private void SetAlarmForNextPoint()
    {
        Utilities.LogDebug("VMSLocationService.SetAlarmForNextPoint");

        Intent i = new Intent(this, VMSLocationService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AppSettings.getMinimumSeconds() * 1000, pi);

    }

    private void SetAlarmForNextPoint(int retryInterval)
    {

        Utilities.LogDebug("VMSLocationService.SetAlarmForNextPoint_retryInterval");

        Intent i = new Intent(this, VMSLocationService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + retryInterval * 1000, pi);

    }
	
	
	
	
	/**
     * Preparing and starting the logging procedures for the VMS Location service.
     */
    protected void StartLogging() {
        Utilities.LogDebug("VMSLocationService.StartLogging");
        
        Toast.makeText(getApplicationContext(), 
    			"VMSservice.StartLogging", Toast.LENGTH_SHORT).show();
        
        //Session.setAddNewTrackSegment(true);//tnt.no needed.
        
        try{
            startForeground(NOTIFICATION_ID, new Notification());
            
        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }


        Session.setStarted(true);
        GetPreferences();
        Notify();
        //ResetCurrentFileName(true);
        //ClearForm();
        StartGpsManager(false);
    }
    
    /**
     * Stops logging, removes notification, stops GPS manager, stops email timer
     */
    public void StopLogging()
    {
        Utilities.LogDebug("VMSLocationService.StopLogging");
        
        Toast.makeText(getApplicationContext(), 
    			"VMSservice.StopLogging", Toast.LENGTH_SHORT).show();
        
        //Session.setAddNewTrackSegment(true);//tnt.no need.

        Utilities.LogInfo("Stopping logging");
        Session.setStarted(false);
        // Email log file before setting location info to null
        //AutoSendLogFileOnStop();
        //CancelAlarm();
        Session.setCurrentLocationInfo(null);
        stopForeground(true);

        RemoveNotification();
        StopAlarm();
        StopGpsManager();
        StopMainActivity();
    }
    
    /**
     * Notifies main form that logging has stopped
     */
    void StopMainActivity()
    {
        //Tung: it should do nothing;
    	
    	/*if (IsMainFormVisible())
        {
            mainServiceClient.OnStopLogging();
        }*/
    }
    
    
    /**
     * Class used for the client Binder.  All the same process of app.
     */
    public class ServiceBinder extends Binder {
    	VMSLocationService getService(){
    		return VMSLocationService.this;
    	}
    }
    
    
	@Override
	public IBinder onBind(Intent intent) {
		// To make client can get connection to this service
		return mBinder;
	}

	
	/**
     * Manages the notification in the status bar
     */
    private void Notify(){
            gpsNotifyManager = (NotificationManager) 
            		getSystemService(NOTIFICATION_SERVICE);
            ShowNotification();       
    }

    /**
     * Shows a notification icon in the status bar for GPS Logger
     */
    private void ShowNotification()
    {
        Utilities.LogDebug("VMSLocationService.ShowNotification");
        // What happens when the notification item is clicked
        Intent contentIntent = new Intent(this, VMSMainActivity.class);

        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, contentIntent,
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification nfc = new Notification(R.drawable.gps_icon1, 
        		null, System.currentTimeMillis());
        nfc.flags |= Notification.FLAG_ONGOING_EVENT;

        NumberFormat nf = new DecimalFormat("###.######");

        String contentText = getString(R.string.gpslogger_still_running);
        if (Session.hasValidLocation())
        {
            contentText = nf.format(Session.getCurrentLatitude()) + ","
                    + nf.format(Session.getCurrentLongitude());
        }

        nfc.setLatestEventInfo(getApplicationContext(), getString(R.string.gpslogger_still_running),
                contentText, pending);

        gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
        Session.setNotificationVisible(true);
    }
    
    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void RemoveNotification()
    {
        Utilities.LogDebug("VMSLocationService.RemoveNotification");
        try
        {
            if (Session.isNotificationVisible())
            {
                gpsNotifyManager.cancelAll();
            }
        }
        catch (Exception ex)
        {
            Utilities.LogError("RemoveNotification", ex);
        }
        finally
        {
            Session.setNotificationVisible(false);
        }
    }
    
    
    private final Random mGenerator = new Random();

    /** method for clients */
    public int getRandomNumber() {
      return mGenerator.nextInt(100);
    }

    /**
     * This method is called periodically to determine whether the cell tower /
     * gps providers have been enabled, and sets class level variables to those
     * values.
     */
    private void CheckTowerAndGpsStatus()
    {
        Session.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        Session.setGpsEnabled(gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }
    
    /**
     * Do checkin procedure:
     * 	- get the current location
     * 	- send the current location to VMS server
     */
    public void doCheckin(){
    	
    	boolean locationNotFix = true;
    	
    	// we seek for the current location:
    	/*while (locationNotFix){
    		StartGpsManager(false);
    		Location currentLocation = Session.getCurrentLocationInfo();
    		if (currentLocation != null){
    			// location is fixed: found it.
    			locationNotFix = false;
    			
    		} 
    	}*/
    	StartGpsManager(false);
		Location currentLocation = Session.getCurrentLocationInfo();
		if (currentLocation != null){
			// location is fixed: found it.
			locationNotFix = false;
		}
	
    	
    }
    
    /**
     * Do Start || Stop procedure:
     * 	- get the current location
     * 	- send the current location to VMS server && setting locStatus is 'start' || 'stop'
     * @param isStart 	'true' if will process start procedures; 
     * 					'false' will process stop procedures.
     */
    public void doStartStop(boolean isStart){
    	
    	boolean locationNotFix = true;
    	Session.setStartStop(true);
    	String locStatus = "";
    	if (isStart){
    		Session.setLocStatus("start");
    		locStatus = "start";
    		SetupAutoSendTimers();
            StartLogging();
    	} else {
    		Session.setLocStatus("stop");
    		locStatus = "stop";
    		StopLogging();
    	}
    		
    	// we seek for the current location:
    	/*while (locationNotFix){
    		StartGpsManager(true);
    		Location currentLocation = Session.getCurrentLocationInfo();
    		if (currentLocation != null){
    			// location is fixed: found it.
    			locationNotFix = false;
    			mainServiceClient.zoomToLocation(currentLocation, locStatus);
    		} 
    	}*/
    	
    	StartGpsManager(true);
		Location currentLocation = Session.getCurrentLocationInfo();
		if (currentLocation != null){
			// location is fixed: found it.
			locationNotFix = false;
			mainServiceClient.zoomToLocation(currentLocation, locStatus);
		}	
    }
    
    /**
     * Sets up the auto send timers based on user preferences.
     * The app will do automatic send after a %AutoSendDelay% time.
     */
    public void SetupAutoSendTimers()
    {
        Utilities.LogDebug("VMSLocationService.SetupAutoSendTimers");
        
        if (AppSettings.isAutoSendEnabled() && Session.getAutoSendDelay() > 0)
        {
            Utilities.LogDebug("Setting up autosend alarm");
            long triggerTime = System.currentTimeMillis()
                    + (long) (Session.getAutoSendDelay() * 60 * 60 * 1000);

            alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
            CancelAlarm();
            Utilities.LogDebug("New alarm intent");
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            Utilities.LogDebug("Alarm has been set");
        }
        else
        {
            Utilities.LogDebug("Checking if alarmIntent is null");
            if (alarmIntent != null)
            {
                Utilities.LogDebug("alarmIntent was null, canceling alarm");
                CancelAlarm();
            }
        }
    }
    
    /**
     * This is a support function for auto send alarm.
     */
    private void CancelAlarm()
    {
        Utilities.LogDebug("VMSLocationService.CancelAlarm");

        if (alarmIntent != null)
        {
            Utilities.LogDebug("VMSLocationService.CancelAlarm");
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Utilities.LogDebug("Pending alarm intent was null? " + String.valueOf(sender == null));
            am.cancel(sender);
        }

    }
    
    /**
     * This function is called when auto send event is fire after the receiver receives it.
     * we don't use log file, so we will process to send points which 'sent_status' is 0 (not sent yet) to the VMS Server.
     * Calls the Auto Email Helper which processes the file and sends it.
     */
    private void AutoSendLogFile()
    {

        Utilities.LogDebug("VMSLocationService.AutoSendLogFile");
        Utilities.LogVerbose("isReadyToBeAutoSent - " + Session.isReadyToBeAutoSent());

        // Check that auto emailing is enabled, there's a valid location and
        // file name.
/*        
 * if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0
                && Session.isReadyToBeAutoSent() && Session.hasValidLocation())
        {

            //Don't show a progress bar when auto-emailing
            Utilities.LogInfo("Emailing Log File");

            FileSenderFactory.SendFiles(getApplicationContext(), this);
            Session.setReadyToBeAutoSent(true);
            SetupAutoSendTimers();
        }*/
        
        // 
        if (Session.isReadyToBeAutoSent() && Session.hasValidLocation()){
        	Utilities.LogInfo("Autosend event:Sending to VMS server");
        	
        	//FileSenderFactory.SendFiles(getApplicationContext(), this);
            Session.setReadyToBeAutoSent(true);
            SetupAutoSendTimers();
        }
        
    }
    


}
