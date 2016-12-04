package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class ATApi {

    private enum API_VERSION {APIv1, APIv2}
    private static API_VERSION currentApiVersion = API_VERSION.APIv2; //todo:replace with setting;
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
        bearings
    }

    private final static String ATRoot = "https://api.at.govt.nz/v2/";
    private final static String HamishRoot = "http://hamishserver.ddns.net/buffer?api=";

    static String getUrl(API api, String param) {
        switch (api) {
            // Special
            case bearings:
                return "http://hamishserver.ddns.net/buffer/bearings.php";
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
                return ATRoot + "gtfs/stops/stopinfo/" + param + getAuthorization();
            case shapeByTripId:
                if (param == null) return null;
                return ATRoot + "gtfs/shapes/tripId/" + param + getAuthorization();
            case stops:
                return ATRoot + "gtfs/stops" + getAuthorization();
            case departures:
                if (param == null) return null;
                return ATRoot + "public-restricted/departures/" + param + getAuthorization();
        }
        return null;
    }

    //final static String ferrys = "https://api.at.govt.nz/v1/api_node/realTime/ferryPositions?callback=lol";
    //final static String routes = ATRoot + "gtfs/routes";
    //final static String vehicleLocations = ATRoot + "public-restricted/realtime/vehiclelocations/";
    //final static String tripUpdates = ATRoot + "public-restricted/realtime/tripUpdates/";
    //final static String realtime = ATRoot + "public-restricted/realtime/";

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
    private static String getAuthorization() {

        if (currentApiVersion == API_VERSION.APIv2) {

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

        if (currentApiVersion == API_VERSION.APIv2) return;

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