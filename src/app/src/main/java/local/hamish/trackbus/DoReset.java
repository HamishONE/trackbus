package local.hamish.trackbus;

import android.util.Log;
import java.util.GregorianCalendar;

class DoReset {

    private static long lastExit = 0L;

    void updateTime() {
        lastExit = GregorianCalendar.getInstance().getTimeInMillis();
    }

    boolean doReset() {
        if (lastExit == 0L) return false;
        long currentTime = GregorianCalendar.getInstance().getTimeInMillis();
        long diff = currentTime - lastExit;
        Log.e("DoReset", "diff = " + (double)diff/1000/60 + "minutes");
        return (diff > 60*60*1000); //60 minutes
    }
}
