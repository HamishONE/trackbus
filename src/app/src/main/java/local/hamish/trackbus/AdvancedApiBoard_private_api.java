package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdvancedApiBoard_private_api extends AdvancedApiBoard {

    private JSONArray tripData = null;
    private JSONArray stopData = null;

    public AdvancedApiBoard_private_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName, boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

    // Call APIs
    @Override
    public void callAPIs() {
        getTripData(ATApi.data.apiRoot + ATApi.data.stopInfo + stopID + ATApi.getAuthorization(), true);
    }

    // Extract trip ids relevant to route from tripData and call stopData for these
    private void getStopData() {
        String tripsForApi = "&tripid=";
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
        getStopData(ATApi.data.apiRoot + ATApi.data.tripUpdates + ATApi.getAuthorization() + tripsForApi);
    }

    // Continues with code after network responds
    private void produceBoard(boolean incStops, boolean isNew) {

        int stopsAway = 0;
        String stopsAwayStr = "";
        String dueStr = "";
        String delayStr = "";
        String route = "";
        String tripDataTrip = "";
        String destination = "";
        int stopSeq = 0;
        String schTimeStr = "";
        Date schTime = null;
        int delay = 0;

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
                            dueStr = String.format("%+.0f:%02.0f", (dueSecs / 60), Math.abs(dueSecs % 60));
                            stopsAwayStr = stopsAway + "";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {

                    // Add to arrays
                    out.tripArray[out.count] = null;
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
                if (stopSeq == 1) {
                    out.scheduledArray[out.count] = true;
                } else if (destination.contains(stopName)) {
                    out.terminatingArray[out.count] = true;
                }

                DateFormat df = new SimpleDateFormat("hh:mm a");
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

    // Get trip data for one stop
    private void getTripData(final String urlString, final boolean isNew) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    tripData = jsonObject.getJSONArray("response");
                    getStopData();
                    produceBoard(false, isNew);
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
    private void getStopData(final String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
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
                produceBoard(true, true);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Refreshes all data
    @Override
    public void updateData() {
        tripData = null;
        stopData = null;
        out.count = 0;
        getTripData(ATApi.data.apiRoot + ATApi.data.stopInfo + stopID + ATApi.getAuthorization(), false);
    }

    // Refreshes board to show/hide terminating services
    @Override
    public void changeTerminating(boolean showTerminating) {
        this.showTerminating = showTerminating;
        produceBoard(true, false);
    }

}