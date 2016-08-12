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

public class AdvancedApiBoard_private_api extends AdvancedApiBoard {

    public AdvancedApiBoard_private_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName, boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

    protected String getTripDataUrl() {
        return ATApi.data.apiRoot + ATApi.data.stopInfo + stopID + ATApi.getAuthorization();
    }

    protected String getStopDataUrl() {
        return ATApi.data.apiRoot + ATApi.data.tripUpdates + ATApi.getAuthorization() + "&";
    }
}