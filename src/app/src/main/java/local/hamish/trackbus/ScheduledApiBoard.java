package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

class ScheduledApiBoard extends OldBoardParent {

    private String date;

    ScheduledApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID, String date) {
        super(serviceBoardActivity, stopID);
        this.date = date;
    }

    @Override
    void updateData() {
        getBoardData();
    }

    private void produceBoard(JSONArray data) {

        ArrayList<Items> items_temp = new ArrayList<>();

        for (int i=0; i<data.length(); i++) {
            try {

                Items item = new Items();
                JSONObject vehicle = data.getJSONObject(i);

                item.route = vehicle.getString("route");
                item.headsign = vehicle.getString("destination");
                item.scheduled = vehicle.getString("time");
                item.dueTime = "";
                item.platform = "";

                int hours = Integer.valueOf(item.scheduled.substring(0, 2));
                if (hours >= 24) {
                    item.scheduled = (hours - 24) + item.scheduled.substring(2, 8);
                }

                try {
                    SimpleDateFormat curFormater = new SimpleDateFormat("yyyyMMddHH:mm:ss", Locale.US);
                    item.scheduledDate = curFormater.parse(date + item.scheduled);
                } catch (ParseException e) {e.printStackTrace();}

                items_temp.add(item);

            } catch (JSONException e) {e.printStackTrace();}
        }

        items = items_temp;
        if (active) {
            serviceBoardActivity.produceViewOld();
        }
    }

    private void getBoardData() {

        String urlString = ATApi.getUrl(serviceBoardActivity, ATApi.API.scheduled, null);
        urlString += "?stop_id=" + stopID + "&date=" + date;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    JSONObject jsonObject = new JSONObject(new String(responseBody));
                    JSONArray boardData = jsonObject.getJSONArray("data");
                    produceBoard(boardData);
                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }
}
