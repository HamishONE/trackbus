package local.hamish.trackbus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class ATApi {
    
    private static int drift = 0;
    static int errorCount = 0;

    enum API {
        vehiclelocations,
        tripupdates,
        realtime,
        routes,
        ferrys,
        stopInfo,
        shapeByTripId,
        stops,
        departures,
        bearings,
        scheduled
    }
    
    private final static String HamishRoot = "http://hamishserver.ddns.net/buffer?api=";
    
    private static String getATRoot(SharedPreferences settings) {
        
        String value = settings.getString("api_version", "v2");
        if (value.equals("v2")) {
            return "https://api.at.govt.nz/v2/";
        } else {
            return "https://api.at.govt.nz/v1/";
        }
    }

    static String getUrl(Context context, API api, String param) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String value = settings.getString("server_buffer", "false");

        if (value.equals("false")) {
            switch (api) {
                case vehiclelocations:
                    return getATRoot(settings) + "public-restricted/realtime/vehiclelocations/" + getAuthorization(settings);
                case tripupdates:
                    return getATRoot(settings) + "public-restricted/realtime/tripUpdates/" + getAuthorization(settings);
                case realtime:
                    return getATRoot(settings) + "public-restricted/realtime/" + getAuthorization(settings);
                case routes:
                    return getATRoot(settings) + "gtfs/routes" + getAuthorization(settings);
                case ferrys:
                    return "https://api.at.govt.nz/v1/api_node/realTime/ferryPositions?callback=lol";
            }
        }

        switch (api) {
            // Special
            case bearings:
                return "http://hamishserver.ddns.net/buffer/bearings.php";
            case scheduled:
                return "http://hamishserver.ddns.net/timetable/api.php";
            // Server buffer
            case vehiclelocations:
                return HamishRoot + "vehiclelocations";
            case tripupdates:
                return HamishRoot + "tripupdates";
            case realtime:
                return HamishRoot + "realtime";
            case routes:
                return HamishRoot + "routes";
            case ferrys:
                return HamishRoot + "ferrys";
            // AT direct
            case stopInfo:
                if (param == null) return null;
                return getATRoot(settings) + "gtfs/stops/stopinfo/" + param + getAuthorization(settings);
            case shapeByTripId:
                if (param == null) return null;
                return getATRoot(settings) + "gtfs/shapes/tripId/" + param + getAuthorization(settings);
            case stops:
                return getATRoot(settings) + "gtfs/stops" + getAuthorization(settings);
            case departures:
                if (param == null) return null;
                return getATRoot(settings) + "public-restricted/departures/" + param + getAuthorization(settings);
        }

        return null;
    }

    static boolean isDoubleDecker(String vehicle) {

        // List from: github.com/consindo/dymajo-transit/blob/master/server/realtime.js
        List<String> doubleDeckers = Arrays.asList(

            // NZ Bus (ADL Enviro500)
            "3A99", "3A9A", "3A9B", "3A9C", "3A9D", "3A9E", "3A9F",
            "3AA0", "3AA1", "3AA2", "3AA3", "3AA4", "3AA5", "3AA6",
            "3AA7", "3AA8", "3AA9", "3AAA", "3AAB", "3AAC", "3AAD",
            "3AAE", "3AAF",

            // Howick and Eastern (ADL Enviro500)
            "5FB4", "5FB5", "5FB6", "5FB7", "5FB8", "5FB9", "5FBA",
            "5FBB", "5FBC", "5FBD", "5FBE", "5FBF", "5FC0", "5FC1",
            "5FC2",

            // Ritchies/NEX (BCI CityRider FBC6123BRZ)
            "5622", "5623", "5624", "5625", "5626", "5627", "5628",
            "5629", "562A", "562B", "562C", "562D", "562E", "562F",
            "5630"
        );
        return doubleDeckers.contains(vehicle);
    }

    // Returns string to append to api call
    private static String getAuthorization(SharedPreferences settings) {

        String value = settings.getString("api_version", "v2");

        if (value.equals("v2")) {

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
        getData("https://api.at.govt.nz/v1/time/epoch?api_key=" + epochKey);
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