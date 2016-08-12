package local.hamish.trackbus;

import android.util.Log;
import java.util.GregorianCalendar;

public class DoReset {

    static long lastExit = 0L;

    public void updateTime() {
        lastExit = GregorianCalendar.getInstance().getTimeInMillis();
    }

    public boolean doReset() {
        if (lastExit == 0L) return false;
        long currentTime = GregorianCalendar.getInstance().getTimeInMillis();
        long diff = currentTime - lastExit;
        Log.e("DoReset", "diff = " + (double)diff/1000/60 + "minutes");
        if (diff > 60*60*1000) { //60 minutes
            return true;
        }
        else return false;
    }
}
