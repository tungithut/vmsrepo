package vn.com.mobifone.mtracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity {

	private boolean networkError = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Session.isLaunched()){
        	// Already launch, by pass to enter to Main Activity
        	startActivity(new Intent(SplashActivity.this, VMSMainActivity.class));
            SplashActivity.this.finish();
            
        } else {
        	// version is ok, go ahead
	        // Not yet launched, now launch the animation
	        setContentView(R.layout.splash);
	        TextView tv = (TextView) findViewById(R.id.appInfo);
	        tv.setText(Session.getAppInfo());
	        
	        startAnimating();
	        // Set the state of launching to TRUE
	        Session.setLaunched(true);
        }
    }

    /**
     * Helper method to start the animation on the splash screen
     */
    private void startAnimating() {
        // Fade in top title
        TextView logo1 = (TextView) findViewById(R.id.appTitle1);
        Animation fade1 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo1.startAnimation(fade1);
        
        // Fade in bottom title after a built-in delay.
        TextView logo2 = (TextView) findViewById(R.id.appTitle2);
        Animation fade2 = AnimationUtils.loadAnimation(this, R.anim.fade_in2);
        logo2.startAnimation(fade2);
        
        // Transition to Main Menu when bottom title finishes animating
        fade2.setAnimationListener(new AnimationListener() {
        	
            public void onAnimationEnd(Animation animation) {
                // The animation has ended, transition to the Main Menu screen
                startActivity(new Intent(SplashActivity.this, VMSMainActivity.class));
                SplashActivity.this.finish();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        
        // Load animations for all views within the TableLayout
        Animation spinin = AnimationUtils.loadAnimation(this, R.anim.custom_anim);
        LayoutAnimationController controller = new LayoutAnimationController(spinin);
        
        ImageView imgCenter = (ImageView) findViewById(R.id.imageView1);
        imgCenter.setAnimation(spinin);
        
    }

    @Override
    protected void onPause() {
        super.onPause();
	        // Stop the animation
	        TextView logo1 = (TextView) findViewById(R.id.appTitle1);
	        logo1.clearAnimation();
	        TextView logo2 = (TextView) findViewById(R.id.appTitle2);
	        logo2.clearAnimation();
	        
	        ImageView imgCenter = (ImageView) findViewById(R.id.imageView1);
	        imgCenter.clearAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start animating at the beginning so we get the full splash screen experience
        startAnimating();
    }
    
    /** NEW ADD 02.jul.2013 **/
    /*private boolean checkAutoUpdateAndInstall(){
        boolean exit = false;
    	// check network availability and perform xml download
    	if (isNetworkAvaiable(getApplicationContext())){
    		// AsyncTask subclass
            new DownloadXmlTask().execute(VMSConstants.VERSION_URL);
    	} else {
    		showErrorPage();
    		exit = true;
    	}
    	return exit;
    }*/

    // Displays an error if the app is unable to load content.
    /*private void showErrorPage() {
        //setContentView(R.layout.main);//should be added dynamically 
        
        String msg = getResources().getString(R.string.connection_error);
        //checkUpdateDialog(this, msg);
        AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this) ;
		
		// Setting Dialog Title
		builder.setTitle("ERROR");
		// Setting Dialog Message
		builder.setMessage(msg);
		
		AlertDialog alertDialog = builder.create();
		
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Write your code here to execute after dialog closed
				SplashActivity.this.finish();
		    }
		});
		
		alertDialog.show();
        return;
    }*/
    
    /*private void checkUpdateDialog(Activity act, Map<String,String> cv){
		AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this) ;
		
		// Check whether we current have the latest version:
		if (VMSConstants.APK_VERSION.equalsIgnoreCase(cv.get("version"))){
			// Display announcement message from VMS side
			builder.setTitle("Changelog");
			builder.setMessage(cv.get("result"));
			
			AlertDialog alertDialog = builder.create();
			
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Write your code here to execute after dialog closed
					//Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
			    }
			});
			
			alertDialog.show();	
			
		} else {
			// AUTO DOWNLOAD and INSTALLATION
			// We need to run auto update process (auto download then auto install)
			startActivity(new Intent(SplashActivity.this, AutoUpdate.class));
			SplashActivity.this.finish();
		}
    }*/

    // perform download version.xml task    
    /*private class DownloadXmlTask extends AsyncTask<String, Void, Map<String,String>> {

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
            checkUpdateDialog(SplashActivity.this, cv);
        }
    }*/

    
    /*private Map<String,String> loadXmlFromNetwork(String urlString) 
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
        cv.put("version", entry.version);
        cv.put("result", versionInfo);
        return cv;
    }*/
    
    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    /*private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000  milliseconds );
        conn.setConnectTimeout(15000  milliseconds );
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }*/
    
    
    /**
     * check whether we have a valid network connection
     * @return
     */
    /*private boolean isNetworkAvaiable(Context context){
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
    }*/
}
