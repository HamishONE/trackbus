package local.hamish.trackbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

interface RoutesReadyCallback {
    void routesReady();
}

class GetRoutes {

    private JSONArray apiResponse;
    private SQLiteDatabase myDB = null;
    private RoutesReadyCallback callback;
    private Context context;

    GetRoutes(Context context, RoutesReadyCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    void updateData() {
        getData();
    }

    private void process(JSONObject object) {
        try {
            String route_id = object.getString("route_id");
            String route_short_name = object.getString("route_short_name");
            String route_long_name = object.getString("route_long_name");
            String route_type = object.getString("route_type");

            String values = "'" + route_id + "','" + route_short_name + "','" + route_long_name + "'," + route_type;
            String sql = "INSERT INTO Routes VALUES (" + values + ");";
            myDB.execSQL(sql);

        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void processData() {

        myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        myDB.beginTransaction();

        String cols = "route_id TEXT, route_short_name TEXT, route_long_name TEXT, route_type INTEGER";
        myDB.execSQL("DROP TABLE IF EXISTS Routes");
        myDB.execSQL("CREATE TABLE Routes (" + cols + ");");

        for (int i=0; i<apiResponse.length(); i++) {
            try {
                JSONObject object = apiResponse.getJSONObject(i);
                process(object);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        myDB.setTransactionSuccessful();
        myDB.endTransaction();
        myDB.close();
        callback.routesReady();
    }

    private void getData() {
        final String urlString = ATApi.getUrl(ATApi.API.routes, null);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    apiResponse = jsonObject.getJSONArray("response");
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
