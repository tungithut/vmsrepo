/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package vn.com.mobifone.mtracker;

import vn.com.mobifone.mtracker.common.Utilities;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.MenuItem;

public class VMSSettingsActivity extends PreferenceActivity
{

    private final Handler handler = new Handler();
    private SharedPreferences prefs;

    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable the home button so you can go back to the main screen
        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.settings);

        if (getIntent().getBooleanExtra("autosend_preferencescreen", false))
        {
            PreferenceScreen screen = (PreferenceScreen) findPreference("gpslogger_preferences");
            int pos = findPreference("autosend_preferencescreen").getOrder();
            screen.onItemClick(null, null, pos, 0);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //boolean useImperial = prefs.getBoolean("useImperial", false);

        /*EditTextPreference distanceBeforeLogging = 
        		(EditTextPreference) findPreference("distance_before_logging");
        EditTextPreference accuracyBeforeLogging = 
        		(EditTextPreference) findPreference("accuracy_before_logging");
*/
        Preference enableDisablePref = findPreference("enableDisableGps");
        enableDisablePref.setOnPreferenceClickListener(new AndroidLocationPreferenceClickListener());
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
            case android.R.id.home:
                Intent intent = new Intent(this, VMSMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }



/*    private final Runnable updateResults = new Runnable()
    {
        public void run()
        {
            finish();

            startActivity(getIntent());
        }

    };
*/

    /**
     * Opens the Android Location preferences screen
     */
    private class AndroidLocationPreferenceClickListener implements OnPreferenceClickListener
    {
        public boolean onPreferenceClick(Preference preference)
        {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }
    }
    

    /*private class ImperialPreferenceChangeListener implements Preference.OnPreferenceChangeListener
    {
        EditTextPreference distanceBeforeLogging;
        EditTextPreference accuracyBeforeLogging;
        SharedPreferences prefs;

        public ImperialPreferenceChangeListener(SharedPreferences prefs, EditTextPreference distanceBeforeLogging, EditTextPreference accuracyBeforeLogging)
        {
            this.prefs = prefs;
            this.distanceBeforeLogging = accuracyBeforeLogging;
            this.accuracyBeforeLogging = accuracyBeforeLogging;
        }

        public boolean onPreferenceChange(Preference preference, final Object newValue)
        {

            Utilities.ShowProgress(VMSSettingsActivity.this, getString(R.string.settings_converting_title),
                    getString(R.string.settings_converting_description));
             
        	
            new Thread()
            {

                public void run()
                {

                    try
                    {
                        sleep(3000); // Give user time to read the message
                    }
                    catch (InterruptedException e)
                    {

                        Log.e("Settings", e.getMessage());

                    }

                    boolean useImp = Boolean.parseBoolean(newValue.toString());

                    String minimumDistanceString = prefs.getString("distance_before_logging", "0");
                    String minimumAccuracyString = prefs.getString("accuracy_before_logging", "0");
                    
                    int minimumDistance;

                    if (minimumDistanceString != null && minimumDistanceString.length() > 0)
                    {
                        minimumDistance = Integer.valueOf(minimumDistanceString);
                    }
                    else
                    {
                        minimumDistance = 0;
                    }
                    
                    int minimumAccuracy;

                    if (minimumAccuracyString != null && minimumAccuracyString.length() > 0)
                    {
                        minimumAccuracy = Integer.valueOf(minimumAccuracyString);
                    }
                    else
                    {
                        minimumAccuracy = 0;
                    }

                    SharedPreferences.Editor editor = prefs.edit();

                    if (useImp)
                    {
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_feet);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        //minimumDistance = Utilities.MetersToFeet(minimumDistance);
                        
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_feet);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        //minimumAccuracy = Utilities.MetersToFeet(minimumAccuracy);
                    }
                    else
                    {
                        //minimumDistance = Utilities.FeetToMeters(minimumDistance);
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_meters);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                                                    
                        //minimumAccuracy = Utilities.FeetToMeters(minimumAccuracy);
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_meters);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                        

                    }

                    if (minimumDistance >= 9999)
                    {
                        minimumDistance = 9999;
                    }
                    
                    if (minimumAccuracy >= 9999)
                    {
                        minimumAccuracy = 9999;
                    }

                    editor.putString("distance_before_logging", String.valueOf(minimumDistance));
                    
                    editor.putString("accuracy_before_logging", String.valueOf(minimumAccuracy));
                    editor.commit();

                    handler.post(updateResults);
                    //Utilities.HideProgress();
                }
            }.start();

            return true;
        }

    }*/

    /**
     * Opens the OpenGTS preferences
     * Listener to ensure that the server is configured when the user wants to enable OpenGTS logging logger
     */
/*    private class LogOpenGTSPreferenceClickListener implements OnPreferenceClickListener
    {
        private SharedPreferences prefs;

        public LogOpenGTSPreferenceClickListener(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        public boolean onPreferenceClick(Preference preference)
        {
            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
            boolean opengts_enabled = prefs.getBoolean("opengts_enabled", false);

            if (chkLog_opengts.isChecked() && !opengts_enabled)
            {
                startActivity(new Intent("com.mendhak.gpslogger.OPENGTS_SETUP"));
            }
            return true;
        }
    }
*/
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        Utilities.LogDebug("VMSSettingsActivity.onWindowFocusChanged");
/*        if (hasFocus)
        {

            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
            boolean opengts_enabled = prefs.getBoolean("opengts_enabled", false);

            if (chkLog_opengts.isChecked() && !opengts_enabled)
            {
                chkLog_opengts.setChecked(false);
            }

        }
*/    }


}
