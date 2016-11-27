package local.hamish.trackbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

interface CombinedApiRequest {
    void done(boolean doReplaceMarkers);
}

class CombinedApiBoard {

    static private JSONArray apiResponse;
    private SQLiteDatabase myDB = null;
    private CombinedApiRequest combinedApiRequest;

    CombinedApiBoard(Context context, CombinedApiRequest combinedApiRequest) {
        myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        this.combinedApiRequest = combinedApiRequest;
    }

    void updateData() {
        getData();
    }

    private void processStopData (JSONObject object) {
        try {
            String trip_id = object.getJSONObject("trip_update").getJSONObject("trip").getString("trip_id");
            String route_id = object.getJSONObject("trip_update").getJSONObject("trip").getString("route_id");
            String vehicle_id = object.getJSONObject("trip_update").getJSONObject("vehicle").getString("id");
            int stop_sequence = object.getJSONObject("trip_update").getJSONObject("stop_time_update").getInt("stop_sequence");
            int stop_id = object.getJSONObject("trip_update").getJSONObject("stop_time_update").getInt("stop_id");
            //int timestamp = object.getJSONObject("trip_update").getInt("timestamp");

            int delay;
            JSONObject stop_time_update = object.getJSONObject("trip_update").getJSONObject("stop_time_update");
            if (stop_time_update.has("arrival")) delay = stop_time_update.getJSONObject("arrival").getInt("delay");
            else delay = stop_time_update.getJSONObject("departure").getInt("delay");

            String values = "'" + trip_id + "','" + route_id + "','" + vehicle_id + "'," + stop_sequence + "," + stop_id + "," + delay;

            /*
            String sql = "INSERT INTO CombinedApi (trip_id,route_id,vehicle_id,stop_sequence,stop_id,delay) VALUES ("+values+");";
            myDB.execSQL(sql);
            */

            String sql2 = "INSERT INTO StopData (trip_id,route_id,vehicle_id,stop_sequence,stop_id,delay) VALUES ("+values+");";
            myDB.execSQL(sql2);

        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void processLocData (JSONObject object) {
        try {

            String trip_id = object.getJSONObject("vehicle").getJSONObject("trip").getString("trip_id");
            String route_id = object.getJSONObject("vehicle").getJSONObject("trip").getString("route_id");
            String start_time = object.getJSONObject("vehicle").getJSONObject("trip").optString("start_time", "");
            String vehicle_id = object.getJSONObject("vehicle").getJSONObject("vehicle").getString("id");
            double latitude = object.getJSONObject("vehicle").getJSONObject("position").getDouble("latitude");
            double longitude = object.getJSONObject("vehicle").getJSONObject("position").getDouble("longitude");
            int bearing = object.getJSONObject("vehicle").getJSONObject("position").optInt("bearing", 0); //todo: use last bearing
            int timestamp = object.getJSONObject("vehicle").getInt("timestamp");
            int occupancy_status = object.getJSONObject("vehicle").optInt("occupancy_status", -1);

            if (start_time.equals("")) {
                latitude = latitude*1.66 + 23.7564;
                longitude = longitude*1.66 - 114.8370;

                if (latitude < -37.091) {
                    latitude += 0.6639;
                }
            }

            /*
            String values = "start_time='"+start_time+"',vehicle_id='"+vehicle_id+"',latitude="+latitude+",longitude="+longitude
                    +",bearing="+bearing+",timestamp="+timestamp+",occupancy_status="+occupancy_status;
            String sql = "UPDATE CombinedApi SET " + values + " WHERE trip_id='" + trip_id + "';";
            myDB.execSQL(sql);
            */

            String values2 = "'" + trip_id + "','" + route_id + "','" + start_time + "','" + vehicle_id + "'," + latitude + ","
                    + longitude + "," + bearing + "," + timestamp + "," + occupancy_status;
            String sql2 = "INSERT INTO LocData (trip_id, route_id, start_time, vehicle_id, latitude, longitude, bearing, timestamp, " +
                    "occupancy_status) VALUES (" + values2 + ");";

            myDB.execSQL(sql2);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processMessage (JSONObject object) {
        //do nothing for now
    }

    private void processData() {

        /*
        String cols0 = "trip_id TEXT, route_id TEXT, vehicle_id TEXT, stop_sequence INTEGER, " +
                "stop_id INTEGER, delay INTEGER, start_time TEXT, latitude REAl, longitude REAL, " +
                "bearing INTEGER, occupancy_status INTEGER, timestamp INTEGER";
        myDB.execSQL("DROP TABLE IF EXISTS CombinedApi");
        myDB.execSQL("CREATE TABLE CombinedApi (" + cols0 + ");");
        */

        String cols1 = "trip_id TEXT, route_id TEXT, vehicle_id TEXT, stop_sequence INTEGER, " + "stop_id INTEGER, delay INTEGER";
        myDB.execSQL("DROP TABLE IF EXISTS StopData");
        myDB.execSQL("CREATE TABLE StopData (" + cols1 + ");");

        String cols2 = "trip_id TEXT, route_id TEXT, start_time TEXT, vehicle_id TEXT, latitude REAL, longitude REAL, bearing INTEGER, timestamp INTEGER, " +
                "occupancy_status INTEGER";
        myDB.execSQL("DROP TABLE IF EXISTS LocData");
        myDB.execSQL("CREATE TABLE LocData (" + cols2 + ");");

        myDB.beginTransaction();
        for (int i=0; i<apiResponse.length(); i++) {
            try {
                JSONObject object = apiResponse.getJSONObject(i);

                if (object.has("trip_update")) processStopData(object);
                else if (object.has("vehicle")) processLocData(object);
                else processMessage(object);

            } catch (JSONException e) { e.printStackTrace(); }
        }
        myDB.setTransactionSuccessful();
        myDB.endTransaction();

        combinedApiRequest.done(true);
    }

    private void getData() {

        final String urlString = ATApi.data.apiRoot() + ATApi.data.realtime + ATApi.getAuthorization();
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);
                    apiResponse = jsonObject.getJSONObject("response").getJSONArray("entity");

                    processData();
                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                //handleError(statusCode); //todo: do something
            }
        });
    }

}