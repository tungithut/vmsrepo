package vn.com.mobifone.mtracker;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import vn.com.mobifone.mtracker.R;
import vn.com.mobifone.mtracker.common.IFileLogger;
import vn.com.mobifone.mtracker.common.Session;
import vn.com.mobifone.mtracker.common.Utilities;
import vn.com.mobifone.mtracker.db.DatabaseHandler;
import vn.com.mobifone.mtracker.db.DatabaseHandler.Waypoints;
import vn.com.mobifone.mtracker.loggers.VMSLogger;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class VMSMainActivity extends Activity implements LocationListener, OnCheckedChangeListener {

	private GoogleMap googleMap;
	private static Intent serviceIntent;
    private VMSLoggingService loggingService = null;
    final int MONITOR_FREQ = 5000*60;//5 min sampling.
    final int MAX_ROUTE_VISIBLED = 3;//number of route is display on the screen.
    
    // Color code to display the route on the Map
    private static String[] colorCodes = {	"#4d2177",
						    		"#9cb426",
						    		"#b48526",
						    		"#b43f26",
						    		"#269cb5",
						    		"#b93131",
						    		"#925a17",
						    		"#c5e9b4",
						    		"#ffdbac",
						    		"#a16175",
						    		"#63b6ab",
						    		"#f0ff00",
						    		"#ffffff",
						    		"#a69d86",
						    		"#440700",
						    		"#20391e",
						    		"#8256a4",
						    		"#025076",
						    		"#794044",
						    		"#025076",
						    		"#78724d",
						    		"#d6b6e6",
						    		"#790ead",
						    		"#5cb8ff"};
	
	/**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection()
    {

        public void onServiceDisconnected(ComponentName name)
        {
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            loggingService = ((VMSLoggingService.ServiceBinder) service).getService();
            VMSLoggingService.SetServiceClient(VMSMainActivity.this);

            if (Session.isDebugEnabled()){
            	Toast.makeText(getApplicationContext(), "onServiced connected !", Toast.LENGTH_SHORT).show();
            }
            //Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);

            //buttonSinglePoint.setOnClickListener(VMSMainActivity.this);

            /*if (Session.isStarted())
            {
                if (Session.isSinglePointMode())
                {
                    SetMainButtonEnabled(false);
                }
                else
                {
                    SetMainButtonChecked(true);
                    SetSinglePointButtonEnabled(false);
                }

                DisplayLocationInfo(Session.getCurrentLocationInfo());
            }*/

            // Form setup - toggle button, display existing location info
            //ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
            //buttonOnOff.setOnCheckedChangeListener(VMSMainActivity.this);
        }
    };
	
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Temporary for debugging purpose:
		Session.setDebugEnabled(false);
		
		if (Session.isDebugEnabled()){
			Toast.makeText(getApplicationContext(), "MainAct.onCreate!", Toast.LENGTH_SHORT).show();
		}
		
		StartAndBindService();
		
 		// Synchnronize logged data with VMS server:
 		//	This will send all logged data which have 'sent_staus' not '1' to VMS server.
 		this.synchLoggedDataToVMS();
 		
 		// The app always work on PORTRAIT mode.
 		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		//Button stop default is disable when launched.
		Button btnStop = (Button) findViewById(R.id.buttonStop);
		btnStop.setEnabled(false);
		
		// Display Google Map
    	// Getting Google Play service availablity ?!
 		int status = GooglePlayServicesUtil
 				.isGooglePlayServicesAvailable(getBaseContext());
 		// Showing status
 		if (status != ConnectionResult.SUCCESS) { 
 			// Google Play Services are not available
 			int requestCode = 10;
 			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
 					requestCode);
 			dialog.show();
 		} else { 
 			// Getting reference to the MapFragment of activity_main.xml
 			MapFragment fm = (MapFragment) 
 					getFragmentManager().findFragmentById(R.id.map);
 			// Getting GoogleMap object from the fragment
 			googleMap = fm.getMap();
 			// Enabling MyLocation Layer of Google Map
 			googleMap.setMyLocationEnabled(true);
 			//Location location = getCurrentLocation();//display current location
 			drawRoute2();
 			//showVisitPlaceDB();
 		}
 	}
	
	/**
	 * Display the given location in the center of screen, and got focus in zoom.
	 * @param loc
	 */
	public void zoomToLocation(Location loc, String status){
		
		if (loc != null){
			// Creating a LatLng object for the current location
			LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
			// Showing the current location in Google Map
			googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
			// Zoom in the Google Map
			googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
			
			int iconId = 0;
			if ("start".equals(status)){
				iconId = R.drawable.route_start;
			} else if ("stop".equals(status)) {
				iconId = R.drawable.route_end;
			} else {
				iconId = R.drawable.map_marker_fav_place;
			}
			
			Marker place = googleMap.addMarker(new MarkerOptions()
			.position(latLng)
			.title(status)
			.icon(BitmapDescriptorFactory
					.fromResource(iconId)));
		} 
	}
	
	/**
	 * Display visit (checkin) places on the Google Map
	 * @param loc
	 */
	public void showVisitPlaces(Location loc){
		TextView tvLocation = (TextView) findViewById(R.id.tv_location);

		// Getting latitude of the current location
		double latitude = loc.getLatitude();

		// Getting longitude of the current location
		double longitude = loc.getLongitude();

		// Creating a LatLng object for the current location
		LatLng latLng = new LatLng(latitude, longitude);

		// Showing the current location in Google Map
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

		// Zoom in the Google Map
		googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));

		// Setting latitude and longitude in the TextView tv_location
		tvLocation.setText("Pin point: Latitude:" + latitude + ", Longitude:" + longitude);

		// Add an Mark to the current location:
		Marker place = googleMap.addMarker(new MarkerOptions()
				.position(latLng)
				.title("Checkin place")
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.map_marker_fav_place)));
	}
	
	/**
	 * Get visit place from DB and display on the Map.
	 * (only today checkin place should be display.
	 */
	public void showVisitPlaceDB(){
		if (Session.isDebugEnabled()){
			Toast.makeText(getApplicationContext(), "MainAct.showVisitPlaceDB!", 
					Toast.LENGTH_SHORT).show();
		}
		
		DatabaseHandler db = new DatabaseHandler(getApplicationContext());
		//List<Waypoints> list = db.getWaypoints(false);
		List<Waypoints> list = db.getCheckinWaypoints(false);
		
		for (Waypoints point : list){
			
			double latitude = point.getLatitude();
			double longitude = point.getLongtitude();
			LatLng latLng = new LatLng(latitude, longitude);
			
			//Mark visit point on the map:
			Marker place = googleMap.addMarker(new MarkerOptions()
			.position(latLng)
			.title("Checked-in place")
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.map_marker_fav_place)));
		}
	}
	
	/**
	 * This function draws Routes accross TODAY logging points from DB
	 * @param location
	 */
	private void drawRoute(){
		
		List<LatLng> route = new ArrayList<LatLng>();
		
		DatabaseHandler db = new DatabaseHandler(getApplicationContext());
		List<Waypoints> list = db.getWaypoints(false);
		
		Waypoints prev_point = null;
		LatLng firstPos;
		for (Waypoints point : list){
			
			/*if (prev_point != null 
					&& (point.getTime() - prev_point.getTime() > this.MONITOR_FREQ))*/
			if (prev_point != null)
			{
				//this is 2nd point go on && sampling time is enough to save the point:
				double latitude = point.getLatitude();
				double longitude = point.getLongtitude();
				LatLng latLng = new LatLng(latitude, longitude);
				
				route.add(latLng);
			} else if (prev_point == null) {
				//this is first point, just save it:
				prev_point = point;
			} else {
				// do nothing;
			}
			
		}
	   
		// Draw the route on the Map screen:
		PolylineOptions polOption = new PolylineOptions();
		polOption.addAll(route);
				
		googleMap.addPolyline(polOption.width(8).color(Color.RED));
		
		if (!route.isEmpty()){
			//Put a Start Market to the begining of the route:
			Marker place = googleMap.addMarker(new MarkerOptions()
			.position(route.get(0))
			.title("Start")
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.route_start)));
			
			//a Marker at the end of route:
			place = googleMap.addMarker(new MarkerOptions()
			.position(route.get(route.size()-1))
			.title("End")
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.route_end)));
		}
				
	}
	
	/**
	 * New draw route function which used the route_id implementation
	 * (3/6/2013)
	 */
	private void drawRoute2(){
		
		
		DatabaseHandler db = new DatabaseHandler(getApplicationContext());
		
		//long currentRouteId = db.getLastestRouteId();
		//List<Waypoints> list = db.getWaypointsByRoute(false, currentRouteId);
		
		List<Long> listId = db.getListOfRouteId();
		int max_loop = (listId.size() > MAX_ROUTE_VISIBLED) ? MAX_ROUTE_VISIBLED : listId.size();
		
		// Loop through all visible routes:
		for (int i = (max_loop - 1); i >= 0; i--){
			
			long id = listId.get(i);
			List<LatLng> route = new ArrayList<LatLng>();
			
			List<Waypoints> waypointlist = db.getWaypointsByRoute(false, id);
			
			for (Waypoints point : waypointlist){
				
				double latitude = point.getLatitude();
				double longitude = point.getLongtitude();
				LatLng latLng = new LatLng(latitude, longitude);
				
				route.add(latLng);
			}
		   
			// Draw the route on the Map screen:
			PolylineOptions polOption = new PolylineOptions();
			polOption.addAll(route);
			
			// Add the option for the polygon line, used for this route:
			int cCode = Color.parseColor(colorCodes[max_loop - i - 1]);
			googleMap.addPolyline(polOption.width(8).color(cCode));
			
			// Drawing the route on the Map:
			if (!route.isEmpty()){
				//Put a Start Market to the beginning of the route:
				Marker place = googleMap.addMarker(new MarkerOptions()
				.position(route.get(0))
				.title("Start")
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.route_start)));
				
				//a Marker at the end of route:
				place = googleMap.addMarker(new MarkerOptions()
				.position(route.get(route.size()-1))
				.title("End")
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.route_end)));
				
				// Put a step pin pos to each step on the route
				for (int index=0; index < route.size(); index ++){
					if (index != 0 && index != (route.size()-1)){
						place = googleMap.addMarker(new MarkerOptions()
						.position(route.get(index))
						.title(String.valueOf(index))
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.step_pin_pos)));
					}
				}
				
			}
		}
			
	}
	
	/**
	 * Using best location provider to provide the current location
	 * @return
	 */
	private Location getCurrentLocation(){
		
		// Getting LocationManager object from System Service
		// LOCATION_SERVICE
		LocationManager locationManager = (LocationManager) 
				getSystemService(LOCATION_SERVICE);

		// Creating a criteria object to retrieve provider
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);//Using GPS
		int code = criteria.getAccuracy();
		
		// Getting the name of the best provider
		String provider = locationManager.getBestProvider(criteria, true);

		// Getting Current Location
		Location location = locationManager.getLastKnownLocation(provider);
		//Location loc2 = googleMap.getMyLocation();
		
		if (location != null) {
			onLocationChanged(location);
		}
		
		locationManager.requestLocationUpdates(provider, 20000, 0, this);
		
		return location;
	}
	
	/**
     * THis function used for the MAIN UI processing to locate current location.
	 * (for testing purpose, need to remove at the release version.
     * @param location
     */
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		TextView tvLocation = (TextView) findViewById(R.id.tv_location);

		// Getting latitude of the current location
		double latitude = location.getLatitude();

		// Getting longitude of the current location
		double longitude = location.getLongitude();

		// Creating a LatLng object for the current location
		LatLng latLng = new LatLng(latitude, longitude);

		// Showing the current location in Google Map
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

		// Zoom in the Google Map
		googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));

		// Setting latitude and longitude in the TextView tv_location
		tvLocation.setText("Latitude:" + latitude + ", Longitude:" + longitude);

		// Add an Mark to the current location:
		Marker currentPos = googleMap.addMarker(new MarkerOptions()
				.position(latLng)
				.title("You are here")
				.snippet("Hello")
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.current_location_pin)));
	
		}
	
	/**
     * Called when the toggle button is clicked
     */
    /*public void onToggleClicked(View view) {
        // ask the status of toggle button
        boolean on = ((ToggleButton) view).isChecked();
        
        if (on) {        	
            // Start logging service
        	startService(new Intent(getBaseContext(),VMSLocationService.class));
        } else {
            // Stop logging service
        	startService(new Intent(getBaseContext(),VMSLocationService.class));
        }
    }*/

	/**
     * Called when the START button is clicked.
     * 
     */
    public void onClickStart(View view)
    {
    	// after started, button START is disabled.
    	view.setEnabled(false);
    	GetPreferences();
    	
    	// button STOP will be enabled.
    	Button btnStop = (Button) findViewById(R.id.buttonStop);
    	btnStop.setEnabled(true);
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "onclickstart", Toast.LENGTH_SHORT).show();
    	}
    	// sending current location to VMS server, flag it as 'start' status.
    	if (loggingService != null){
    		// start button procedure:
    		loggingService.doStartStop(true);
    	}
    }
    
    /**
     * Called when the STOP button is clicked.
     * 
     */
    public void onClickStop(View view)
    {
    	// after started, button START is disabled.
    	view.setEnabled(false);
    	
    	// button START will be enabled.
    	Button btnStart = (Button) findViewById(R.id.buttonStart);
    	btnStart.setEnabled(true);
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "onclickstop", Toast.LENGTH_SHORT).show();
    	}
    	// sending current location to VMS server, flag it as 'stop' status.
    	if (loggingService != null){
    		// stop button procedure:
    		loggingService.doStartStop(false);
    	}
    }
	
    /**
     * Called when the Checkin button is clicked.
     * 
     */
    public void onClick(View view)
    {
       //TODO: have the implementation of checkin procedures here:
    	//Session.setCheckin(true);
    	
    	if (loggingService != null){
    		//int num = loggingService.getRandomNumber();
    		//Toast.makeText(getApplicationContext(), "number: " + num, Toast.LENGTH_SHORT).show();
    		// executing the checkin procedures:
    		loggingService.doCheckin();
    	}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.optionsmenu, menu);
		return true;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService()
    {
        Utilities.LogDebug("StartAndBindService - binding now");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "MainAct.StartAndBindService!", Toast.LENGTH_SHORT).show();
        }
        serviceIntent = new Intent(this, VMSLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
        //mIsBound  = true;
    }
    
    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void StopAndUnbindServiceIfRequired()
    {
        Utilities.LogDebug("VMSMainActivity.StopAndUnbindServiceIfRequired");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "MainAct.StopAndUnbindServiceIfRequired!", Toast.LENGTH_SHORT).show();
        }
        if (Session.isBoundToService())
        {
            unbindService(gpsServiceConnection);
            Session.setBoundToService(false);
        }

        if (!Session.isStarted())
        {
            Utilities.LogDebug("StopServiceIfRequired - Stopping the service");
            //serviceIntent = new Intent(this, VMSLocationService.class);
            stopService(serviceIntent);
        }

    }
    
    @Override
    protected void onStart()
    {
        Utilities.LogDebug("VMSMainActivity.onStart");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "MainAct.onStart", Toast.LENGTH_SHORT).show();
        }
        super.onStart();
        StartAndBindService();
        
    }

    @Override
    protected void onResume()
    {
        //Utilities.LogDebug("VMSMainActivity.onResume");
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "MainAct.onResume", Toast.LENGTH_SHORT).show();
    	}
        super.onResume();
        Session.setImei(getImei());
        
        GetPreferences();
        StartAndBindService();
        
        //Scheduling display update map when it become visible:
        /*int delay = 5000;// in ms 

        Timer timer = new Timer();
        timer.schedule(new TimerTask(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				showVisitPlaceDB();
			}
        }, delay);*/
        
    }
    
    @Override
    protected void onPause()
    {
    	if (Session.isDebugEnabled()){
    		Toast.makeText(getApplicationContext(), "MainAct.onPause", Toast.LENGTH_SHORT).show();
    	}
        Utilities.LogDebug("VMSMainActivity.onPause");
        StopAndUnbindServiceIfRequired();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {

        Utilities.LogDebug("VMSMainActivity.onDestroy");
        if (Session.isDebugEnabled()){
        	Toast.makeText(getApplicationContext(), "MainAct.onDestroy", Toast.LENGTH_SHORT).show();
        }
        StopAndUnbindServiceIfRequired();
        super.onDestroy();
        //unbindService(gpsServiceConnection);//tnt.add
    }
    
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		//Utilities.LogDebug("GpsMainActivity.onCheckedChanged");

        /*if (isChecked)
        {
            //GetPreferences();
            //SetSinglePointButtonEnabled(false);
            //loggingService.SetupAutoSendTimers();
            loggingService.StartLogging();
        }
        else
        {
            //SetSinglePointButtonEnabled(true);
            loggingService.StopLogging();
        }*/
        
		// tnt:do nothing due to service is automatic started.
		if (true) 
			return;
		
        boolean on = ((ToggleButton) buttonView).isChecked();
        
        if (on) {        	
            // Start logging service
        	//startService(new Intent(getBaseContext(),VMSLocationService.class));
        	GetPreferences();
        	loggingService.StartLogging();
        } else {
            // Stop logging service
        	//startService(new Intent(getBaseContext(),VMSLocationService.class));
        	loggingService.StopLogging();
        }
        
	}
	
	private boolean mIsBound;
	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(gpsServiceConnection);
	        mIsBound = false;
	    }
	}
	
	private String getImei(){
		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String imei = 		telephonyManager.getDeviceId();
		return imei;
	}
	
	
	/**
     * Handles the hardware back-button press
     */
    /*public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService())
        {
            StopAndUnbindServiceIfRequired();
        }

        return super.onKeyDown(keyCode, event);
    }*/
	
	/**
     * Gets preferences chosen by the user
     */
    private void GetPreferences()
    {
        Utilities.PopulateAppSettings(getApplicationContext());
        //ShowPreferencesSummary();// no used.ntt.
    }

    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case R.id.mnuSettings:
                Intent settingsActivity = new Intent(getApplicationContext(), 
                		VMSSettingsActivity.class);
                startActivity(settingsActivity);
                break;
            case R.id.mnuFAQ:
                //Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                //startActivity(faqtivity);
                break;
            case R.id.mnuExit:
                // Synchnorize the logged data to VMS server before exit.
         		this.synchLoggedDataToVMS();
         		loggingService.StopLogging();
                loggingService.stopSelf();
         		// Clear session data before exit.
                clearSessionData();
                finish();
                break;
        }
        return false;
    }    
    
    /**
     * Fire when callback from the service.
     * Refresh the main UI when location update event fire.
     * @param loc
     */
    public void OnLocationUpdate(Location loc)
    {
        //Utilities.LogDebug("VMSMainActivity.OnLocationUpdate");
        //Toast.makeText(getApplicationContext(), "MainAct.onLocationUpdate", Toast.LENGTH_SHORT).show();
        //DisplayLocationInfo(loc);
        //ShowPreferencesSummary();
        //SetMainButtonChecked(true);

       /* if (Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            //SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }*/
    	
    	drawRoute2();
    	showVisitPlaceDB();
    	// Add an Mark to the current location:
    	// No need to display current location pin, 
    	//	due to Main UI already had one in the MapFragement
    	/*
    	 * Marker currentPos = googleMap.addMarker(new MarkerOptions()
    					.position(new LatLng(loc.getLatitude(), loc.getLongitude()))
    					.title("You are here")
    					.snippet("Hello")
    					.icon(BitmapDescriptorFactory
    							.fromResource(R.drawable.current_location_pin)));
    							*/
    }
    
    /**
     * We need to clear session data when user really want to EXIT the app.
     */
    public void clearSessionData(){
    	Session.setCheckin(false);
    	Session.setCheckinWithoutRoute(false);
    	Session.setStartStop(false);
    	Session.setStarted(false);
    	Session.setLaunched(false);
    }
    
    /**
     * This function should be called at the time enter the app to sync logged data in the DB with VMS server.
     */
    public void synchLoggedDataToVMS(){
    	GetPreferences();
    	VMSLogger vmsLogger = new VMSLogger();
    	try {
    		vmsLogger.autoSendLoggedData(getApplicationContext());
    		
    	} catch (Exception e) {
    		// This happended when the first time installing the app, we don't have 
    		// port/server/path setting, so the parsing function will fire illegal parsing
    		// exception, just ommit this case.
    		Utilities.LogError("synchLoggedDataToVMS", e);
    	}
    }

}
