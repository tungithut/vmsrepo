

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
     * (orignal version, work fine without route implementatin)
     * @param loc
     * @param cv
     * @param context
     */
    public void LogToDatabase(Location loc, ContentValues cv, Context context){
    	DatabaseHandler handler = new DatabaseHandler(context);
    	//Log into the DB this new obtained location:
    	handler.insertWaypoint(loc, cv);
    }
    
    /**
     * New version for Route implementation.
     * @param loc
     * @param cv
     * @param context
     */
    public void LogToDatabase2(Location loc, ContentValues cv, Context context){
    	
    	long latestId = 0;
    	DatabaseHandler handler = new DatabaseHandler(context);
    	
    	//Get the latest route ID:
    	if ("start".equals(cv.getAsString("loc_status"))){
    		// This call comes from on click 'START' button,
    		//	we need to insert new record to ROUTE table.
    		latestId = handler.insertNewRoute(loc);
    		
    	} else if ("1".equals(cv.getAsString("checkin_status"))
    			&& !"stop".equals(cv.getAsString("loc_status"))) {
    		// This call comes from on click 'CHECKIN' button,
    		//	this location point won't belonged to any routes.
    		//	with the condition that the check-in not a STOP point.
    		latestId = 0;//Anonymous route.
    		
    	} else {
    		// This call comes from other scenarios, just return the latest id:
    		//	(STOP point might come here)
    		latestId = handler.getLastestRouteId();
    	}
    	    	
    	//Log into the DB this new obtained location (with route just obtained):
    	cv.put("routeId", latestId);
    	handler.insertWaypoint(loc, cv);
    }
    
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
    	
    	// Get logged data from the DB which has 'sent_status' NOT EQUAL '1':
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

