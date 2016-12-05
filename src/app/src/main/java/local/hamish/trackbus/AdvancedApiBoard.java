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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

class AdvancedApiBoard {

    private ServiceBoardActivity serviceBoardActivity;
    private String stopID;
    private String stopName;
    private boolean showTerminating;
    private JSONArray tripData = null;
    boolean active = true;
    private JSONArray stopData = null;
    private ArrayList<OutputItem> out = new ArrayList<>();

    // Constructor
    AdvancedApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName,
                       boolean showTerminating, ArrayList<OutputItem> out) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;
        this.stopName = stopName;
        this.showTerminating = showTerminating;
        this.out = out;
    }

    // Get trip data for one stop
    private void getTripData(final boolean isNew) {
        final String urlString = ATApi.getUrl(ATApi.API.stopInfo, stopID);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);
                    tripData = jsonObject.getJSONArray("response");

                    if (active) serviceBoardActivity.prepareMap(); // todo: stop showing future stops, send other arrays

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
    private void getStopData() {
        final String urlString = ATApi.getUrl(ATApi.API.tripupdates, null);
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
        GregorianCalendar schTime;
        int delay;
        String vehicle_id;

        out.clear();

        // Loop through all bus trips
        for (int i = 0; i < tripData.length(); i++) {

            OutputItem item = new OutputItem();
            vehicle_id = null;

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
                GregorianCalendar cTime = new GregorianCalendar();

                if (schTimeStr.substring(0, 2).equals("24")) {
                    // Adjust time as falls in next day
                    schTimeStr = schTimeStr.replace("24", "00");

                    (schTime = new GregorianCalendar()).setTime(Util.deformatTime(schTimeStr, "HH:mm:ss"));
                    schTime = new GregorianCalendar(cTime.get(Calendar.YEAR), cTime.get(Calendar.MONTH),
                            cTime.get(Calendar.DAY_OF_MONTH)+1, schTime.get(Calendar.HOUR_OF_DAY),
                            schTime.get(Calendar.MINUTE), schTime.get(Calendar.SECOND) );
                } else {

                    (schTime = new GregorianCalendar()).setTime(Util.deformatTime(schTimeStr, "HH:mm:ss"));
                    schTime = new GregorianCalendar(cTime.get(Calendar.YEAR), cTime.get(Calendar.MONTH),
                            cTime.get(Calendar.DAY_OF_MONTH), schTime.get(Calendar.HOUR_OF_DAY),
                            schTime.get(Calendar.MINUTE), schTime.get(Calendar.SECOND) );
                }

                if (stopData!=null) {
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
                            if (schTime.after(GregorianCalendar.getInstance())) {
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

                            vehicle_id = stopDict.getJSONObject("trip_update").getJSONObject("vehicle").getString("id");

                            // Find delay
                            try {
                                delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update").getJSONObject("departure").getInt("delay");
                            } catch (JSONException e) {
                                delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update").getJSONObject("arrival").getInt("delay");
                            }

                            // Calculate due time
                            double dueTime = schTime.getTimeInMillis() / 1000 + delay;
                            double dueSecs = dueTime - (new Date().getTime() / 1000);

                            // Add to arrays
                            item.stopSequence = stopSeq;

                            // Format numbers
                            dueStr = String.format(Locale.US, "%+.0f:%02.0f", (dueSecs / 60), Math.abs(dueSecs % 60));
                            stopsAwayStr = stopsAway + "";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {

                    // Add to arrays
                    item.stopSequence = 100; //todo: fix this!

                    if (schTime.after(GregorianCalendar.getInstance())) {
                        // Make strings blank
                        stopsAwayStr = "";
                        dueStr = "";
                    } else {
                        continue; //Skips to next iteration
                    }
                }

                // Check for terminating or scheduled service
                item.isScheduled = false;
                item.isTerminating = false;
                if (stopSeq == 1) {
                    item.isScheduled = true;
                } else if (destination.contains(stopName)) {
                    item.isTerminating = true; //todo: no longer works, consider removing
                }

                DateFormat df = new SimpleDateFormat("hh:mm a", Locale.US);
                String schTimeStrNew = df.format(schTime.getTime());

                item.trip = tripDataTrip;
                item.route = route;
                item.schTime = schTimeStrNew;
                item.stopsAway = stopsAwayStr;
                item.headsign = destination;
                item.dueTime = dueStr;
                item.dateScheduled = schTime.getTimeInMillis() / 1000;
                item.vehicle_id = vehicle_id;
                out.add(item);

            } catch (JSONException e) {e.printStackTrace();}
        }

        if (active && (isNew || incStops)) serviceBoardActivity.produceView(incStops);
        if (active && incStops) serviceBoardActivity.allBusesHelper.simplify();
    }

    void callAPIs() {
        getTripData(true);
        getStopData(); //new

        //new CombinedApiBoard(serviceBoardActivity).updateData();
    }

    // Refreshes all data
    void updateData() {

        tripData = null;
        stopData = null;
        getTripData(false);
        getStopData(); //new
    }

    // Refreshes board to show/hide terminating services
    void changeTerminating(boolean showTerminating) {
        this.showTerminating = showTerminating;
        produceBoard(false);
    }

    static class OutputItem {

        int stopSequence;
        String route;
        String trip;
        String headsign;
        String schTime;
        String stopsAway;
        String dueTime;
        boolean isTerminating;
        boolean isScheduled;
        long dateScheduled;
        String vehicle_id;
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

        String message = Util.generateErrorMessage(serviceBoardActivity, statusCode);

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
