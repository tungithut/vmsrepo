

//TODO: Simplify email logic (too many methods)
//TODO: Allow messages in IActionListener callback methods
//TODO: Handle case where a fix is not found and GPS gives up - restart alarm somehow?

package vn.com.mobifone.mtracker;

import android.app.*;
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
import android.widget.Toast;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import vn.com.mobifone.mtracker.common.AppSettings;
import vn.com.mobifone.mtracker.common.IActionListener;
import vn.com.mobifone.mtracker.common.Session;
import vn.com.mobifone.mtracker.common.Utilities;
import vn.com.mobifone.mtracker.loggers.VMSLogger;
import vn.com.mobifone.mtracker.senders.AlarmReceiver;

public class VMSLoggingService extends Service implements IActionListener
{
    private static NotificationManager gpsNotifyManager;
    private static int NOTIFICATION_ID = 8675309;

    private final IBinder mBinder = new ServiceBinder();
    private static VMSMainActivity mainServiceClient;

    // ---------------------------------------------------
    // Helpers and managers
    // ---------------------------------------------------
    private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    LocationManager gpsLocationManager;
    private LocationManager towerLocationManager;

    private Intent alarmIntent;

    AlarmManager nextPointAlarmManager;

    // ---------------------------------------------------

    @Override
    public IBinder onBind(Intent arg0)
    {
        Utilities.LogDebug("VMSLoggingService.onBind");
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        Utilities.LogDebug("VMSLoggingService.onCreate");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service:onCreate", Toast.LENGTH_SHORT).show();
        }
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Utilities.LogInfo("GPSLoggerService created");
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Utilities.LogDebug("VMSLoggingService.onStart");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service: onStart", Toast.LENGTH_SHORT).show();
        }
        HandleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Utilities.LogDebug("VMSLoggingService.onStartCommand");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service: onStartCommand", Toast.LENGTH_SHORT).show();
        }
        HandleIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        Utilities.LogWarning("VMSLoggingService is being destroyed by Android OS.");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.onDestroy", Toast.LENGTH_SHORT).show();
        }
        mainServiceClient = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        Utilities.LogWarning("Android is low on memory.");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.onDestroy:low memory", Toast.LENGTH_SHORT);
        }
        super.onLowMemory();
    }

    private void HandleIntent(Intent intent)
    {

        Utilities.LogDebug("VMSLoggingService.handleIntent");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.handleIntent", Toast.LENGTH_SHORT).show();
        }
        GetPreferences();

        Utilities.LogDebug("Null intent? " + String.valueOf(intent == null));

        if (intent != null)
        {
            Bundle bundle = intent.getExtras();

            if (bundle != null)
            {
                //boolean stopRightNow = bundle.getBoolean("immediatestop");//this event belongs to shortcut 'start' on home screen
                //boolean startRightNow = bundle.getBoolean("immediate");//this event belongs to shortcut 'stop' on home screen
                boolean sendEmailNow = bundle.getBoolean("emailAlarm");//this event belong to auto send event.
                boolean getNextPoint = bundle.getBoolean("getnextpoint");

                //Utilities.LogDebug("startRightNow - " + String.valueOf(startRightNow));

                Utilities.LogDebug("emailAlarm - " + String.valueOf(sendEmailNow));

                /*if (startRightNow)
                {
                    Utilities.LogInfo("Auto starting logging");

                    StartLogging();
                }

                if (stopRightNow)
                {
                    Utilities.LogInfo("Auto stop logging");
                    StopLogging();
                }*/

                if (sendEmailNow)
                {

                    Utilities.LogDebug("setReadyToBeAutoSent = true");

                    Session.setReadyToBeAutoSent(true);
                    AutoSendLogFile();
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

        }
    }

    @Override
    public void OnComplete()
    {
        //Utilities.HideProgress();
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "service.onComplete", Toast.LENGTH_SHORT).show();
    	}
    }

    @Override
    public void OnFailure()
    {
        //Utilities.HideProgress();
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "service.onFailure", Toast.LENGTH_SHORT).show();
    	}
    }

    /**
     * Can be used from calling classes as the go-between for methods and
     * properties.
     */
    public class ServiceBinder extends Binder
    {
        public VMSLoggingService getService()
        {
            Utilities.LogDebug("ServiceBinder.getService");
            return VMSLoggingService.this;
        }
    }

    /**
     * Sets up the auto sending timers based on user preferences.
     * default frequently sending out: 1 minute 
     */
    public void SetupAutoSendTimers()
    {
        Utilities.LogDebug("VMSLoggingService.SetupAutoSendTimers");
        Utilities.LogDebug("isAutoSendEnabled - " + String.valueOf(AppSettings.isAutoSendEnabled()));
        Utilities.LogDebug("Session.getAutoSendDelay - " + String.valueOf(Session.getAutoSendDelay()));
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

    private void CancelAlarm()
    {
        Utilities.LogDebug("VMSLoggingService.CancelAlarm");

        if (alarmIntent != null)
        {
            Utilities.LogDebug("VMSLoggingService.CancelAlarm");
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Utilities.LogDebug("Pending alarm intent was null? " + String.valueOf(sender == null));
            am.cancel(sender);
        }

    }

    /**
     * Method to be called if user has chosen to auto email log files when he
     * stops logging
     */
    private void AutoSendLogFileOnStop()
    {
        Utilities.LogDebug("VMSLoggingService.AutoSendLogFileOnStop");
        Utilities.LogVerbose("isAutoSendEnabled - " + AppSettings.isAutoSendEnabled());
        // autoSendDelay 0 means send it when you stop logging.
        if (AppSettings.isAutoSendEnabled() && Session.getAutoSendDelay() == 0)
        {
            Session.setReadyToBeAutoSent(true);
            AutoSendLogFile();
        }
    }

    /**
     * This function is called when auto send event fired after every %AutoSendDelay% time.
     * This function will read data from DB then send them to VMS server.
     * There are some posibilities:
     * 		+ points that have 'sent_status' is null: 	just send them to VMS, then update 'sent_status' field according to the sending result.
     * 		+ points that have 'sent_status' is 0: 		send them again to VMS, then update the 'sent_status' field according to the sending result.
     * 		+ points that have 'sent_status' is 1: 		do not send again. 
     * Calls the Auto Email Helper which processes the file and sends it.
     */
    private void AutoSendLogFile()
    {

        Utilities.LogDebug("VMSLoggingService.AutoSendLogFile");
        Utilities.LogVerbose("isReadyToBeAutoSent - " + Session.isReadyToBeAutoSent());

        // Check that auto emailing is enabled, there's a valid location and
        // file name.
        /*if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0
                && Session.isReadyToBeAutoSent() && Session.hasValidLocation())
        {

            //Don't show a progress bar when auto-emailing
            Utilities.LogInfo("Emailing Log File");

            FileSenderFactory.SendFiles(getApplicationContext(), this);
            Session.setReadyToBeAutoSent(true);
            SetupAutoSendTimers();
        }*/
        
        if ( Session.isReadyToBeAutoSent() && Session.hasValidLocation()) {

            //Don't show a progress bar when auto-emailing
            Utilities.LogInfo("Auto sending to VMS");
            
            VMSLogger vmsLogger = new VMSLogger();
            vmsLogger.autoSendLoggedData(getApplicationContext());
            
            Session.setReadyToBeAutoSent(true);
            SetupAutoSendTimers();
        }
    }

    /*protected void ForceEmailLogFile()
    {

        Utilities.LogDebug("VMSLoggingService.ForceEmailLogFile");
        if (AppSettings.isAutoSendEnabled() && Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0)
        {
            if (IsMainFormVisible())
            {
                Utilities.ShowProgress(mainServiceClient.GetActivity(), getString(R.string.autosend_sending),
                        getString(R.string.please_wait));
            }

            Utilities.LogInfo("Force emailing Log File");
            FileSenderFactory.SendFiles(getApplicationContext(), this);
        }
    }
*/

    /**
     * Sets the activity form for this service. The activity form needs to
     * implement IGpsLoggerServiceClient.
     *
     * @param mainForm The calling client
     */
    protected static void SetServiceClient(VMSMainActivity mainForm)
    {
        mainServiceClient = mainForm;
    }

    /**
     * Gets preferences chosen by the user and populates the AppSettings object.
     * Also sets up email timers if required.
     */
    private void GetPreferences()
    {
        Utilities.LogDebug("VMSLoggingService.GetPreferences");
        Utilities.PopulateAppSettings(getApplicationContext());

        Utilities.LogDebug("Session.getAutoSendDelay: " + Session.getAutoSendDelay());
        Utilities.LogDebug("AppSettings.getAutoSendDelay: " + AppSettings.getAutoSendDelay());

        if (Session.getAutoSendDelay() != AppSettings.getAutoSendDelay())
        {
            Utilities.LogDebug("Old autoSendDelay - " + String.valueOf(Session.getAutoSendDelay())
                    + "; New -" + String.valueOf(AppSettings.getAutoSendDelay()));
            Session.setAutoSendDelay(AppSettings.getAutoSendDelay());
            SetupAutoSendTimers();
        }

    }

    /**
     * Resets the form, resets file name if required, reobtains preferences
     */
    protected void StartLogging()
    {
        Utilities.LogDebug("VMSLoggingService.StartLogging");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.startLogging", Toast.LENGTH_SHORT).show();
        }
        Session.setAddNewTrackSegment(true);

        if (Session.isStarted())
        {
            return;
        }

        Utilities.LogInfo("Starting logging procedures");
        try
        {
            startForeground(NOTIFICATION_ID, new Notification());
        }
        catch (Exception ex)
        {
            System.out.print(ex.getMessage());
        }


        Session.setStarted(true);

        GetPreferences();
        Notify();
        //ResetCurrentFileName(true);
        ClearForm();
        StartGpsManager();

    }

    /**
     * Asks the main service client to clear its form.
     */
    private void ClearForm()
    {
        /*if (IsMainFormVisible())
        {
            mainServiceClient.ClearForm();
        }*/
    }

    /**
     * Stops logging, removes notification, stops GPS manager, stops email timer
     */
    public void StopLogging()
    {
        Utilities.LogDebug("VMSLoggingService.StopLogging");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.stopLogging", Toast.LENGTH_SHORT).show();
        }
        Session.setAddNewTrackSegment(true);

        Utilities.LogInfo("Stopping logging");
        Session.setStarted(false);
        // Email log file before setting location info to null
        AutoSendLogFileOnStop();
        CancelAlarm();
        Session.setCurrentLocationInfo(null);
        stopForeground(true);

        RemoveNotification();
        StopAlarm();
        StopGpsManager();
        StopMainActivity();
    }

    /**
     * Manages the notification in the status bar
     */
    private void Notify()
    {

        Utilities.LogDebug("VMSLoggingService.Notify");
        if (AppSettings.shouldShowInNotificationBar())
        {
            gpsNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            ShowNotification();
        }
        else
        {
            RemoveNotification();
        }
    }

    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void RemoveNotification()
    {
        Utilities.LogDebug("VMSLoggingService.RemoveNotification");
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

    /**
     * Shows a notification icon in the status bar for GPS Logger
     */
    private void ShowNotification()
    {
        Utilities.LogDebug("VMSLoggingService.ShowNotification");
        // What happens when the notification item is clicked
        Intent contentIntent = new Intent(this, VMSMainActivity.class);

        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, contentIntent,
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification nfc = new Notification(R.drawable.mtrack48_notify, null, System.currentTimeMillis());
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
     * Starts the location manager. There are two location managers - GPS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GPS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    private void StartGpsManager()
    {
        Utilities.LogDebug("VMSLoggingService.StartGpsManager");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.startGPSManager", Toast.LENGTH_SHORT).show();
        }

        GetPreferences();

        if (gpsLocationListener == null)
        {
            gpsLocationListener = new GeneralLocationListener(this);
        }

        if (towerLocationListener == null)
        {
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
                    1000, 0,
                    gpsLocationListener);

            gpsLocationManager.addGpsStatusListener(gpsLocationListener);

            Session.setUsingGps(true);
        }
        else if (Session.isTowerEnabled())
        {
            Utilities.LogInfo("Requesting tower location updates");
            Session.setUsingGps(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000, 0,
                    towerLocationListener);

        }
        else
        {
            Utilities.LogInfo("No provider available");
            Session.setUsingGps(false);
            //SetStatus(R.string.gpsprovider_unavailable);
            //SetFatalMessage(R.string.gpsprovider_unavailable);
            StopLogging();
            return;
        }

        //SetStatus(R.string.started);
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
     * Stops the location managers
     */
    private void StopGpsManager()
    {
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "service.StopGpsManager", Toast.LENGTH_SHORT).show();
    	}
        Utilities.LogDebug("VMSLoggingService.StopGpsManager");

        if (towerLocationListener != null)
        {
            Utilities.LogDebug("Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gpsLocationListener != null)
        {
            Utilities.LogDebug("Removing gpsLocationManager updates");
            gpsLocationManager.removeUpdates(gpsLocationListener);
            gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
        }

        //SetStatus(getString(R.string.stopped));
    }

    /**
     * Sets the current file name based on user preference.
     */
    /*private void ResetCurrentFileName(boolean newStart)
    {

        Utilities.LogDebug("VMSLoggingService.ResetCurrentFileName");

        String newFileName = Session.getCurrentFileName();

        if(AppSettings.isStaticFile())
        {
            newFileName = AppSettings.getStaticFileName();
            Session.setCurrentFileName(AppSettings.getStaticFileName());
        }
        else if (AppSettings.shouldCreateNewFileOnceADay())
        {
            // 20100114.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        }
        else if (newStart)
        {
            // 20100114183329.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        }

        if (IsMainFormVisible())
        {
            mainServiceClient.onFileName(newFileName);
        }

    }*/

    /**
     * Gives a status message to the main service client to display
     *
     * @param status The status message
     */
    void SetStatus(String status)
    {
        /*if (IsMainFormVisible())
        {
            mainServiceClient.OnStatusMessage(status);
        }*/
    }

    /**
     * Gives an error message to the main service client to display
     *
     * @param messageId ID of string to lookup
     */
    void SetFatalMessage(int messageId)
    {
        /*if (IsMainFormVisible())
        {
            mainServiceClient.OnFatalMessage(getString(messageId));
        }*/
    }

    /**
     * Gets string from given resource ID, passes to SetStatus(String)
     *
     * @param stringId ID of string to lookup
     */
    private void SetStatus(int stringId)
    {
        String s = getString(stringId);
        SetStatus(s);
    }

    /**
     * Notifies main form that logging has stopped
     */
    void StopMainActivity()
    {
        /*if (IsMainFormVisible())
        {
            mainServiceClient.OnStopLogging();
        }*/
    }


    /**
     * Stops location manager, then starts it.
     */
    void RestartGpsManagers()
    {
        Utilities.LogDebug("VMSLoggingService.RestartGpsManagers");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.RestartGpsManager", Toast.LENGTH_SHORT).show();
        }
        StopGpsManager();
        StartGpsManager();
    }


    /**
     * This event is raised when the GeneralLocationListener has a new location.
     * This method in turn updates notification, writes to file, reobtains
     * preferences, notifies main service client and resets location managers.
     *
     * @param loc Location object
     */
    void OnLocationChanged(Location loc)
    {
        int retryTimeout = Session.getRetryTimeout();
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "service.OnLocationChanged", Toast.LENGTH_SHORT).show();
        }

        if (!Session.isStarted())
        {
            Utilities.LogDebug("OnLocationChanged called, but Session.isStarted is false");
            StopLogging();
            return;
        }

        Utilities.LogDebug("VMSLoggingService.OnLocationChanged");


        long currentTimeStamp = System.currentTimeMillis();

        // Wait some time even on 0 frequency so that the UI doesn't lock up

        if ((currentTimeStamp - Session.getLatestTimeStamp()) < 1000)
        {
            return;
        }

        // Don't do anything until the user-defined time has elapsed
        if ((currentTimeStamp - Session.getLatestTimeStamp()) < (AppSettings.getMinimumSeconds() * 1000))
        {
            return;
        }

        // Don't do anything until the user-defined accuracy is reached
        if (AppSettings.getMinimumAccuracyInMeters() > 0)
        {
          if(AppSettings.getMinimumAccuracyInMeters() < Math.abs(loc.getAccuracy()))
            {
                if(retryTimeout < 50)
                {
                    Session.setRetryTimeout(retryTimeout+1);
                    //SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached");
                    Utilities.LogDebug("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached");
                    
                    StopManagerAndResetAlarm(AppSettings.getRetryInterval());
                    return;
                }
                else
                {
                    Session.setRetryTimeout(0);
                    //SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached and timeout reached");
                    Utilities.LogDebug("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached and timeout reached");
                    
                    StopManagerAndResetAlarm();
                    return;
                }
            }
        }

        //Don't do anything until the user-defined distance has been traversed
        if (AppSettings.getMinimumDistanceInMeters() > 0 && Session.hasValidLocation())
        {

            double distanceTraveled = Utilities.CalculateDistance(loc.getLatitude(), loc.getLongitude(),
                    Session.getCurrentLatitude(), Session.getCurrentLongitude());

            if (AppSettings.getMinimumDistanceInMeters() > distanceTraveled)
            {
                //SetStatus("Only " + String.valueOf(Math.floor(distanceTraveled)) + " m traveled.");
                Utilities.LogDebug("Only " + String.valueOf(Math.floor(distanceTraveled)) + " m traveled.");
                
                StopManagerAndResetAlarm();
                return;
            }

        }


        Utilities.LogInfo("New location obtained");
        //ResetCurrentFileName(false);
        Session.setLatestTimeStamp(System.currentTimeMillis());
        Session.setCurrentLocationInfo(loc);
        SetDistanceTraveled(loc);
        Notify();
        WriteToFile(loc);
        GetPreferences();
        StopManagerAndResetAlarm();

        if (IsMainFormVisible())
        {
            mainServiceClient.OnLocationUpdate(loc);
        }
    }

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

    protected void StopManagerAndResetAlarm()
    {
        Utilities.LogDebug("VMSLoggingService.StopManagerAndResetAlarm");
        if( !AppSettings.shouldkeepFix() )
        {
            StopGpsManager();
        }
        SetAlarmForNextPoint();
    }

    protected void StopManagerAndResetAlarm(int retryInterval)
    {
        Utilities.LogDebug("VMSLoggingService.StopManagerAndResetAlarm_retryInterval");
        if( !AppSettings.shouldkeepFix() )
        {
            StopGpsManager();
        }
        SetAlarmForNextPoint(retryInterval);
    }

    private void StopAlarm()
    {
        Utilities.LogDebug("VMSLoggingService.StopAlarm");
        Intent i = new Intent(this, VMSLoggingService.class);
        i.putExtra("getnextpoint", true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);
    }


    private void SetAlarmForNextPoint()
    {

        Utilities.LogDebug("VMSLoggingService.SetAlarmForNextPoint");

        Intent i = new Intent(this, VMSLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AppSettings.getMinimumSeconds() * 1000, pi);

    }

    private void SetAlarmForNextPoint(int retryInterval)
    {

        Utilities.LogDebug("VMSLoggingService.SetAlarmForNextPoint_retryInterval");

        Intent i = new Intent(this, VMSLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + retryInterval * 1000, pi);

    }

    /**
     * Calls file helper to write a given location to a file.
     *
     * @param loc Location object
     */
    /*private void WriteToFile(Location loc)
    {
        Utilities.LogDebug("VMSLoggingService.WriteToFile");
        List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers();
        Session.setAddNewTrackSegment(false);

        for (IFileLogger logger : loggers)
        {
            try
            {
                logger.Write(loc);
                Session.setAllowDescription(true);
            }
            catch (Exception e)
            {
                SetStatus(R.string.could_not_write_to_file);
            }
        }

    }*/
    
    /**
     * This function is called at the time onLocationChange fired.
     * It will write found current location into Database for following sending procedure provided by alarm events.
     * @param loc
     */
    private void WriteToFile(Location loc)
    {
    	Utilities.LogDebug("VMSLoggingService.WriteToFile");
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), 
    			"VMSservice.WriteToFile", Toast.LENGTH_SHORT).show();
    	}
    	TelephonyManager telephonyManager = (TelephonyManager)
    			getSystemService(Context.TELEPHONY_SERVICE);
    	String imei = telephonyManager.getDeviceId();
    	
    	//Session.setAddNewTrackSegment(false);
    	// call VMSLogger to send location data to VMS server
    	try {
    		ContentValues cv = new ContentValues();
    		
        	cv.put("imei", imei);
        	cv.put("checkin_status", (Session.isCheckin()? "1":"0"));
        	cv.put("loc_status", Session.getLocStatus());
        	//Session.setAllowDescription(true);
        	
			//new VMSLogger().LogToDatabase(loc, cv, getApplicationContext());
        	new VMSLogger().LogToDatabase2(loc, cv, getApplicationContext());//Added route implements.
			
			// if we've come from 'stop' process, at this point we have successfully write on the DB,
			// we need to stop the logging process.
			if ("stop".equals(Session.getLocStatus())){
				// Immediatately send the logged data to VMS server
				Utilities.LogInfo("Auto sending to VMS after STOP");	            
	            VMSLogger vmsLogger = new VMSLogger();
	            vmsLogger.autoSendLoggedData(getApplicationContext());
				
	            // Now will stop logging process.
				Toast.makeText(getApplicationContext(), R.string.vms_stop_message, Toast.LENGTH_LONG).show();
				StopLogging();
	            
			} else if ("start".equals(Session.getLocStatus())){
				// Start message
				Toast.makeText(getApplicationContext(), R.string.vms_start_message, Toast.LENGTH_LONG).show();
			} else if (Session.isCheckin()){
				// Checkin message
				//Toast.makeText(getApplicationContext(), R.string.vms_checkin_message, Toast.LENGTH_LONG).show();
			}
			
			// Reset all checkin/start/stop status.
			if (Session.isCheckin()) {		
				//come here, we've done checkin processes.
        		Session.setCheckin(false);
        		
        		if (Session.isCheckinWithoutRoute()){
        			// in case of checkin action without route, must explicitly 'stop' the logging progress.
        			StopLogging();
        		}
        		
        		Toast.makeText(getApplicationContext(), R.string.vms_checkin_message, Toast.LENGTH_LONG).show();
			}
    		
        	Session.setLocStatus("");// reset loc status.
        	Session.setStartStop(false);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    

    /**
     * Informs the main service client of the number of visible satellites.
     *
     * @param count Number of Satellites
     */
    void SetSatelliteInfo(int count)
    {
        /*if (IsMainFormVisible())
        {
            mainServiceClient.OnSatelliteCount(count);
        }*/
    }


    private boolean IsMainFormVisible()
    {
        return mainServiceClient != null;
    }
    
    /**
     * Do checkin procedure:
     * 	- get the current location
     * 	- send the current location to VMS server
     */
    public void doCheckin(){
    	
    	boolean locationNotFix = true;
    	
    	// validate if user has clicked on the 'checkin' double times:
    	if (Session.isCheckin()){
    		// do nothing if we not yet finished checkin at recent location
    		Toast.makeText(getApplicationContext(), "Be patient, please wait for the check-in progress...", Toast.LENGTH_LONG).show();
    		return ;
    	}
    	
    	Session.setCheckin(true);
    	int count = 5;
    	
    	if (!Session.isStarted()){
    		//User click on 'checkin' without any awareness of clicking on the Start button.
    		// we still have to recognize their work.
    		Session.setCheckinWithoutRoute(true);
    		SetupAutoSendTimers();
            StartLogging();
    	} else {
    		// Checkin action on the current Route.
    		Session.setCheckinWithoutRoute(false);
    	}
    	
    	// we seek for the current location:
    	/*while (locationNotFix && (count-- > 0)){
    		StartGpsManager();
    		Location currentLocation = Session.getCurrentLocationInfo();
    		if (currentLocation != null){
    			// location is fixed: found it.
    			locationNotFix = false;
    			
    		} 
    	}*/	
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
    	int count = 5;
    	
    	if (isStart){
    		Session.setLocStatus("start");
    		locStatus = "start";
    		
    		//from main activity
    		SetupAutoSendTimers();
            StartLogging();
    	} else {
    		Session.setLocStatus("stop");
    		locStatus = "stop";
    		
    		//The stop processes is do the same as the 'start' process, except for at the end of life, when
    		// data was successfully logged into DB, it need to call to StopLogging();
    		//from main activity
    		//StopLogging();
    		//from main activity
    		// 20/5/2013: come here, we assume that the 'start' process is already on-going, it means that the logging process is logging,
    		// and the timer alarm is already set. No need to setup timer and start logging. we always have logging and timer available at this moment.
    		if (false){
    			SetupAutoSendTimers();
    			StartLogging();
    		}
    	}
    		
    	Location currentLocation = Session.getCurrentLocationInfo();
    	if (currentLocation!=null){
    		mainServiceClient.zoomToLocation(currentLocation, locStatus);
    	}
    	
    	// we seek for the current location:
    	/*while (locationNotFix && (count-- > 0)){
    		StartGpsManager();
    		Location currentLocation = Session.getCurrentLocationInfo();
    		if (currentLocation != null){
    			// location is fixed: found it.
    			locationNotFix = false;
    			mainServiceClient.zoomToLocation(currentLocation, locStatus);
    		} 
    	}*/
    	
    	
    }


}
