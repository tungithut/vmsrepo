package vn.com.mobifone.mtracker.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import vn.com.mobifone.mtracker.common.Session;
import vn.com.mobifone.mtracker.common.Utilities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.provider.BaseColumns;

public class DatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "db.mtracker";
	private Context mContext;

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Routes.CREATE_STATEMENT);
		db.execSQL(Waypoints.CREATE_STATEMENT);
	}

	/**
	 * Creates a waypoint under the current track segment with the current time
	 * on which the waypoint is reached
	 * 
	 * @param track
	 *            track
	 * @param latitude
	 *            latitude
	 * @param longitude
	 *            longitude
	 * @param time
	 *            time
	 * @param speed
	 *            the measured speed
	 * @param loc_status the location status ('start' || 'stop' || empty)
	 * @param checkin_status
	 *            : checkin status (1: checkin; else: non-checkin)
	 *            @param sent_status: 1 OK, 0 fail.
	 * @return
	 */
	public long insertWaypoint(Location location, ContentValues cv) {

		int checkin_status = 0;
		long routeId = 0;
		
		try {
			checkin_status = Integer.parseInt(cv.getAsString("checkin_status"));
			routeId = Long.parseLong(cv.getAsString("routeId"));
			
		} catch (Exception e)  {
			e.printStackTrace();
		}
		
		if (isDuplicateKey(location.getTime(), cv.getAsString("loc_status"), checkin_status)){
			//this location already logged, no need to insert again.
			return 0;
		}
		
		SQLiteDatabase sqldb = getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(WaypointsColumns.IMEI_COL, cv.getAsString("imei"));
		args.put(WaypointsColumns.TIME_COL, location.getTime());
		args.put(WaypointsColumns.LATITUDE_COL, location.getLatitude());
		args.put(WaypointsColumns.LONGITUDE_COL, location.getLongitude());
		args.put(WaypointsColumns.SPEED_COL, location.getSpeed());
		args.put(WaypointsColumns.ACCURACY_COL, location.getAccuracy());
		args.put(WaypointsColumns.ALTITUDE_COL, location.getAltitude());
		args.put(WaypointsColumns.BEARING_COL, location.getBearing());
		args.put(WaypointsColumns.CHECKIN_STATUS_COL, checkin_status);
		
		// New implement:
		args.put(WaypointsColumns.ROUTE_ID_COL, routeId);
		
		if (cv.getAsInteger("sent_status") != null){
			args.put(WaypointsColumns.SENT_STATUS_COL, cv.getAsInteger("sent_status"));
		} else {
			args.put(WaypointsColumns.SENT_STATUS_COL, 0);//not sent yet; this time is logged into DB only.
		}
		
		args.put(WaypointsColumns.LOC_STATUS_COL, cv.getAsString("loc_status") );
		
		long waypointId = sqldb.insert(Waypoints.TABLE, null, args);
		
		if (waypointId == -1){
			Utilities.LogError("insertWaypoint.error while insert" , new Exception(""));
		}
		
		sqldb.close();
		
		return waypointId;
	}
	
	/**
	 * Update a waypoint, its sent_status would be change when it successfully sent out.
	 * 
	 * @param track
	 *            track
	 * @param latitude
	 *            latitude
	 * @param longitude
	 *            longitude
	 * @param time
	 *            time
	 * @param speed
	 *            the measured speed
	 * @param loc_status the location status ('start' || 'stop' || empty)
	 * @param checkin_status
	 *            : checkin status (1: checkin; else: non-checkin)
	 *            @param sent_status: 1 OK, 0 fail.
	 * @return
	 */
	public void updateWaypointSent(Location location, int sent_status) {

		SQLiteDatabase sqldb = getWritableDatabase();

		ContentValues args = new ContentValues();
		
		args.put(WaypointsColumns.SENT_STATUS_COL, sent_status);
		
		String whereClause = " time = " + String.valueOf(location.getTime());
		
		int rowAffected = sqldb.update(Waypoints.TABLE, args, whereClause, null);
		
		sqldb.close();
	}
	
	public void updateWaypointSent(Waypoints point, int sent_status) {

		SQLiteDatabase sqldb = getWritableDatabase();

		ContentValues args = new ContentValues();
		
		args.put(WaypointsColumns.SENT_STATUS_COL, sent_status);
		
		String whereClause = " time == " + String.valueOf(point.getTime())
							+ " AND loc_status == " + "\"" + point.getLoc_status() + "\""
							+ " AND checkin_status == " + String.valueOf(point.getCheckin_status());
		
		int rowAffected = sqldb.update(Waypoints.TABLE, args, whereClause, null);
		
		sqldb.close();
	}
	
	/**
	 * there 3 colums make the primary key in the table.
	 * this function is to check whether the newly get location data duplicated 
	 * 	with the already saved data
	 * @param time
	 * @param locStatus
	 * @param checkin_status
	 * @return
	 */
	public boolean isDuplicateKey(long time, String locStatus, int checkin_status){
		boolean result = false;
		String sql = "Select * FROM " + Waypoints.TABLE 
						+ " WHERE time == " + String.valueOf(time)
						+ " AND loc_status == " + "\"" + locStatus + "\"" 
						+ " AND checkin_status == " + String.valueOf(checkin_status);
		
		SQLiteDatabase sqldb = getWritableDatabase();
		Cursor cursor = sqldb.rawQuery(sql, null);
		try {
			result = (cursor.getCount() == 0? false : true);
		} finally {
			cursor.close();
			sqldb.close();
		}
		return result;
	}
	
	/**
	 * Test whether a given location found is already logged in the DB 
	 * @param loc
	 * @return true if a preivously logged location; false if not.
	 */
	public boolean isLoggedLocation(Location loc){
		boolean result = false;
		String sql = "Select * FROM " + Waypoints.TABLE 
						+ " WHERE time == " + String.valueOf(loc.getTime());
						 
		SQLiteDatabase sqldb = getWritableDatabase();
		Cursor cursor = sqldb.rawQuery(sql, null);
		try {
			result = (cursor.getCount() == 0? false : true);
		} finally {
			cursor.close();
			sqldb.close();
		}
		return result;
	}	
	

	/**
	 * Used in case of ugrade Database version
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + Waypoints.TABLE);

		// Create tables again
		onCreate(db);
	}

	/**
	 * This add new record to the VISIT Table when user do check-in.
	 * 
	 * @param visit
	 *            object
	 */
	/*
	 * public long addVisit(Visit visit) { SQLiteDatabase db =
	 * this.getWritableDatabase();
	 * 
	 * ContentValues values = new ContentValues(); values.put(LATTITUDE,
	 * visit.getLattitude()); // Lattitude values.put(LONGTITUDE,
	 * visit.getLongtitude()); // Longtitude //values.put(VISIT_TIME,
	 * visit.getVisitTime()); //Visit_time
	 * 
	 * // Inserting Row long retCode = (long) db.insert(TABLE_VISIT, null,
	 * values); db.close(); // Closing database connection return retCode; //-1
	 * if error; else the index of inserted record. }
	 */

	/**
	 * Get Visit record by given ID
	 * 
	 * @param id
	 * @return
	 */

	/*
	 * public Visit getVisitById(int id) { SQLiteDatabase db =
	 * this.getReadableDatabase();
	 * 
	 * Cursor cursor = db.query(TABLE_VISIT, new String[] { KEY_ID, LATTITUDE,
	 * LONGTITUDE }, KEY_ID + "=?", new String[] { String.valueOf(id) }, null,
	 * null, null, null);
	 * 
	 * if (cursor != null) cursor.moveToFirst();
	 * 
	 * Visit visit = new Visit(Integer.parseInt(cursor.getString(0)),
	 * Float.valueOf( cursor.getString(1)), Float.valueOf(cursor.getString(2)));
	 * 
	 * //return VISIT object return visit; }
	 */
	
	/**
	 * Return the total mili seconds number from 1970 to zero hour of today (using Vietnam time zone)
	 * @return
	 */
	public String getTimeFromZerohourToday(){
		
		TimeZone tz =   TimeZone.getTimeZone("Asia/Bangkok");
		Calendar cal = Calendar.getInstance(tz);
		
		// reset to today's zero hour
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);  
		
		return String.valueOf(cal.getTimeInMillis());
	}
	
	/**
	 * Get list of all waypoint with time < current time.
	 * @param sent_status true will return list of waypoint that not success updated to server. false will return today list of waypoints.
	 * @return
	 */
	public List<Waypoints> getWaypoints(boolean with_sent_status) {

		List<Waypoints> waypointList = new ArrayList<Waypoints>();

		// Select All Query
		String selectQuery = "";
		
		String today0hr = getTimeFromZerohourToday();
		//today0hr = "0";
		
		if (with_sent_status) {
			// Get all waypoints logged today && not successfully sent yet
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					//+ " WHERE TIME < " + System.currentTimeMillis()
					+ " WHERE TIME >= " + today0hr 
					+ " AND SENT_STATUS != 1"
					+ " ORDER BY TIME";
		} else {
			// Get all waypoints logged today.
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					//+ " WHERE TIME < " + System.currentTimeMillis()
					+ " WHERE TIME >= " + today0hr
					+ " ORDER BY TIME";
		}

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		try {

			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {
					Waypoints point = new Waypoints();
	
					point.setImei(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.IMEI_COL)));
	
					point.setAccuracy(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ACCURACY_COL)));
	
					point.setAltitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ALTITUDE_COL)));
	
					point.setBearing(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.BEARING_COL)));
	
					point.setCheckin_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.CHECKIN_STATUS_COL)));
	
					point.setLatitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LATITUDE_COL)));
	
					point.setLongtitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LONGITUDE_COL)));
	
					point.setSpeed(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.SPEED_COL)));
	
					point.setTime(cursor.getLong(cursor
							.getColumnIndex(WaypointsColumns.TIME_COL)));
	
					point.setSent_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.SENT_STATUS_COL)));
	
					point.setLoc_status(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.LOC_STATUS_COL)));
					
					point.setRouteId(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.ROUTE_ID_COL)));
	
					// Adding contact to list
					waypointList.add(point);
	
				} while (cursor.moveToNext());
			}
			
		} finally {
			// to assure that the SQLite does no leaked.
			cursor.close();
			db.close();
		}

		// return waypoint list
		return waypointList;
	}
	
	/**
	 * Get today's waypoints by given route id
	 * @param with_sent_status
	 * @return
	 */
	public List<Waypoints> getWaypointsByRoute(boolean with_sent_status, long routeId) {

		List<Waypoints> waypointList = new ArrayList<Waypoints>();

		// Select All Query
		String selectQuery = "";
		
		String today0hr = getTimeFromZerohourToday();
		
		if (with_sent_status) {
			// Get all waypoints logged today && not successfully sent yet
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					+ " WHERE TIME < " + System.currentTimeMillis()
					+ " AND TIME >= " + today0hr 
					+ " AND SENT_STATUS != 1"
					+ " AND ROUTE_ID == " + routeId
					+ " ORDER BY TIME";
		} else {
			// Get all waypoints logged today.
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					+ " WHERE TIME < " + System.currentTimeMillis()
					+ " AND TIME >= " + today0hr
					+ " AND ROUTE_ID == " + routeId
					+ " ORDER BY TIME";
		}

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		try {

			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {
					Waypoints point = new Waypoints();
	
					point.setImei(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.IMEI_COL)));
	
					point.setAccuracy(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ACCURACY_COL)));
	
					point.setAltitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ALTITUDE_COL)));
	
					point.setBearing(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.BEARING_COL)));
	
					point.setCheckin_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.CHECKIN_STATUS_COL)));
	
					point.setLatitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LATITUDE_COL)));
	
					point.setLongtitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LONGITUDE_COL)));
	
					point.setSpeed(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.SPEED_COL)));
	
					point.setTime(cursor.getLong(cursor
							.getColumnIndex(WaypointsColumns.TIME_COL)));
	
					point.setSent_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.SENT_STATUS_COL)));
	
					point.setLoc_status(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.LOC_STATUS_COL)));
					
					point.setRouteId(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.ROUTE_ID_COL)));
	
					// Adding contact to list
					waypointList.add(point);
	
				} while (cursor.moveToNext());
			}
			
		} finally {
			// to assure that the SQLite does no leaked.
			cursor.close();
			db.close();
		}

		// return waypoint list
		return waypointList;
	}
	
	
	/**
	 * Get list of checked-in waypoints with time < current time.
	 * @param with_sent_status \n 
	 * 			given 'true' to select point success sent.\n 
	 * 			given 'false' to select all logged points.\n
	 * @return list off selected waypoints.
	 */
	public List<Waypoints> getCheckinWaypoints(boolean with_sent_status) {

		List<Waypoints> waypointList = new ArrayList<Waypoints>();

		// Select All Query
		String selectQuery = "";
		String today0hr = getTimeFromZerohourToday();
		
		if (with_sent_status) {
			// Get checked-in waypoints logged today && not successfully sent yet
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					+ " WHERE TIME < " + System.currentTimeMillis()
					+ " AND TIME >= " + today0hr 
					+ " AND SENT_STATUS != 1"
					+ " AND CHECKIN_STATUS == 1"
					+ " ORDER BY TIME";
		} else {
			// Get checked-in waypoints logged today.
			selectQuery = "SELECT  * FROM " + Waypoints.TABLE
					+ " WHERE TIME < " + System.currentTimeMillis()
					+ " AND TIME >= " + today0hr
					+ " AND CHECKIN_STATUS == 1"
					+ " ORDER BY TIME";
		}

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		try {		
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {
					Waypoints point = new Waypoints();
					point.setImei(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.IMEI_COL)));
					point.setAccuracy(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ACCURACY_COL)));
					point.setAltitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.ALTITUDE_COL)));
					point.setBearing(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.BEARING_COL)));
					point.setCheckin_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.CHECKIN_STATUS_COL)));
					point.setLatitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LATITUDE_COL)));
					point.setLongtitude(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.LONGITUDE_COL)));
					point.setSpeed(cursor.getFloat(cursor
							.getColumnIndex(WaypointsColumns.SPEED_COL)));
					point.setTime(cursor.getLong(cursor
							.getColumnIndex(WaypointsColumns.TIME_COL)));
					point.setSent_status(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.SENT_STATUS_COL)));
					point.setLoc_status(cursor.getString(cursor
							.getColumnIndex(WaypointsColumns.LOC_STATUS_COL)));
					point.setRouteId(cursor.getInt(cursor
							.getColumnIndex(WaypointsColumns.ROUTE_ID_COL)));
					// Adding contact to list
					waypointList.add(point);
				} while (cursor.moveToNext());
			}
		} finally {
			// To gurantee that the SQLite does not leaked.
			cursor.close();
			db.close();
		}

		// return waypoint list
		return waypointList;
	}

	/**
	 * Get visit counting
	 * 
	 * @return
	 */
	/*
	 * public int getVisitCount() {
	 * 
	 * String countQuery = "SELECT  * FROM " + TABLE_VISIT; SQLiteDatabase db =
	 * this.getReadableDatabase(); Cursor cursor = db.rawQuery(countQuery,
	 * null); cursor.close();
	 * 
	 * // return count return cursor.getCount(); }
	 */
	/**
	 * Update a single visit by ID
	 * 
	 * @param contact
	 * @return
	 */
	/*
	 * public int updateById(Visit visit) { SQLiteDatabase db =
	 * this.getWritableDatabase();
	 * 
	 * ContentValues values = new ContentValues(); values.put(LONGTITUDE,
	 * visit.getLongtitude()); values.put(LATTITUDE, visit.getLattitude());
	 * 
	 * // updating row return db.update(TABLE_VISIT, values, KEY_ID + " = ?",
	 * new String[] { String.valueOf(visit.getId()) }); }
	 */

	/**
	 * This table contains waypoints.
	 * 
	 * @author 
	 */
	public static final class Waypoints extends WaypointsColumns implements
			android.provider.BaseColumns {
		/** The name of this table, waypoints */
		public static final String TABLE = "waypoints";
		static final String CREATE_STATEMENT = "CREATE TABLE "
				+ Waypoints.TABLE + "(" + " " + BaseColumns._ID + " "
				+ WaypointsColumns._ID_TYPE + "," + " "
				+ WaypointsColumns.LATITUDE_COL + " "
				+ WaypointsColumns.LATITUDE_TYPE + "," + " "
				+ WaypointsColumns.LONGITUDE_COL + " "
				+ WaypointsColumns.LONGITUDE_TYPE + "," + " "
				+ WaypointsColumns.TIME_COL + " " 
				+ WaypointsColumns.TIME_TYPE + "," + " " 
				+ WaypointsColumns.SPEED_COL + " "
				+ WaypointsColumns.SPEED_TYPE + "," + " "
				+ WaypointsColumns.IMEI_COL + " " 
				+ WaypointsColumns.IMEI_TYPE + "," + " " 
				+ WaypointsColumns.ACCURACY_COL + " "
				+ WaypointsColumns.ACCURACY_TYPE + "," + " "
				+ WaypointsColumns.ALTITUDE_COL + " "
				+ WaypointsColumns.ALTITUDE_TYPE + "," + " "
				+ WaypointsColumns.BEARING_COL + " "
				+ WaypointsColumns.BEARING_TYPE + "," + " "
				+ WaypointsColumns.ROUTE_ID_COL + " "
				+ WaypointsColumns.ROUTE_ID_TYPE + "," + " "
				+ WaypointsColumns.CHECKIN_STATUS_COL + " "
				+ WaypointsColumns.CHECKIN_STATUS_TYPE + "," + " "
				+ WaypointsColumns.LOC_STATUS_COL + " "
				+ WaypointsColumns.LOC_STATUS_TYPE + "," + " "
				+ WaypointsColumns.SENT_STATUS_COL + " "
				+ WaypointsColumns.SENT_STATUS_TYPE + ");";
	}

	/**
	 * Columns from the waypoints table.
	 * 
	 * @author 
	 */
	public static class WaypointsColumns {

		/** The imei */
		public static final String IMEI_COL = "imei";
		/** The latitude */
		public static final String LATITUDE_COL = "latitude";
		/** The longitude */
		public static final String LONGITUDE_COL = "longitude";
		/** The recorded time */
		public static final String TIME_COL = "time";
		/** The speed in meters per second */
		public static final String SPEED_COL = "speed";
		/** The accuracy of the fix */
		public static final String ACCURACY_COL = "accuracy";
		/** The altitude */
		public static final String ALTITUDE_COL = "altitude";
		/** the bearing of the fix */
		public static final String BEARING_COL = "bearing";
		/** the checkin status */
		public static final String CHECKIN_STATUS_COL = "checkin_status";
		/** the sent status */
		public static final String SENT_STATUS_COL = "sent_status";
		/** the loc status (start || stop) */
		public static final String LOC_STATUS_COL = "loc_status";
		/** the route id */
		public static final String ROUTE_ID_COL = "route_id";
		
		static final String LOC_STATUS_TYPE = "TEXT NOT NULL";
		static final String IMEI_TYPE = "TEXT NOT NULL";
		static final String LATITUDE_TYPE = "REAL NOT NULL";
		static final String LONGITUDE_TYPE = "REAL NOT NULL";
		static final String TIME_TYPE = "INTEGER NOT NULL";
		static final String SPEED_TYPE = "REAL";

		static final String ACCURACY_TYPE = "REAL";
		static final String ALTITUDE_TYPE = "REAL";
		static final String BEARING_TYPE = "REAL";
		static final String CHECKIN_STATUS_TYPE = "INTEGER";
		static final String SENT_STATUS_TYPE = "INTEGER";
		static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
		static final String ROUTE_ID_TYPE = "INTEGER";

		// Data object's properties:
		private String imei;
		private float latitude;
		private float longtitude;
		private long time;
		private float speed;
		private float accuracy;
		private float altitude;
		private float bearing;
		private int checkin_status;
		private int sent_status;
		private String loc_status;
		private long routeId;
		
		public long getRouteId() {
			return routeId;
		}

		public void setRouteId(long routeId) {
			this.routeId = routeId;
		}

		public String getLoc_status() {
			return loc_status;
		}

		public void setLoc_status(String loc_status) {
			this.loc_status = loc_status;
		}

		public int getSent_status() {
			return sent_status;
		}

		public void setSent_status(int sent_status) {
			this.sent_status = sent_status;
		}

		// getter and setter methods:
		public String getImei() {
			return imei;
		}

		public void setImei(String imei) {
			this.imei = imei;
		}

		public float getLatitude() {
			return latitude;
		}

		public void setLatitude(float latitude) {
			this.latitude = latitude;
		}

		public float getLongtitude() {
			return longtitude;
		}

		public void setLongtitude(float longtitude) {
			this.longtitude = longtitude;
		}

		public long getTime() {
			return time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public float getSpeed() {
			return speed;
		}

		public void setSpeed(float speed) {
			this.speed = speed;
		}

		public float getAccuracy() {
			return accuracy;
		}

		public void setAccuracy(float accuracy) {
			this.accuracy = accuracy;
		}

		public float getAltitude() {
			return altitude;
		}

		public void setAltitude(float altitude) {
			this.altitude = altitude;
		}

		public float getBearing() {
			return bearing;
		}

		public void setBearing(float bearing) {
			this.bearing = bearing;
		}

		public int getCheckin_status() {
			return checkin_status;
		}

		public void setCheckin_status(int checkin_status) {
			this.checkin_status = checkin_status;
		}

	}
	
	/**
	 * This table contains Routes.
	 * 
	 * @author 
	 */
	public static final class Routes extends RouteColumns implements
			android.provider.BaseColumns {
		/** The name of this table, routes */
		public static final String TABLE = "routes";
		static final String CREATE_STATEMENT = "CREATE TABLE "
				+ Routes.TABLE + "(" + " " 
				+ Routes.ID_COL + " "
				+ Routes._ID_TYPE + "," + " "
				+ Routes.TIME_COL + " " 
				+ Routes.TIME_TYPE + ");" ;
	}

	/**
	 * Columns from the Route table.
	 * 
	 * @author 
	 */
	public static class RouteColumns {
		
		/** The id col */
		public static final String ID_COL = "_id";
		
		/** The recorded time */
		public static final String TIME_COL = "time";
		
		static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
		static final String TIME_TYPE = "INTEGER NOT NULL";
		
		/** Data object's properties **/
		private long routeId;
		private long time;

		/** Getter and Setter functions for properties **/
		public long getTime() {
			return time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public long getRouteId() {
			return routeId;
		}

		public void setRouteId(long routeId) {
			this.routeId = routeId;
		}
	}
	
	/**
	 * When user click on 'start' button, this time is to create a new route, and this function is called to insert a new ID to DB
	 * @param location
	 * @param cv
	 * @return
	 */
	public long insertNewRoute(Location location) {
		
		SQLiteDatabase sqldb = getWritableDatabase();

		ContentValues args = new ContentValues();
		
		args.put(Routes.TIME_COL, location.getTime());
		
		long routeId = sqldb.insert(Routes.TABLE, null, args);
		
		if (routeId == -1){
			Utilities.LogError("insertNewRoute.error while insert" , new Exception(""));
		}
		
		sqldb.close();
		
		return routeId;
	}
	
	/**
	 * Get the latest route id from Route table.
	 * @return the latest route id
	 */
	public long getLastestRouteId() {

		long lastestId = 0;
		
		// Select All Query
		String selectQuery = "SELECT MAX(_ID) AS LATEST_ID FROM " + Routes.TABLE;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		try {
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {				
					lastestId = cursor.getLong(cursor.getColumnIndex("LATEST_ID"));
					
				} while (cursor.moveToNext());
			}
		} finally {
			// to assure that the SQLite does no leaked.
			cursor.close();
			db.close();
		}
		
		return lastestId;
	}
	
	/**
	 * Get the list of today's route id from Route table (Reversed order returned)
	 * @return the latest route id
	 */
	public List<Long> getListOfRouteId() {
		
		List<Long> listOfRouteId = new ArrayList<Long>();
		String today0hr = getTimeFromZerohourToday();
		
		// Select All Query
		String selectQuery = 	"SELECT * FROM " + Routes.TABLE
								+ " WHERE TIME >= " + today0hr
								+ " ORDER BY TIME DESC";
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		try {
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {				
					long routeId = cursor.getLong(cursor.getColumnIndex("_id"));
					listOfRouteId.add(routeId);
					
				} while (cursor.moveToNext());
			}
		} finally {
			// to assure that the SQLite does no leaked.
			cursor.close();
			db.close();
		}
		
		return listOfRouteId;
	}


}
