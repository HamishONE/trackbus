package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class ATApi {

    private enum API_VERSION {APIv1, APIv2};
    private static API_VERSION currentApiVersion = API_VERSION.APIv2; //todo:replace with setting;
    private static int drift = 0;
    static int errorCount = 0;

    // Important strings from AT Track My Bus App
    public static class data {

        static String apiRoot() {
            if (currentApiVersion == API_VERSION.APIv2) {
                return "https://api.at.govt.nz/v2/";
            } else  {
                return "https://api.at.govt.nz/v1/";
            }
        }

        final static String stopInfo = "gtfs/stops/stopinfo/";
        final static String vehicleLocations = "public-restricted/realtime/vehiclelocations/";
        final static String tripUpdates = "public-restricted/realtime/tripUpdates/";
        final static String realtime = "public-restricted/realtime/";
        final static String shapeByTripId = "gtfs/shapes/tripId/";
        final static String stops = "gtfs/stops";
        final static String departures = "public-restricted/departures/";
    }

    // Returns string to append to api call
    static String getAuthorization() {

        if (currentApiVersion == API_VERSION.APIv2) {

            //final String key = "fd2c776a9b0543d9b5e3fba4a2e25576"; //Hamish primary
            //final String key = "8f65bac859494abf9a8808983a3c8ab5"; //Hamish secondary
            final String key = "323741614c1c4b9083299adefe100aa6"; //AT internal

            return "?subscription-key=" + key;

        } else {

            final String apiKey = "1e6069be9fa8e5f7aa3fbcee39a783e6";
            final String sharedSecret = "3843d5c0be9dc71e3a547d16bf2fcc9c";

            long urlTime = new Date().getTime()/1000 + drift;
            int epoch = (int) Math.floor(urlTime);
            String signature = Util.hmacSHA1(epoch + apiKey, sharedSecret);
            return "?api_sig=" + signature + "&api_key=" + apiKey + "&ivu=true";
        }
    }

    // Gets drift global var
    static void getDrift() {

        final String epochKey = "a471a096baaa08c893f48a909d0ae3d3";
        getData(ATApi.data.apiRoot() + "time/epoch?api_key=" + epochKey);
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