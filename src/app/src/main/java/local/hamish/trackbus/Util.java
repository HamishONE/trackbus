package local.hamish.trackbus;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class Util {

    private Util() {}

    private static long start = System.nanoTime();
    static void printTiming(String message) {
        Log.d("HamishTiming", message + "  [after " + (System.nanoTime()-start)/1000 + "us]");
        //Log.d("HamishTiming", message + "  [after " + (System.nanoTime()-start)/1000000 + "ms]");
        start = System.nanoTime();
    }

    static LatLng fixTrainLocation(double lati, double longi) {

        lati = lati*1.66 + 23.7564;
        longi = longi*1.66 - 114.8370;

        if (lati < -37.091) {
            lati += 0.6639;
        }

        return new LatLng(lati, longi);
    }

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
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json));
    }

    static boolean checkPlayServices(Activity context) {

        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        if (result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(context, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }

    static String beautifyRouteName(String route) {
        switch(route) {
            case "SKY":
                return "SkyBus";
            case "INN":
                return "Inner Link";
            case "CTY":
                return "City Link";
            case "OUT":
                return "Outer Link";
            case "NEX":
                return "Northern Express";
            case "WEST":
                return "Western Line";
            case "STH":
                return "Southern Line";
            case "ONE":
                return "Onehunga Line";
            case "EAST":
                return "Eastern Line";
            case "PUK":
                return "Pukekohe Shuttle";
            default:
                return "Route " + route;
        }
    }

}