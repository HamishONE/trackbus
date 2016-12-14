package local.hamish.trackbus;

import android.support.annotation.NonNull;
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
import java.util.Date;
import java.util.Locale;

class TraditionalApiBoard {

    ArrayList<Items> items;
    boolean active = true;

    private ServiceBoardActivity serviceBoardActivity;
    private JSONArray boardData = null;
    private String stopID;

    // Constructor
    TraditionalApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;
    }

    // Refreshes all data
    void updateData() {
        getBoardData(ATApi.getUrl(serviceBoardActivity, ATApi.API.departures, stopID));
    }

    // Continues with code after network responds
    private void produceBoard() {

        ArrayList<Items> items_temp = new ArrayList<>();

        for (int i=0; i < boardData.length(); i++) {
            try {
                // Export relevant maxxData
                boolean isMonitored = boardData.getJSONObject(i).getBoolean("monitored");
                String route = boardData.getJSONObject(i).getString("route_short_name");
                String sign = boardData.getJSONObject(i).getString("destinationDisplay");
                String actual = boardData.getJSONObject(i).getString("expectedDepartureTime");
                String scheduled = boardData.getJSONObject(i).getString("scheduledDepartureTime");
                String platform = boardData.getJSONObject(i).getString("departurePlatformName");

                // Clean and format scheduled time
                Date schTime = cleanTime(scheduled);
                DateFormat df = new SimpleDateFormat("hh:mm a", Locale.US);
                String schStr = df.format(schTime);

                // Add general data to arrays
                Items item = new Items();
                item.route = route;
                item.headsign = sign;
                item.scheduled = schStr;
                item.scheduledDate = schTime;
                item.platform = platform;

                //Check if actual due time
                if (isMonitored && !actual.equals("null")) {
                    // Calculate due time and add to array
                    double dblActual = cleanTime(actual).getTime() - (new Date().getTime());
                    dblActual /= 1000*60;
                    item.dueTime = String.valueOf(Math.round(dblActual));
                }

                items_temp.add(item);
            } catch (JSONException e) {e.printStackTrace();}
        }

        items = items_temp;

        if (active) {
            serviceBoardActivity.produceViewOld();
        }
    }

    // Get trip data for one stop
    private void getBoardData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                extractJsonArray(new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Changes string into seconds since epoch
    private Date cleanTime(String string) {
        string = string.substring(0,19); // Remove time zone (GMT)
        string = string.replace('T', '_'); // Otherwise T is processed to mean something
        return Util.gmtDeformatTime(string, "yyyy-MM-dd_HH:mm:ss");
    }

    private void extractJsonArray(String str) {
        try {
            JSONObject jsonObject = new JSONObject(str);
            boardData = jsonObject.getJSONObject("response").getJSONArray("movements");
            produceBoard();
        } catch (JSONException e) {
            if (active) serviceBoardActivity.produceViewOld();
            e.printStackTrace();
        }
    }

    // To store output data
    class Items implements Comparable<Items> {
        String route;
        String headsign;
        String scheduled;
        String dueTime;
        String platform;
        Date scheduledDate;

        // Constructor sets all scheduled times to well into the future
        Items() {
            scheduledDate = new Date(Long.MAX_VALUE);
        }

        @Override // Allows sorting by scheduled times
        public int compareTo(@NonNull Items o) {
            return scheduledDate.compareTo(o.scheduledDate);
        }
    }

    // Show snackbar and allow refreshing on HTTP failure
    private void handleError(int statusCode) {
        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelOld);
        if (circle == null) {
            Log.e("Early exit", "from handleError in AdvancedApiBoard_private_api class");
            return;
        }
        circle.setVisibility(View.GONE);

        String message = Util.generateErrorMessage(serviceBoardActivity, statusCode);

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
