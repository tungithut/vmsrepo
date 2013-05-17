package vn.com.mobifone.mtracker.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utilities {

	private static final int LOGLEVEL = 5;
	

	public static void LogInfo(String message) {
		if (LOGLEVEL >= 3) {
			Log.i("mTracker", message);
		}

	}

	public static void LogError(String methodName, Exception ex) {
		try {
			LogError(methodName + ":" + ex.getMessage());
		} catch (Exception e) {
			/**/
		}
	}

	private static void LogError(String message) {
		Log.e("mTracker", message);

	}

	@SuppressWarnings("unused")
	public static void LogDebug(String message) {
		if (LOGLEVEL >= 4) {
			Log.d("mTracker", message);
		}

	}

	public static void LogWarning(String message) {
		if (LOGLEVEL >= 2) {
			Log.w("mTracker", message);
		}

	}

	@SuppressWarnings("unused")
	public static void LogVerbose(String message) {
		if (LOGLEVEL >= 5) {
			Log.v("mTracker", message);
		}

	}

	/**
	 * Given a Date object, returns an ISO 8601 date time string in UTC.
	 * Example: 2010-03-23T05:17:22Z but not 2010-03-23T05:17:22+04:00
	 * 
	 * @param dateToFormat
	 *            The Date object to format.
	 * @return The ISO 8601 formatted string.
	 */
	public static String GetIsoDateTime(Date dateToFormat) {
		// GPX specs say that time given should be in UTC, no local time.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(dateToFormat);
	}

	public static String GetReadableDateTime(Date dateToFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
		return sdf.format(dateToFormat);
	}

	/**
	 * Uses the Haversine formula to calculate the distnace between to lat-long
	 * coordinates
	 * 
	 * @param latitude1
	 *            The first point's latitude
	 * @param longitude1
	 *            The first point's longitude
	 * @param latitude2
	 *            The second point's latitude
	 * @param longitude2
	 *            The second point's longitude
	 * @return The distance between the two points in meters
	 */
	public static double CalculateDistance(double latitude1, double longitude1,
			double latitude2, double longitude2) {
		/*
		 * Haversine formula: A = sin²(Δlat/2) +
		 * cos(lat1).cos(lat2).sin²(Δlong/2) C = 2.atan2(√a, √(1−a)) D =
		 * R.c R = radius of earth, 6371 km. All angles are in radians
		 */

		double deltaLatitude = Math.toRadians(Math.abs(latitude1 - latitude2));
		double deltaLongitude = Math.toRadians(Math
				.abs(longitude1 - longitude2));
		double latitude1Rad = Math.toRadians(latitude1);
		double latitude2Rad = Math.toRadians(latitude2);

		double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
				+ (Math.cos(latitude1Rad) * Math.cos(latitude2Rad) * Math.pow(
						Math.sin(deltaLongitude / 2), 2));

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return 6371 * c * 1000; // Distance in meters

	}

	/**
	 * Checks if a string is null or empty
	 * 
	 * @param text
	 * @return
	 */
	public static boolean IsNullOrEmpty(String text) {
		return text == null || text.length() == 0;
	}

	public static byte[] GetByteArrayFromInputStream(InputStream is) {

		try {
			int length;
			int size = 1024;
			byte[] buffer;

			if (is instanceof ByteArrayInputStream) {
				size = is.available();
				buffer = new byte[size];
				is.read(buffer, 0, size);
			} else {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				buffer = new byte[size];
				while ((length = is.read(buffer, 0, size)) != -1) {
					outputStream.write(buffer, 0, length);
				}

				buffer = outputStream.toByteArray();
			}
			return buffer;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				Utilities
						.LogWarning("GetStringFromInputStream - could not close stream");
			}
		}

		return null;

	}

	/**
	 * Loops through an input stream and converts it into a string, then closes
	 * the input stream
	 * 
	 * @param is
	 * @return
	 */
	public static String GetStringFromInputStream(InputStream is) {
		String line;
		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		try {
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				Utilities
						.LogWarning("GetStringFromInputStream - could not close stream");
			}
		}

		// Return full string
		return total.toString();
	}

	/**
	 * Converts an input stream containing an XML response into an XML Document
	 * object
	 * 
	 * @param stream
	 * @return
	 */
	public static Document GetDocumentFromInputStream(InputStream stream) {
		Document doc;

		try {
			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory
					.newInstance();
			xmlFactory.setNamespaceAware(true);
			DocumentBuilder builder = xmlFactory.newDocumentBuilder();
			doc = builder.parse(stream);
		} catch (Exception e) {
			doc = null;
		}

		return doc;
	}

	/**
	 * Gets the mTracker-specific MIME type to use for a given
	 * filename/extension
	 * 
	 * @param fileName
	 * @return
	 */
	public static String GetMimeTypeFromFileName(String fileName) {

		if (fileName == null || fileName.length() == 0) {
			return "";
		}

		int pos = fileName.lastIndexOf(".");
		if (pos == -1) {
			return "application/octet-stream";
		} else {

			String extension = fileName.substring(pos + 1, fileName.length());

			if (extension.equalsIgnoreCase("gpx")) {
				return "application/gpx+xml";
			} else if (extension.equalsIgnoreCase("kml")) {
				return "application/vnd.google-earth.kml+xml";
			} else if (extension.equalsIgnoreCase("zip")) {
				return "application/zip";
			}
		}

		// Unknown mime type
		return "application/octet-stream";

	}
	
	/**
     * Gets user preferences, populates the AppSettings class.
     */
    public static void PopulateAppSettings(Context context)
    {

        Utilities.LogInfo("Getting preferences");
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        /*AppSettings.setUseImperial(prefs.getBoolean("useImperial", false));
        AppSettings.setLogToKml(prefs.getBoolean("log_kml", false));
        AppSettings.setLogToGpx(prefs.getBoolean("log_gpx", false));
        AppSettings.setLogToPlainText(prefs.getBoolean("log_plain_text", false));
        AppSettings.setLogToOpenGTS(prefs.getBoolean("log_opengts", false));
         */
        AppSettings.setShowInNotificationBar(prefs.getBoolean(
                "show_notification", true));

        AppSettings.setPreferCellTower(prefs.getBoolean("prefer_celltower",
                false));


        String minimumDistanceString = prefs.getString(
                "distance_before_logging", "0");

        if (minimumDistanceString != null && minimumDistanceString.length() > 0)
        {
            AppSettings.setMinimumDistanceInMeters(Integer
                    .valueOf(minimumDistanceString));
        }
        else
        {
            AppSettings.setMinimumDistanceInMeters(0);
        }

        String minimumAccuracyString = prefs.getString(
                "accuracy_before_logging", "0");

        if (minimumAccuracyString != null && minimumAccuracyString.length() > 0)
        {
            AppSettings.setMinimumAccuracyInMeters(Integer
                    .valueOf(minimumAccuracyString));
        }
        else
        {
            AppSettings.setMinimumAccuracyInMeters(0);
        }

        /*if (AppSettings.shouldUseImperial())
        {
            AppSettings.setMinimumDistanceInMeters(Utilities.FeetToMeters(AppSettings
                    .getMinimumDistanceInMeters()));

            AppSettings.setMinimumAccuracyInMeters(Utilities.FeetToMeters(AppSettings
                    .getMinimumAccuracyInMeters()));
        }*/


        String minimumSecondsString = prefs.getString("time_before_logging",
                "60");

        if (minimumSecondsString != null && minimumSecondsString.length() > 0)
        {
            AppSettings
                    .setMinimumSeconds(Integer.valueOf(minimumSecondsString));
        }
        else
        {
            AppSettings.setMinimumSeconds(60);
        }

        AppSettings.setKeepFix(prefs.getBoolean("keep_fix",
                false));

        String retryIntervalString = prefs.getString("retry_time",
                "60");

        if (retryIntervalString != null && retryIntervalString.length() > 0)
        {
            AppSettings
                    .setRetryInterval(Integer.valueOf(retryIntervalString));
        }
        else
        {
             AppSettings.setRetryInterval(60);
        }

        /*AppSettings.setNewFileCreation(prefs.getString("new_file_creation",
                "onceaday"));

        if (AppSettings.getNewFileCreation().equals("onceaday"))
        {
            AppSettings.setNewFileOnceADay(true);
            AppSettings.setStaticFile(false);
        }
        else if(AppSettings.getNewFileCreation().equals("static"))
        {
            AppSettings.setStaticFile(true);
            AppSettings.setStaticFileName(prefs.getString("new_file_static_name","gpslogger"));
        }
        else
        {
            AppSettings.setNewFileOnceADay(false);
            AppSettings.setStaticFile(false);
        }


        AppSettings.setAutoEmailEnabled(prefs.getBoolean("autoemail_enabled",
                false));*/

        AppSettings.setAutoSendEnabled(prefs.getBoolean("autosend_enabled", true));//always auto send after %AutoDelayTime%

        if (Float.valueOf(prefs.getString("autosend_frequency", "0")) >= 8f)
        {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("autosend_frequency", "8");
            editor.commit();
        }
        
        AppSettings.setAutoSendDelay(Float.valueOf(prefs.getString(
                "autosend_frequency", "0.01667f")));//default:0.01667f == 1 min.

        /*

        AppSettings.setSmtpServer(prefs.getString("smtp_server", ""));
        AppSettings.setSmtpPort(prefs.getString("smtp_port", "25"));
        AppSettings.setSmtpSsl(prefs.getBoolean("smtp_ssl", true));
        AppSettings.setSmtpUsername(prefs.getString("smtp_username", ""));
        AppSettings.setSmtpPassword(prefs.getString("smtp_password", ""));
        AppSettings.setAutoEmailTargets(prefs.getString("autoemail_target", ""));
        AppSettings.setDebugToFile(prefs.getBoolean("debugtofile", false));
        AppSettings.setShouldSendZipFile(prefs.getBoolean("autosend_sendzip", true));
        AppSettings.setSmtpFrom(prefs.getString("smtp_from", ""));
        AppSettings.setOpenGTSEnabled(prefs.getBoolean("opengts_enabled", false));
        AppSettings.setAutoOpenGTSEnabled(prefs.getBoolean("autoopengts_enabled", false));*/
        
        AppSettings.setVmsServer(prefs.getString("vms_server", ""));
        AppSettings.setVmsServerPort(prefs.getString("vms_server_port", ""));
        AppSettings.setVmsServerPath(prefs.getString("vms_server_path", ""));
        
        
        /*AppSettings.setOpenGTSDeviceId(prefs.getString("opengts_device_id", ""));
        AppSettings.setOpenGTSServerCommunicationMethod(prefs.getString("opengts_server_communication_method", ""));
        
        AppSettings.setAutoFtpEnabled(prefs.getBoolean("autoftp_enabled",false));
        AppSettings.setFtpServerName(prefs.getString("autoftp_server",""));
        AppSettings.setFtpUsername(prefs.getString("autoftp_username",""));
        AppSettings.setFtpPassword(prefs.getString("autoftp_password",""));
        AppSettings.setFtpPort(Integer.valueOf(prefs.getString("autoftp_port", "21")));
        AppSettings.setFtpUseFtps(prefs.getBoolean("autoftp_useftps", false));
        AppSettings.setFtpProtocol(prefs.getString("autoftp_ssltls",""));
        AppSettings.setFtpImplicit(prefs.getBoolean("autoftp_implicit", false));*/

    }

}
