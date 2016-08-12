package local.hamish.trackbus;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// This class brings in the traditional real time board using the older maxx.co.nz API
public class TraditionalApiBoard_maxx_co_nz extends TraditionalApiBoard {

    public TraditionalApiBoard_maxx_co_nz(ServiceBoardActivity serviceBoardActivity, String stopID) {
        super(serviceBoardActivity, stopID);
    }

    // Call API
    @Override
    public void callAPI() {
        getBoardData(ATApi.data.maxxUrl + stopID + "?hours=6");
    }

    // Continues with code after network responds
    @Override
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

    @Override
    protected void extractJsonArray(String str) {
        try {
            JSONObject jsonObject = new JSONObject(str);
            boardData = jsonObject.getJSONArray("Movements");
            produceBoard();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("boardData error", "");
        }
    }

}