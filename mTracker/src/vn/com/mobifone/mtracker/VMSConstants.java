/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.com.mobifone.mtracker;

import android.app.AlarmManager;

public class VMSConstants {
  
  // APK version number (incresement each deployment)
  public static String APK_VERSION = "1.2.1";
  
  // Auto-update configuration
  public static String DEST_FILE_PATH 	 = "mtracker.apk";
  public static String VERSION_URL = 	"http://partner.mobifone.com.vn/gsht/mtracker.version.xml";
  public static String APK_URL = 		"http://partner.mobifone.com.vn/gsht/mtracker.apk";
  public static String APP_HOME = "/mtracker";
  public static String HELP_URL = "http://partner.mobifone.com.vn/gsht/hdsd.htm";
  
  // TODO Turn off when deploying your app.
  public static boolean DEVELOPER_MODE = true;
  
  // The default search radius when searching for places nearby.
  public static int DEFAULT_RADIUS = 150;
  // The maximum distance the user should travel between location updates. 
  public static int MAX_DISTANCE = DEFAULT_RADIUS/2;
  // The maximum time that should pass before the user gets a location update.
  public static long MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
 
  
}
