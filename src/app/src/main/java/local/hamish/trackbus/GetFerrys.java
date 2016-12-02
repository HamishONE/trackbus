package local.hamish.trackbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

interface FerrysReadyCallback {
    void ferrysReady();
}

class GetFerrys {

    private JSONArray apiResponse;
    private SQLiteDatabase myDB = null;
    private FerrysReadyCallback callback;
    private Context context;

    GetFerrys(Context context, FerrysReadyCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    void updateData() {
        getData();
    }

    private void process(JSONObject object) {
        try {
            String vessel = object.getString("vessel");
            String latitude = object.getString("lat");
            String longitude = object.getString("lng");
            String timestamp = object.getString("timestamp"); //todo: deformat timestamp to make it usable

            String values = "'" + vessel + "'," + latitude + "," + longitude + ",'" + timestamp + "'";
            String sql = "INSERT INTO Ferrys VALUES (" + values + ");";
            myDB.execSQL(sql);

        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void processData() {

        myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        myDB.beginTransaction();

        String cols = "vessel TEXT, latitude REAL, longitude REAL, timestamp STRING";
        myDB.execSQL("DROP TABLE IF EXISTS Ferrys");
        myDB.execSQL("CREATE TABLE Ferrys (" + cols + ");");

        for (int i=0; i<apiResponse.length(); i++) {
            try {
                JSONObject object = apiResponse.getJSONObject(i);
                process(object);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        myDB.setTransactionSuccessful();
        myDB.endTransaction();
        callback.ferrysReady();
    }

    private void getData() {
        final String urlString = ATApi.getUrl(ATApi.API.ferrys, null);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    str = str.substring(11, str.length()-2);
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
