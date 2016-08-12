package local.hamish.trackbus;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdvancedApiBoard_public_api extends AdvancedApiBoard {

    public AdvancedApiBoard_public_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName, boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

    protected String getTripDataUrl() {
        return PublicApiV2.data.apiRoot + PublicApiV2.data.stopInfo + stopID;
    }

    protected String getStopDataUrl() {
        //return PublicApiV2.data.apiRoot + PublicApiV2.data.realtime + "?";
        return PublicApiV2.data.apiRoot + PublicApiV2.data.tripUpdates + "?";
    }
}
