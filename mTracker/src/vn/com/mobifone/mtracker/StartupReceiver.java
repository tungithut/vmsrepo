

package vn.com.mobifone.mtracker;



import vn.com.mobifone.mtracker.common.Utilities;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class StartupReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean startImmediately = prefs.getBoolean("startonbootup", false);
            

        	//tnt.The system should automatic start the logging 
        	// service immediately right after app was launched.
        	
        	//boolean startImmediately = true;
        	
            /*Utilities.LogInfo("Did the user ask for start on bootup? - "
                    + String.valueOf(startImmediately));*/
        	Utilities.LogDebug("Automatic start the logging service");

            if (startImmediately)
            {
                Utilities.LogInfo("Launching VMSLocationService");
                Intent serviceIntent = new Intent(context, VMSLoggingService.class);
                serviceIntent.putExtra("immediate", true);
                context.startService(serviceIntent);
            }
        }
        catch (Exception ex)
        {
            Utilities.LogError("StartupReceiver", ex);

        }

    }

}
