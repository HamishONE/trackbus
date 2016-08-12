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
import java.util.Date;

public abstract class TraditionalApiBoard {

    public Items[] items = new Items[1000];
    public int count = 0;
    public boolean active = true;

    protected ServiceBoardActivity serviceBoardActivity;
    protected JSONArray boardData = null;
    protected String stopID;

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
    protected void getBoardData(String urlString) {
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

    protected abstract void extractJsonArray(String str);

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
