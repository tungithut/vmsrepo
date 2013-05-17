

package vn.com.mobifone.mtracker.loggers;

import java.util.List;


import vn.com.mobifone.mtracker.common.AppSettings;
import vn.com.mobifone.mtracker.common.IActionListener;
import vn.com.mobifone.mtracker.common.IFileLogger;
import vn.com.mobifone.mtracker.common.VMSClient;
import vn.com.mobifone.mtracker.db.DatabaseHandler;
import vn.com.mobifone.mtracker.db.DatabaseHandler.Waypoints;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;






/**
 * Send locations directly to an OpenGTS server <br/>
 *
 * @author Francisco Reynoso
 */
public class VMSLogger implements IFileLogger
{

    protected final String name = "mTracker";

    public VMSLogger()
    {
    }

    /**
     * This function will insert the given location and additional status (checkin/start/stop) into Database.
     * @param loc
     * @param cv
     * @param context
     */
    public void LogToDatabase(Location loc, ContentValues cv, Context context){
    	DatabaseHandler handler = new DatabaseHandler(context);
    	//Log into the DB this new obtained location:
    	handler.insertWaypoint(loc, cv);
    }
    
    /*public void Write(Location loc, ContentValues cv, Context context) throws Exception
    {
    
        
        String server = AppSettings.getVmsServer();
        int port = Integer.parseInt(AppSettings.getVmsServerPort());
        String path = AppSettings.getVmsServerPath();
        
        
    	String server = cv.getAsString("server_addr");	//i.e:130.30.31.138
    	int port = Integer.parseInt(cv.getAsString("port"));				//i.e:8080
    	String path = cv.getAsString("path");			//i.e: /gprmc2/Data
    	String imei = cv.getAsString("deviceId");	//imei number
    	int checkin_status = cv.getAsInteger("checkin_status");
    	
        
        IActionListener al = new IActionListener()
        {
            @Override
            public void OnComplete()
            {
            }

            @Override
            public void OnFailure()
            {
            }
        };

        //OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path, al, null);
    	VMSClient vClient = new VMSClient(server, port, path, al, context);
    	//vClient.sendHTTP(deviceId, loc);
    	//Waypoints pointList = new 
    	//vClient.sendHTTP_VMS();
    	
    	DatabaseHandler handler = new DatabaseHandler(context);
    	// 1.get the list of waypoints that we have failed in sending to VMS server last time.
    	List<Waypoints> pointList = handler.getWaypoints(true);
    	// 2.sending to the VMS server again:
    	if (!pointList.isEmpty()){
    		vClient.sendHTTP_VMS(pointList);
    	}
    	// 3.sending this current location the VMS server for the first time:
    	//String imei = "12345678910";
    	
    	int retCode = vClient.sendHTTP_VMS(imei, loc,checkin_status);
    	// 4.now update the current location to the DB:

    }*/

    /**
     * This function will read data from DB then send to VMS server.
     * This function is called when auto send event fired after every %AutoSendDelay% time.
     * This function will read data from DB then send them to VMS server.
     * There are some posibilities:
     * 		+ points that have 'sent_status' is null: 	just send them to VMS, then update 'sent_status' field according to the sending result.
     * 		+ points that have 'sent_status' is 0: 		send them again to VMS, then update the 'sent_status' field according to the sending result.
     * 		+ points that have 'sent_status' is 1: 		do not send again. 
     */
    public void autoSendLoggedData(Context context){
    	
    	 IActionListener al = new IActionListener()
         {
             @Override
             public void OnComplete()
             {
             }

             @Override
             public void OnFailure()
             {
             }
         };
         
    	VMSClient vClient = new VMSClient(AppSettings.getVmsServer(), 
				    			Integer.parseInt(AppSettings.getVmsServerPort()), 
				    			AppSettings.getVmsServerPath(), 
				    			al, context);
    	
    	DatabaseHandler handler = new DatabaseHandler(context);
    	
    	// Get logged data from the DB:
    	List<Waypoints> pointList = handler.getWaypoints(true);
    	// Send logged data again to VMS server:
    	if (!pointList.isEmpty()){
    		vClient.sendHTTP_VMS(pointList);
    	}
    }
    
    @Override
    public void Annotate(String description, Location loc) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName()
    {
        return name;
    }


	@Override
	public void Write(Location loc) throws Exception {
		// TODO Auto-generated method stub
		
	}


	

	

}

