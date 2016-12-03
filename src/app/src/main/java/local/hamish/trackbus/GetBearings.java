package local.hamish.trackbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

interface BearingsReadyCallback {
    void bearingsReady();
}

class GetBearings {

    private JSONArray apiResponse;
    private SQLiteDatabase myDB = null;
    private BearingsReadyCallback callback;
    private Context context;

    GetBearings(Context context, BearingsReadyCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    static void createTable(Context context) {

        SQLiteDatabase myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);

        String cols = "trip_id TEXT, bearing INTEGER";
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Bearings (" + cols + ");");

        myDB.close();
    }

    void updateData() {
        getData();
    }

    private void process(JSONObject object) {
        try {
            String trip_id = object.getString("trip_id");
            String bearing = object.getString("bearing");

            String values = "'" + trip_id + "'," + bearing;
            String sql = "INSERT INTO Bearings VALUES (" + values + ");";
            myDB.execSQL(sql);

        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void processData() {

        myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        myDB.beginTransaction();

        myDB.execSQL("DELETE FROM Bearings");

        for (int i=0; i<apiResponse.length(); i++) {
            try {
                JSONObject object = apiResponse.getJSONObject(i);
                process(object);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        myDB.setTransactionSuccessful();
        myDB.endTransaction();
        callback.bearingsReady();
    }

    private void getData() {
        final String urlString = ATApi.getUrl(ATApi.API.bearings, null);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    apiResponse = jsonObject.getJSONArray("data");
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
