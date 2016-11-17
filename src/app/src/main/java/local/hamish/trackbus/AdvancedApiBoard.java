package local.hamish.trackbus;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class AdvancedApiBoard {

    private ServiceBoardActivity serviceBoardActivity;
    private String stopID;
    private String stopName;
    private boolean showTerminating;
    private Output out;
    boolean active = true;
    private JSONArray tripData = null;
    private JSONArray stopData = null;

    // Constructor
    AdvancedApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName,
                       boolean showTerminating, Output out) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;
        this.stopName = stopName;
        this.showTerminating = showTerminating;
        this.out = out;
    }

    // Get trip data for one stop
    private void getTripData(final boolean isNew) {
        final String urlString = ATApi.data.apiRoot() + ATApi.data.stopInfo + stopID + ATApi.getAuthorization();
        AsyncHttpClient client = new AsyncHttpClient();
        //client.addHeader(PublicApiV2.data.headerTerm, PublicApiV2.data.primaryKey);
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    tripData = jsonObject.getJSONArray("response");
                    getStopDataWithTripIDs();
                    produceBoard(isNew);
                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Get all stop data
    private void getStopData(final String tripIDs) {
        final String urlString = ATApi.data.apiRoot() + ATApi.data.tripUpdates + ATApi.getAuthorization() + "&" + tripIDs;
        AsyncHttpClient client = new AsyncHttpClient();
        //client.addHeader(PublicApiV2.data.headerTerm, PublicApiV2.data.primaryKey);
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    ATApi.errorCount = 0;
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);
                    stopData = jsonObject.getJSONObject("response").getJSONArray("entity");

                } catch (JSONException e) {e.printStackTrace();}
                // Refresh board with new data
                out.count = 0;
                produceBoard(true);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Continues with code after network responds
    private void produceBoard(boolean isNew) {

        if (tripData == null) return;
        boolean incStops = !(stopData == null);

        int stopsAway;
        String stopsAwayStr = "";
        String dueStr = "";
        String route;
        String tripDataTrip;
        String destination ;
        int stopSeq;
        String schTimeStr;
        Date schTime;
        int delay;

        for (int i = 0; i < out.terminatingArray.length; i++) {
            out.terminatingArray[i] = false;
        }

        out.count = 0; // todo: some code now redundant???

        // Loop through all bus trips
        for (int i = 0; i < tripData.length(); i++) {
            try {
                // Extract key trip data
                route = tripData.getJSONObject(i).getString("route_short_name");
                tripDataTrip = tripData.getJSONObject(i).getString("trip_id");
                stopSeq = tripData.getJSONObject(i).getInt("stop_sequence");
                schTimeStr = tripData.getJSONObject(i).getString("departure_time");
                destination = tripData.getJSONObject(i).getString("trip_headsign");

                if (destination.contains(stopName) && !showTerminating && stopSeq != 1) { //todo: are we ok with contains (not equals)?
                    continue;
                }

                // Parse scheduled time
                Date cTime = new Date();
                // todo: clean up below
                //schTime = Util.deformatTime(schTimeStr, "kk:mm:ss");
                //schTime = new Date(cTime.getYear(), cTime.getMonth(), cTime.getDate(), schTime.getHours(), schTime.getMinutes(), schTime.getSeconds());

                if (schTimeStr.substring(0, 2).equals("24")) {
                    // Adjust time as falls in next day
                    schTimeStr = schTimeStr.replace("24", "00");
                    schTime = Util.deformatTime(schTimeStr, "HH:mm:ss");
                    schTime = new Date(cTime.getYear(), cTime.getMonth(), cTime.getDate() + 1, schTime.getHours(), schTime.getMinutes(), schTime.getSeconds());
                } else {
                    schTime = Util.deformatTime(schTimeStr, "HH:mm:ss");
                    schTime = new Date(cTime.getYear(), cTime.getMonth(), cTime.getDate(), schTime.getHours(), schTime.getMinutes(), schTime.getSeconds());
                }

                if (incStops && stopData!=null) {
                    // Find stops sequence
                    JSONObject stopDict;
                    try {
                        stopDict = null;
                        for (int j = 0; j < stopData.length(); j++) {
                            if (stopData.getJSONObject(j).getJSONObject("trip_update").getJSONObject("trip").getString("trip_id").equals(tripDataTrip + "")) {
                                stopDict = stopData.getJSONObject(j);
                                break;
                            }
                        }
                        if (stopDict == null) {
                            if (schTime.after(new Date())) {
                                // Make strings blank
                                stopsAwayStr = "";
                                dueStr = "";
                            } else {
                                continue; //Skips to next iteration
                            }
                        } else {
                            // Find stops away
                            stopsAway = stopSeq - stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update").getInt("stop_sequence");
                            if (stopsAway < 0) continue; //Skips to next iteration

                            // Find delay
                            try {
                                delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update").getJSONObject("departure").getInt("delay");
                            } catch (JSONException e) {
                                delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update").getJSONObject("arrival").getInt("delay");
                            }

                            // Calculate due time
                            double dueTime = schTime.getTime() / 1000 + delay;
                            double dueSecs = dueTime - (new Date().getTime() / 1000);

                            // Add to arrays
                            out.tripArray[out.count] = tripDataTrip;
                            out.stopSeqArray[out.count] = stopSeq;

                            // Format numbers
                            dueStr = String.format(Locale.US, "%+.0f:%02.0f", (dueSecs / 60), Math.abs(dueSecs % 60));
                            stopsAwayStr = stopsAway + "";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {

                    // Add to arrays
                    out.tripArray[out.count] = tripDataTrip;
                    out.stopSeqArray[out.count] = 100;

                    if (schTime.after(new Date())) {
                        // Make strings blank
                        stopsAwayStr = "";
                        dueStr = "";
                    } else {
                        continue; //Skips to next iteration
                    }
                }

                // Check for terminating or scheduled service
                out.scheduledArray[out.count] = false;
                out.terminatingArray[out.count] = false;
                if (stopSeq == 1) {
                    out.scheduledArray[out.count] = true;
                } else if (destination.contains(stopName)) {
                    out.terminatingArray[out.count] = true; //no longer works, consider removing
                }

                DateFormat df = new SimpleDateFormat("hh:mm a", Locale.US);
                String schTimeStrNew = df.format(schTime);

                out.routeArray[out.count] = route;
                out.schTimeArray[out.count] = schTimeStrNew;
                out.stopsAwayArray[out.count] = stopsAwayStr;
                out.headsignArray[out.count] = destination;
                out.dueTimeArray[out.count] = dueStr;
                out.dateSchArray[out.count] = schTime.getTime() / 1000;
                out.count++;
            } catch (JSONException e) {e.printStackTrace();}
        }
        if (active && (isNew || incStops)) serviceBoardActivity.produceView(incStops);
        if (active && incStops) serviceBoardActivity.allBusesHelper.simplify(out.tripArray, out.routeArray, out.stopSeqArray);
    }

    // Extract trip ids relevant to route from tripData and call stopData for these
    private void getStopDataWithTripIDs() {
        String tripsForApi = "tripid=";
        int i;
        for (i = 0; i < tripData.length(); i++) {
            try {
                tripsForApi += tripData.getJSONObject(i).getString("trip_id");
                tripsForApi += ",";
                out.tripArray[i] = tripData.getJSONObject(i).getString("trip_id");
            } catch (JSONException e) {e.printStackTrace();}
        }
        out.count = i;
        if (active) serviceBoardActivity.prepareMap(); // todo: stop showing future stops, send other arrays
        //getStopData(tripsForApi);
    }

    void callAPIs() {
        getTripData(true);
        getStopData(""); //new
    }

    // Refreshes all data
    void updateData() {
        tripData = null;
        stopData = null;
        out.count = 0;
        getTripData(false);
        getStopData(""); //new
    }

    // Refreshes board to show/hide terminating services
    void changeTerminating(boolean showTerminating) {
        this.showTerminating = showTerminating;
        produceBoard(false);
    }

    // Object for outputs
    static class Output {

        String listArray[] = new String[1000];
        int stopSeqArray[] = new int[1000];
        String routeArray[] = new String[1000];
        String tripArray[] = new String[1000];
        String headsignArray[] = new String[1000];
        String schTimeArray[] = new String[1000];
        String stopsAwayArray[] = new String[1000];
        String dueTimeArray[] = new String[1000];
        boolean terminatingArray[] = new boolean[1000];
        boolean scheduledArray[] = new boolean[1000];
        long dateSchArray[] = new long[1000];
        int count = 0;
    }

    // Show snackbar and allow refreshing on HTTP failure
    private void handleError(int statusCode) {
        serviceBoardActivity.prepareMap();
        serviceBoardActivity.allBusesHelper.handleError(statusCode);

        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelNew);
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
