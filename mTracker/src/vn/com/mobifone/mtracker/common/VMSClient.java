

package vn.com.mobifone.mtracker.common;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import vn.com.mobifone.mtracker.db.DatabaseHandler;
import vn.com.mobifone.mtracker.db.DatabaseHandler.Waypoints;


/**
 * VMS Client to connect to VMS server
 *
 * @author Tungnt2
 */
public class VMSClient
{

    private Context applicationContext;
    private IActionListener callback;
    private String server;
    private Integer port;
    private String path;
    private AsyncHttpClient httpClient;
    private int locationsCount 			= 0;
    private int sentLocationsCount 		= 0;
    private DatabaseHandler dbHandler;
    private int checkin_status;
   
    final int SEND_SUCCESS 				= 1;
    final int SEND_FAIL 				= 0;
    final String START_STOP_SENDING 	= "1";
    final String NORMAL_SENDING 		= "2";

    public VMSClient(String server, Integer port, String path, 
    		IActionListener callback, Context applicationContext)
    //public VMSClient(String server, Integer port, String path, Context applicationContext)
    {
        this.server = server;
        this.port = port;
        this.path = path;
        this.callback = callback;
        this.applicationContext = applicationContext;
        this.dbHandler = new DatabaseHandler(applicationContext);
    }

    

    /**
     * Send locations using HTTP GET request to the VMS server
     * Currently only used to send data that has sent failed last time  to server.
     * Note: do update the after-sending result into the DB again.
     * <p/>
     * @param id        id of the device
     * @param locations locations
     */

    public void sendHTTP_VMS(List<Waypoints> pointList)
    {
        try
        {
            locationsCount = pointList.size();
            StringBuilder url = new StringBuilder();
            url.append("http://");
            url.append(getURL());

            httpClient = new AsyncHttpClient();

            for (Waypoints point : pointList)
            {
                RequestParams params = new RequestParams();
                params.put("imei", String.valueOf(point.getImei()));
                params.put("accuracy", String.valueOf(point.getAccuracy()));
                params.put("altitude", String.valueOf(point.getAltitude()));
                params.put("bearing", String.valueOf(point.getBearing()));
                params.put("visit", String.valueOf(point.getCheckin_status()));
                params.put("lat", String.valueOf(point.getLatitude()));
                params.put("lon", String.valueOf(point.getLongtitude()));
                params.put("speed", String.valueOf(point.getSpeed()));
                params.put("timestamp", String.valueOf(point.getTime()));
                params.put("locStatus", point.getLoc_status());
                params.put("routeId", String.valueOf(point.getRouteId()));
                
                if ("start".equals(point.getLoc_status())
                		|| "stop".equals(point.getLoc_status())) {
                	//this is start or ending point, 'opt' must be '1'
                	params.put("opt", START_STOP_SENDING);
                } else {
                	// this is a normal point, 'opt' must be '2'
                	params.put("opt", NORMAL_SENDING);
                }
              
                Utilities.LogDebug("Sending URL " + url + " with params " + params.toString());
                httpClient.get(applicationContext, url.toString(), 
                		params, new MyResponseHandlerSavedData(this, point, params.toString()));
                
            }
        }
        catch (Exception e)
        {
            Utilities.LogError("VMSClient.sendHTTP", e);
            OnFailure();
        }
    }
    
    /**
     * Send one location to VMS Server.
     * @param location
     * @param cv
     * @return
     */
    public int sendHTTP_VMS(Location loc, ContentValues cv){
    
    	int retCode = 0;
        try
        {
	            StringBuilder url = new StringBuilder();
	            url.append("http://");
	            url.append(getURL());
	
	            httpClient = new AsyncHttpClient();            
                //saving this value to use in the event of response handler.
            	
            	RequestParams params = new RequestParams();
                
            	params.put("imei", cv.getAsString("imei"));
                params.put("visit", cv.getAsString("checkin_status"));
                params.put("locStatus", cv.getAsString("loc_status"));
                
                params.put("accuracy", String.valueOf(loc.getAccuracy()));
                params.put("altitude", String.valueOf(loc.getAltitude()));
                params.put("bearing", String.valueOf(loc.getBearing()));
                params.put("lat", String.valueOf(loc.getLatitude()));
                params.put("lon", String.valueOf(loc.getLongitude()));
                params.put("speed", String.valueOf(loc.getSpeed()));
                params.put("timestamp", String.valueOf(loc.getTime()));
                
                
                if ("start".equals(Session.getLocStatus())
                		|| "stop".equals(Session.getLocStatus())) {
                	//this is start or ending point, 'opt' must be '1'
                	params.put("opt", START_STOP_SENDING);
                	
                } else {
                	// this is a normal point, 'opt' must be '2'
                	params.put("opt", NORMAL_SENDING);
                }
                
                Utilities.LogDebug("Sending URL " + url + " with params " + params.toString());
                httpClient.get(applicationContext, url.toString(), params, 
                		new MyResponseHandler(this, loc, cv));
                
            
        }
        catch (Exception e)
        {
            Utilities.LogError("VMSClient.sendHTTP", e);
            OnFailure();
            return retCode;
        }
    	return retCode;
    }
    

    public void sendRAW(String id, Location location)
    {
        // TODO
    }

    private void sendRAW(String id, Location[] locations)
    {
        // TODO
    }

    private String getURL()
    {
        StringBuilder url = new StringBuilder();
        url.append(server);
        if (port != null)
        {
            url.append(":");
            url.append(port);
        }
        if (path != null)
        {
            url.append(path);
        }
        return url.toString();
    }


    private class MyResponseHandler extends AsyncHttpResponseHandler
    {
        private VMSClient callback;
        private ContentValues content;
        private Location location;

        public MyResponseHandler(VMSClient callback, Location loc, ContentValues cv)
        {
            super();
            this.callback = callback;
            this.content = cv;
            this.location = loc;
        }

        @Override
        public void onSuccess(String response)
        {
            Utilities.LogInfo("Response Success :" + response);
            // response must be 'OK' to be sure the data sent successfully
            String imei = Session.getImei();
            if (response != null && "OK".equals(response)){
            	content.put("sent_status", SEND_SUCCESS);
	            dbHandler.insertWaypoint(location, content);
	            callback.OnCompleteLocation();
            } else {
            	// still failure.
            	content.put("sent_status", SEND_FAIL);
            	dbHandler.insertWaypoint(location, content);
            	callback.OnFailure();
            }
            //reset the start/stop status; after this point always normal point:
        	Session.setLocStatus("");
        }

        @Override
		public void onFailure(Throwable arg0) {
			// TODO Auto-generated method stub
			super.onFailure(arg0);
		}

		@Override
		public void onFinish() {
			// TODO Auto-generated method stub
			super.onFinish();
		}

		@Override
		public void onStart() {
			// TODO Auto-generated method stub
			super.onStart();
		}

		@Override
        public void onFailure(Throwable e, String response)
        {
            Utilities.LogError("OnCompleteLocation.MyResponseHandler Failure with response :" + response, new Exception(e));
            content.put("sent_status", SEND_FAIL);
        	dbHandler.insertWaypoint(location, content);
            callback.OnFailure();
        }
    }
    
    /**
     * this listener version is used for the sending fail-sent data to server.
     * Due to fail-sent data already saved in the DB, no need to save them again.
     * Instead, find them and update the sent-status become '1' in case of success sending.
     * @author tungnt2
     *
     */
    private class MyResponseHandlerSavedData extends AsyncHttpResponseHandler
    {
        private VMSClient callback;
        private Waypoints point;
        private String params;

        public MyResponseHandlerSavedData(VMSClient callback, Waypoints p, String sParams)
        {
            super();
            this.callback = callback;
            this.point = p;
            this.params = sParams;
        }

        @Override
        public void onSuccess(String response)
        {
            //Utilities.LogInfo("Response Success :" + response);
            // response must be 'OK' to be sure the data sent successfully
            if (response != null && 
            		("OK".equals(response) || "OK\n\r\n".equals(response))){
            	// update into DB the sent_status become '1'.
            	Utilities.LogInfo("onSuccess.Success :" + response + " params:" + params);
            	//Utilities.LogError("onSucess.Failure :" + response + " params:" + params, new Exception(""));
            	dbHandler.updateWaypointSent(point, SEND_SUCCESS);
	            callback.OnCompleteLocation();
            } else {
            	// still failure.Do nothing (we already had data).
            	Utilities.LogError("onSucess.Failure :" + response + " params:" + params, new Exception(""));
            	callback.OnFailure();
            }
        }

        @Override
		public void onFailure(Throwable arg0) {
			// TODO Auto-generated method stub
			super.onFailure(arg0);
		}

		@Override
		public void onFinish() {
			// TODO Auto-generated method stub
			super.onFinish();
		}

		@Override
		public void onStart() {
			// TODO Auto-generated method stub
			super.onStart();
		}

		@Override
        public void onFailure(Throwable e, String response)
        {
            Utilities.LogError("onFailure.MyResponseHandlerSavedData Failure with response :" + response, new Exception(e));
            //Do nothing. we already had data.
            callback.OnFailure();
        }
    }

    public void OnCompleteLocation()
    {
        sentLocationsCount += 1;
        Utilities.LogDebug("OnCompleteLocation.Sent locations count: " + sentLocationsCount + "/" + locationsCount);
        if (locationsCount == sentLocationsCount)
        {
            OnComplete();
        }
    }

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
        httpClient.cancelRequests(applicationContext, true);
        callback.OnFailure();
    }

    

    /**
     * Converts given meters/second to nautical mile/hour.
     *
     * @param mps meters per second
     * @return knots
     */
    public static double MetersPerSecondToKnots(double mps)
    {
        // Google "meters per second to knots"
        return mps * 1.94384449;
    }

}
