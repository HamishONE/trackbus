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

// This class brings in the traditional real time board using the newer at.govt.nz API
public class DeparturesApiBoard {

    public Items[] items = new Items[1000];
    public int count = 0;
    public boolean active = true;

    private ServiceBoardActivity serviceBoardActivity;
    private JSONArray boardData = null;
    private String stopID;

    // Constructor
    public DeparturesApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;

        // Initialise output array
        for (int i=0; i< items.length; i++) {
            items[i] = new Items();
        }
    }

    // Call API
    public void callAPI() {
        getBoardData(ATApi.data.apiRoot + ATApi.data.departures + stopID + ATApi.getAuthorization());
    }

    // Continues with code after network responds
    public void produceBoard() {
        for (int i=0; i < boardData.length(); i++) {
            try {
                // Export relevant maxxData
                boolean isReal = boardData.getJSONObject(i).getBoolean("monitored");
                String route = boardData.getJSONObject(i).getString("route_short_name");
                String sign = boardData.getJSONObject(i).getString("destinationDisplay");
                String actual = boardData.getJSONObject(i).getString("expectedDepartureTime");
                String scheduled = boardData.getJSONObject(i).getString("scheduledDepartureTime");
                String platform = boardData.getJSONObject(i).getString("departurePlatformName");

                // Clean and format scheduled time
                Date schTime = cleanTime(scheduled);
                DateFormat df = new SimpleDateFormat("hh:mm a");
                String schStr = df.format(schTime);

                // Add general data to arrays
                items[count].route = route;
                items[count].headsign = sign;
                items[count].scheduled = schStr;
                items[count].scheduledDate = schTime;
                items[count].platform = platform;

                //Check if actual due time
                if (isReal && !actual.equals("null")) {
                    // Calculate due time and add to array
                    Date hi1 = cleanTime(actual);
                    Date hi2 = new Date();
                    double dblActual = cleanTime(actual).getTime() - (new Date().getTime());
                    dblActual /= 1000*60;
                    items[count].dueTime = String.valueOf(Math.round(dblActual));
                }
                count++;
            } catch (JSONException e) {e.printStackTrace();}
        }
        if (active) serviceBoardActivity.produceViewOld();
    }

    // Get trip data for one stop
    private void getBoardData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    boardData = jsonObject.getJSONObject("response").getJSONArray("movements");
                    produceBoard();
                } catch (JSONException e) {
                    e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Refreshes all data
    public void updateData() {
        //if (serviceBoardActivity.snackbar != null) serviceBoardActivity.snackbar.dismiss();

        boardData = null;
        count = 0;
        callAPI();
    }

    // Changes string into seconds since epoch
    private Date cleanTime(String string) {
        string = string.substring(0,19); // Remove time zone (GMT)
        string = string.replace('T', '_'); // Otherwise T is processed to mean something
        Date sched = Util.gmtDeformatTime(string, "yyyy-MM-dd_HH:mm:ss");
        return sched;
    }

    // To store output data
    public class Items implements Comparable<Items> {
        String route;
        String headsign;
        String scheduled;
        String dueTime;
        String platform;
        Date scheduledDate;

        // Constructor sets all scheduled times to the epoch
        public Items() {
            scheduledDate = new Date(Long.MAX_VALUE);
        }

        @Override // Allows sorting by scheduled times
        public int compareTo(Items o) {
            return scheduledDate.compareTo(o.scheduledDate);
        }
    }

    // Show snackbar and allow refreshing on HTTP failure
    private void handleError(int statusCode) {
        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelOld);
        if (circle == null) {
            Log.e("Early exit", "from handleError in NewApiBoard class");
            return;
        }
        circle.setVisibility(View.GONE);
        // Prepare message for snackbar
        String message;
        if (!Util.isNetworkAvailable(serviceBoardActivity.getSystemService(Context.CONNECTIVITY_SERVICE)))
            message = "Please connect to the internet";
        else if (statusCode == 0) message = "Network error (no response)";
        else if (statusCode >= 500) message = String.format("AT server error (HTTP response %d)", statusCode);
        else message = String.format("Network error (HTTP response %d)", statusCode);
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