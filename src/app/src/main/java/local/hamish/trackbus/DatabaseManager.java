package local.hamish.trackbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

class DatabaseManager {

    private Context context;

    DatabaseManager(Context context) {
        this.context = context;
    };

    void createTables() {

        SQLiteDatabase myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        myDB.beginTransaction();

        myDB.execSQL("CREATE TABLE IF NOT EXISTS FavRoutes(route TEXT);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Favourites(stopID INTEGER, stopName TEXT, userName TEXT);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS RecentStops(stopID INTEGER, stopName TEXT);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Stops(stopID INTEGER, lat FLOAT, lon FLOAT, stopName TEXT);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Meta (id TEXT, value INTEGER);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Routes(route_id TEXT, route_short_name TEXT, " +
                "route_long_name TEXT, route_type INTEGER);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS StopData (trip_id TEXT, route_id TEXT, vehicle_id TEXT, " +
                "stop_sequence INTEGER, stop_id INTEGER, delay INTEGER);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS LocData (trip_id TEXT, route_id TEXT, start_time TEXT, vehicle_id TEXT, " +
                "latitude REAL, longitude REAL, bearing INTEGER, timestamp INTEGER, occupancy_status INTEGER);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Ferrys (vessel TEXT, latitude REAL, longitude REAL, timestamp TEXT);");
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Bearings (trip_id TEXT, bearing INTEGER);");

        // Update or create db_version
        int db_version = 1;
        myDB.execSQL("UPDATE Meta SET value=" + db_version + " WHERE id='db_version';" +
                "INSERT INTO Meta (id, value) SELECT 'db_version', " + db_version + " WHERE (Select Changes() = 0);");

        myDB.setTransactionSuccessful();
        myDB.endTransaction();
        myDB.close();
    }
}
