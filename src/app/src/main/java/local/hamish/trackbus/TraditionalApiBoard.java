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

public abstract class TraditionalApiBoard {

    public Items[] items = new Items[1000];
    public int count = 0;
    public boolean active = true;

    private ServiceBoardActivity serviceBoardActivity;
    private JSONArray boardData = null;
    private String stopID;

    // Constructor
    public TraditionalApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;

        // Initialise output array
        for (int i=0; i< items.length; i++) {
            items[i] = new Items();
        }
    }

    // Call API
    public abstract void callAPI();

    // Continues with code after network responds
    public abstract void produceBoard();

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
