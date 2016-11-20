package local.hamish.trackbus;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Locale;

class AllBusesHelper {

    private AdvancedApiBoard.Output out;
    private GoogleMap map;
    private JSONArray locData;
    Marker[] markers = new Marker[1000];
    int markLen = 0;
    private boolean firstCall = true;
    private ServiceBoardActivity serviceBoardActivity;
    private View circle;
    private FavouritesHelper favouritesHelper;

    private HashMap<Marker, Integer> mHashMap = new HashMap<>();

    // Constructor //todo: remove favouritesHelper req
    AllBusesHelper(ServiceBoardActivity serviceBoardActivity, View circle, GoogleMap map) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.map = map;
        this.circle = circle;
        favouritesHelper = new FavouritesHelper(serviceBoardActivity);
    }

    // Calls the API
    void callAPI(AdvancedApiBoard.Output out) {

        this.out = out;

        String allTrips = "&tripid=";
        for (int i = 0; i < out.count; i++) {
            allTrips += out.tripArray[i] + ",";
        }
        Log.e("Num requested: ", String.valueOf(out.count));
        getData(ATApi.data.apiRoot() + ATApi.data.vehicleLocations + ATApi.getAuthorization() + allTrips);
    }

    // Calls the API
    void simplify() {

        if (locData != null) main();
    }

    // Get the locations data JSON Array
    private void getData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    locData = jsonObject.getJSONObject("response").getJSONArray("entity");
                    main();
                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Finds the bus locations and adds to map
    private void main() {

        String locTripID;
        double lat;
        double lon;
        LatLng latLng;

        // Remove existing markers
        for (int i=0; i<markLen; i++) {markers[i].remove();}
        markLen = 0;
        mHashMap.clear();

        try {
            for (int i = 0; i < out.count; i++) {

                if (out.tripArray[i] == null) continue;

                for (int j = 0; j < locData.length(); j++) {
                    locTripID = locData.getJSONObject(j).getJSONObject("vehicle").getJSONObject("trip").getString("trip_id");
                    if (out.tripArray[i].equals(locTripID)) {
                        lat = locData.getJSONObject(j).getJSONObject("vehicle").getJSONObject("position").getDouble("latitude");
                        lon = locData.getJSONObject(j).getJSONObject("vehicle").getJSONObject("position").getDouble("longitude");
                        latLng = new LatLng(lat, lon);

                        Marker marker;
                        if (favouritesHelper.isFavRoute(out.routeArray[i])) {
                            marker =  map.addMarker(new MarkerOptions().position(latLng).title(out.routeArray[i])
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.heart_icon_pink)));
                        } else {
                            marker =  map.addMarker(new MarkerOptions().position(latLng).title(out.routeArray[i]));
                        }
                        markers[markLen++] = marker;
                        mHashMap.put(marker, i);
                    }
                }
            }
        } catch (JSONException e) {e.printStackTrace();}

        // Zoom to show all buses only on first call
        if (firstCall) { // todo: fiX!!!!!!
            serviceBoardActivity.zoomToAll();
            if (markLen != 0) firstCall = false;
        }

        // Remove loading bars
        circle.setVisibility(View.GONE);
        //swipeLayout.setRefreshing(false);

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker arg0) {
                String route = arg0.getTitle();
                int pos = mHashMap.get(arg0);
                String trip = out.tripArray[pos];
                int stopSeq = out.stopSeqArray[pos];

                serviceBoardActivity.callTracker(trip, stopSeq, route, serviceBoardActivity.out.dateSchArray[pos]); //todo:rah!
            }
        });
    }

    // Show snackbar and allow refreshing on HTTP failure
    void handleError(int statusCode) {
        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelMap);
        if (circle == null) {
            Log.e("Early exit", "from handleError in AdvancedApiBoard_private_api class");
            return;
        }
        circle.setVisibility(View.GONE);
        // Prepare message for snackbar
        String message;
        if (!Util.isNetworkAvailable(serviceBoardActivity.getSystemService(Context.CONNECTIVITY_SERVICE)))
            message = "Please connect to the internet";
        else if (statusCode == 0) message = "Network error (no response)";
        else if (statusCode >= 500) message = String.format(Locale.US, "AT server error (HTTP response %d)", statusCode);
        else message = String.format(Locale.US, "Network error (HTTP response %d)", statusCode);
        // Show snackbar
        if (serviceBoardActivity.snackbar != null && serviceBoardActivity.snackbar.isShown()) return;
        View view = serviceBoardActivity.findViewById(R.id.cordLayout);
        serviceBoardActivity.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        serviceBoardActivity.snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {serviceBoardActivity.updateData(true);}
        });
        serviceBoardActivity.snackbar.show();
    }

}