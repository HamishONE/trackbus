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

// This class brings in the traditional real time board using the older maxx.co.nz API
public class OldApiBoard {

    public Items[] items = new Items[1000];
    public int count = 0;
    public boolean active = true;

    private ServiceBoardActivity serviceBoardActivity;
    private JSONArray boardData = null;
    private String stopID;

    // Constructor
    public OldApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;

        // Initialise output array
        for (int i=0; i< items.length; i++) {
            items[i] = new Items();
        }
    }

    // Call API
    public void callAPI() {
        getBoardData(ATApi.data.maxxUrl + stopID + "?hours=6");
    }

    // Continues with code after network responds
    public void produceBoard() {

        for (int i=0; i < boardData.length(); i++) {
            try {
                // Export relevant maxxData
                boolean isReal = boardData.getJSONObject(i).getBoolean("Monitored");
                String route = boardData.getJSONObject(i).getString("Route");
                String sign = boardData.getJSONObject(i).getString("DestinationDisplay");
                String actual = boardData.getJSONObject(i).getString("ExpectedDepartureTime");
                String scheduled = boardData.getJSONObject(i).getString("ActualDepartureTime");
                String platform = boardData.getJSONObject(i).getString("DeparturePlatformName");

                // Convert scheduled time to minutes away
                scheduled = scheduled.substring(6,19);
                Date shdDate = new Date(Long.valueOf(scheduled));

                items[count].route = route;
                items[count].headsign = sign;
                DateFormat df = new SimpleDateFormat("hh:mm a");
                items[count].scheduled = df.format(shdDate);
                items[count].scheduledDate = shdDate;
                items[count].platform = platform;

                //Check if real maxxData (not just scheduled)
                if (isReal) {
                    //If so convert actual time to minutes away and print line
                    double dblActual = (Double.valueOf(actual.substring(6,19)) - new Date().getTime())/1000;
                    dblActual /= 60;
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
                    boardData = jsonObject.getJSONArray("Movements");
                    produceBoard();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("boardData error", "");
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
            }
        });
    }

    // Refreshes all data
    public void updateData() {
        boardData = null;
        count = 0;
        callAPI();
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

}