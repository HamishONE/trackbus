package local.hamish.trackbus;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class Util {

    private Util() {}

    // Converts time string to Date object using format, assumes NZST/NZDT
    public static Date deformatTime(String s, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Pacific/Auckland"));
        try {
            return simpleDateFormat.parse(s);
        }
        catch (ParseException ex) {
            System.out.println("Exception "+ex);
            return null;
        }
    }

    // Converts time string to Date object using format, assumes GMT/UTC
    public static Date gmtDeformatTime(String s, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return simpleDateFormat.parse(s);
        }
        catch (ParseException ex) {
            System.out.println("Exception "+ex);
            return null;
        }
    }

    // Emulator for CryptoJS Hmac.SHA1
    public static String hmacSHA1(String value, String key) {
        String type = "HmacSHA1";
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(type);
            javax.crypto.spec.SecretKeySpec secret = new javax.crypto.spec.SecretKeySpec(key.getBytes(), type);
            mac.init(secret);
            byte[] digest = mac.doFinal(value.getBytes());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            String s;
            for (byte b : digest) {
                s = Integer.toHexString(b & 0xFF);
                if (s.length() == 1) sb.append('0');
                sb.append(s);
            }
            return sb.toString();
        } catch (Exception e) {
            android.util.Log.v("TAG", "Exception [" + e.getMessage() + "]", e);
        }
        return "";
    }

    // Returns 1 or 2 if train or ferry
    public static int findStopType(String stopName) {
        if (stopName.contains("Train")) {
            return 1;
        } else if (stopName.contains("Ferry")) {
            return 2;
        }
        return 0;
    }

    // Converts dp to px for a given application context
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    //Call like this: out = Util.isNetworkAvailable(getSystemService(Context.CONNECTIVITY_SERVICE))
    public static boolean isNetworkAvailable(Object systemService) {
        ConnectivityManager connectivityManager = (ConnectivityManager) systemService;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}