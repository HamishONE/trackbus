package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdvancedApiBoard_public_api extends AdvancedApiBoard {

    public AdvancedApiBoard_public_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName,
                                       boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

    @Override
    public void callAPIs() {
        getRealtimeData(PublicApiV2.data.apiRoot + PublicApiV2.data.realtime);
    }

    @Override
    public void changeTerminating(boolean showTerminating) {

    }

    private void produceBoard(JSONObject jsonObject) {
        try {
            JSONArray realTimeData = jsonObject.getJSONObject("response").getJSONArray("entity");

            realTimeData = null;
        }
        catch(JSONException e) {e.printStackTrace();}
    }

    // Get trip data for one stop
    private void getRealtimeData(final String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(PublicApiV2.data.headerTerm, PublicApiV2.data.primaryKey);
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);
                    produceBoard(jsonObject);
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
