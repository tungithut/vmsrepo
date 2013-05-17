
package vn.com.mobifone.mtracker;

import android.content.Context;
import android.location.*;
import android.os.Bundle;

import java.util.Iterator;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener {

	private static VMSLoggingService loggingService;

	GeneralLocationListener(VMSLoggingService activity) {
		// Utilities.LogDebug("GeneralLocationListener constructor");
		loggingService = activity;
	}

	private void makeUseOfNewLocation(Location location) {

	}

	/**
	 * This class implement location listener, when event raised when a new fix
	 * is received, this function is called, then we should transfer processing
	 * to the main location service.
	 */
	public void onLocationChanged(Location loc) {

		try {
			if (loc != null) {
				// Utilities.LogVerbose("GeneralLocationListener.onLocationChanged");
				loggingService.OnLocationChanged(loc);
			}

		} catch (Exception ex) {
			// Utilities.LogError("GeneralLocationListener.onLocationChanged",
			// ex);
			// loggingService.SetStatus(ex.getMessage());
			ex.printStackTrace();
		}

	}

	public void onProviderDisabled(String provider) {
		// Utilities.LogInfo("Provider disabled");
		// Utilities.LogDebug(provider);
		loggingService.RestartGpsManagers();
	}

	public void onProviderEnabled(String provider) {

		// Utilities.LogInfo("Provider enabled");
		// Utilities.LogDebug(provider);
		loggingService.RestartGpsManagers();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == LocationProvider.OUT_OF_SERVICE) {
			// Utilities.LogDebug(provider + " is out of service");
			loggingService.StopManagerAndResetAlarm();
		}

		if (status == LocationProvider.AVAILABLE) {
			// Utilities.LogDebug(provider + " is available");
		}

		if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			// Utilities.LogDebug(provider + " is temporarily unavailable");
			loggingService.StopManagerAndResetAlarm();
		}
	}

	public void onGpsStatusChanged(int event) {

		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			// Utilities.LogDebug("GPS Event First Fix");
			// loggingService.SetStatus(loggingService.getString(R.string.fix_obtained));
			break;

		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

			// Utilities.LogDebug("GPS Satellite status obtained");
			GpsStatus status = loggingService.gpsLocationManager
					.getGpsStatus(null);

			int maxSatellites = status.getMaxSatellites();

			Iterator<GpsSatellite> it = status.getSatellites().iterator();
			int count = 0;

			while (it.hasNext() && count <= maxSatellites) {
				it.next();
				count++;
			}

			// loggingService.SetSatelliteInfo(count);
			break;

		case GpsStatus.GPS_EVENT_STARTED:
			// Utilities.LogInfo("GPS started, waiting for fix");
			// loggingService.SetStatus(loggingService.getString(R.string.started_waiting));
			break;

		case GpsStatus.GPS_EVENT_STOPPED:
			// Utilities.LogInfo("GPS Stopped");
			// loggingService.SetStatus(loggingService.getString(R.string.gps_stopped));
			break;

		}
	}

}
