package local.hamish.trackbus;

import android.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class Util {

    private Util() {}

    // Converts time string to Date object using format, assumes NZST/NZDT
    static Date deformatTime(String s, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.US);
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
    static Date gmtDeformatTime(String s, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.US);
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
    static String hmacSHA1(String value, String key) {
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
    static int findStopType(String stopName) {
        if (stopName.contains("Train")) {
            return 1;
        } else if (stopName.contains("Ferry")) {
            return 2;
        }
        return 0;
    }

    // Converts dp to px for a given application context
    static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    //Call like this: out = Util.isNetworkAvailable(getSystemService(Context.CONNECTIVITY_SERVICE))
    static boolean isNetworkAvailable(Object systemService) {
        ConnectivityManager connectivityManager = (ConnectivityManager) systemService;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Sets up the map the way we like it
    static void setupMap (Context context ,GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
    }

}