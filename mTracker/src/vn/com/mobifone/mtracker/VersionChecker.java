package vn.com.mobifone.mtracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;


import vn.com.mobifone.mtracker.common.AppSettings;
import vn.com.mobifone.mtracker.common.Session;
import vn.com.mobifone.mtracker.common.VMSXmlParser;
import vn.com.mobifone.mtracker.common.VMSXmlParser.Entry;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

public class VersionChecker extends Activity {
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		if (Session.isLaunched()){
			//do nothing when application already launched.
			Intent intent = new Intent(VersionChecker.this, SplashActivity.class);
			startActivity(intent);
			VersionChecker.this.finish();
			
		} else {
			//app not launched yet, we will check:
			//	- version number is latest
			//	- whether we have an announcement message from VMS
			
			if (isNetworkAvaiable(getApplicationContext())){
	    		// Network OK, download xml then parsing
	            new DownloadXmlTask().execute(VMSConstants.VERSION_URL);
	    	} else {
	    		// Not available network service, display error dialog and exit the app
	    		showErrorPage();
	    		//finish();
	    	}
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

    // Displays an error if the app is unable to load content.
    private void showErrorPage() {
        //setContentView(R.layout.main);//should be added dynamically 
        
        String msg = getResources().getString(R.string.connection_error);
        //checkUpdateDialog(this, msg);
        AlertDialog.Builder builder = new AlertDialog.Builder(VersionChecker.this) ;
		
		// Setting Dialog Title
		builder.setTitle("ERROR");
		// Setting Dialog Message
		builder.setMessage(msg);
		
		AlertDialog alertDialog = builder.create();
		
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Write your code here to execute after dialog closed
				VersionChecker.this.finish();
		    }
		});
		
		alertDialog.show();
        return;
    }
    
    /**
     * calling the splash activity
     */
    private void callSplash(){
    	Intent intent = new Intent(VersionChecker.this, SplashActivity.class);
		startActivity(intent);
		VersionChecker.this.finish();
    }
    
    /**
     * Check current version, announcement message
     * @param act
     * @param cv
     */
    private void checkVersion(Activity act, Map<String,String> cv){
		AlertDialog.Builder builder = new AlertDialog.Builder(VersionChecker.this) ;
		
		//Session.setAppInfo(Session.getAppInfo() + cv.get("version"));
		Session.setAppInfo(String.format(Session.getAppInfo(), cv.get("version")));
		
		// Check whether we current have the latest version:
		if (VMSConstants.APK_VERSION.equalsIgnoreCase(cv.get("version"))){
			
			if (cv.get("message") == null 
				|| "".equals(cv.get("message"))){
				// Check whether we have an announcement from VMS:
				//	No message: just go through to the Splash activity
				callSplash();
			} else {
				// We have a valid message from VMS, now display in the popup dialog
				// Display announcement message from VMS side
				builder.setTitle("INFORMATION");
				builder.setMessage(cv.get("message"));
				
				AlertDialog alertDialog = builder.create();
				
				alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK" , new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						callSplash();
				    }
				});
				
				alertDialog.show();	
			}	
			
		} else {
			// AUTO DOWNLOAD and INSTALLATION
			// We need to run auto update process (auto download then auto install)
			startActivity(new Intent(VersionChecker.this, AutoUpdate.class));
			VersionChecker.this.finish();
		}
    }

    // perform download version.xml task    
    private class DownloadXmlTask extends AsyncTask<String, Void, Map<String,String>> {

        @Override
        protected Map<String,String> doInBackground(String... urls) {
        	
        	Map<String,String> cv = new HashMap<String,String>();
        	try {
            	cv = loadXmlFromNetwork(urls[0]);
                
            } catch (IOException e) {
            	cv.put("version", "");
            	cv.put("result", getResources().getString(R.string.connection_error));
            } catch (XmlPullParserException e) {
                cv.put("version", "");
            	cv.put("result", getResources().getString(R.string.xml_error));
            }
            
            return cv;
        }

        @Override
        protected void onPostExecute(Map<String,String> cv) {
            //setContentView(R.layout.main);//should add dynamically.
            checkVersion(VersionChecker.this, cv);
            getServerConfig(VersionChecker.this, cv);
        }
    }

    /**
     * Get configuration params from VMS server, and set to Application wide preference.
     * @param act
     * @param cv
     */
    private void getServerConfig(Activity act, Map<String,String> cv){
    	boolean preferCell = "true".equals(cv.get("preferCellTowner"))? true : false;
        boolean keepGps = "true".equals(cv.get("keepFixGPS"))? true : false;
        
        int accuracy = 0;
    	int timeIntever = 0;
        try {
        	accuracy = Integer.parseInt(cv.get("accuracyBeforeLogging"));
        	timeIntever = Integer.parseInt(cv.get("timeIntervalForAccuracy"));
        } catch (Exception e){
        	e.printStackTrace();
        }
        
        AppSettings.setPreferCellTower(preferCell);
        AppSettings.setKeepFix(keepGps);
        AppSettings.setMinimumAccuracyInMeters(accuracy);
        AppSettings.setRetryInterval(timeIntever);
    }
    
    private Map<String,String> loadXmlFromNetwork(String urlString) 
    		throws XmlPullParserException, IOException {
        InputStream stream = null;
        VMSXmlParser vmsParser = new VMSXmlParser();
        Entry entry = null;
        Map<String,String> cv = new HashMap<String,String>();
        
        try {
            stream = downloadUrl(urlString);
            entry = vmsParser.parse(stream);
        // Makes sure that the InputStream is closed after the app is
        // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        
        if (entry == null){
        	return cv;
        }
        
        String versionInfo = 	"Version:" + entry.version + "\n" 
        						+ "Message:" + entry.message + "\n" 
        						+ "Event:" + entry.event;        
        // Version and Annoucement message from VSM
        cv.put("version", entry.version);
        cv.put("message", entry.message);
        cv.put("result", versionInfo);
        
        // Configuration params from VMS server
        cv.put("preferCellTowner", entry.preferCellTowner);
        cv.put("keepFixGPS", entry.keepFixGPS);
        cv.put("accuracyBeforeLogging", entry.accuracyBeforeLogging);
        cv.put("timeIntervalForAccuracy", entry.timeIntervalForAccuracy);
        
        return cv;
    }
    
    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }
    
    
    /**
     * check whether we have a valid network connection
     * @return
     */
    private boolean isNetworkAvaiable(Context context){
    	boolean result = false;
    	ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	
        if (networkInfo != null 
        		&& networkInfo.isConnected()){
        	result = true;
        } else {
        	result = false;
        }
        
    	return result;
    }
	
}
