package local.hamish.trackbus;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// This class brings in the traditional real time board using the newer at.govt.nz API
public class TraditionalApiBoard_at_govt_nz extends TraditionalApiBoard {

    public TraditionalApiBoard_at_govt_nz(ServiceBoardActivity serviceBoardActivity, String stopID) {
        super(serviceBoardActivity, stopID);
    }

    // Call API
    @Override
    public void callAPI() {
        getBoardData(ATApi.data.apiRoot + ATApi.data.departures + stopID + ATApi.getAuthorization());
    }

    // Continues with code after network responds
    @Override
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

    @Override
    protected void extractJsonArray(String str) {
        try {
            JSONObject jsonObject = new JSONObject(str);
            boardData = jsonObject.getJSONObject("response").getJSONArray("movements");
            produceBoard();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Changes string into seconds since epoch
    private Date cleanTime(String string) {
        string = string.substring(0,19); // Remove time zone (GMT)
        string = string.replace('T', '_'); // Otherwise T is processed to mean something
        Date sched = Util.gmtDeformatTime(string, "yyyy-MM-dd_HH:mm:ss");
        return sched;
    }
}