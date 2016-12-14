package local.hamish.trackbus;

import android.util.Log;
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

class TraditionalApiBoard extends OldBoardParent {

    private JSONArray boardData = null;

    TraditionalApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID) {
        super(serviceBoardActivity, stopID);
    }

    // Refreshes all data
    @Override
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

}
