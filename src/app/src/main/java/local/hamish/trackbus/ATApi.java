package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

public final class ATApi {

    // Blank
    private ATApi() {}

    // Global vars
    private static int drift = 0;
    public static int errorCount = 0;

    // Important strings from AT Track My Bus App
    public final static class data {
        final static String apiRoot = "https://api.at.govt.nz/v1/";
        final static String apiKey = "1e6069be9fa8e5f7aa3fbcee39a783e6";
        final static String sharedSecret = "3843d5c0be9dc71e3a547d16bf2fcc9c";
        final static String stopInfo = "gtfs/stops/stopinfo/";
        final static String vehicleLocations = "public-restricted/realtime/vehiclelocations/";
        final static String tripUpdates = "public-restricted/realtime/tripUpdates/";
        final static String realtime = "public-restricted/realtime/";
        final static String shapeByTripId = "gtfs/shapes/tripId/";
        final static String stops = "gtfs/stops";
        final static String maxxUrl = "http://api.maxx.co.nz/RealTime/v2/Departures/Stop/";
        final static String departures = "public-restricted/departures/";
        final static String epochKey = "a471a096baaa08c893f48a909d0ae3d3";
    }

    // Returns string to append to api call
    public static String getAuthorization() {

        long urlTime = new Date().getTime()/1000 + drift;
        int epoch = (int) Math.floor(urlTime);
        String signature = Util.hmacSHA1(epoch + ATApi.data.apiKey, ATApi.data.sharedSecret);
        return "?api_sig=" + signature + "&api_key=" + ATApi.data.apiKey + "&ivu=true";
    }

    // Gets drift global var
    public static void getDrift() {
        getData(data.apiRoot + "time/epoch?api_key=" + data.epochKey);
    }

    // Finishes getting the drift
    private static void getData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    long urlTime = jsonObject.getJSONObject("response").getLong("time");
                    double computerTime = Math.floor(new Date().getTime() / 1000);
                    drift = (int) (urlTime - computerTime);

                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                // Todo: Implement way for user to know and relaunch
            }
        });
    }

}