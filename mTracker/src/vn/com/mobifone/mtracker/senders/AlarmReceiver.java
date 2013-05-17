

package vn.com.mobifone.mtracker.senders;

import vn.com.mobifone.mtracker.common.Utilities;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class AlarmReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            Utilities.LogInfo("Autosend event alarm received");
            Intent serviceIntent = new Intent(context.getPackageName() + ".VMSLoggingService");
            serviceIntent.putExtra("emailAlarm", true);
            // Start the service in case it isn't already running
            context.startService(serviceIntent);
        }
        catch (Exception ex)
        {
             Utilities.LogError("AlarmReceiver.onReceive", ex);
        }


    }
}
