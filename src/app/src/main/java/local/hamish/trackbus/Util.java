package local.hamish.trackbus;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
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

    // Converts time string to Date object using format, assumes NZST/NZDT
    static GregorianCalendar deformatTime(String s, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Pacific/Auckland"));
        try {
            Date date = simpleDateFormat.parse(s);
            GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("Pacific/Auckland"));
            gc.setTime(date);
            return gc;
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
    enum StopType {BUS, TRAIN, FERRY};
    static StopType findStopType(String stopName) {
        if (stopName.contains("Train")) {
            return StopType.TRAIN;
        } else if (stopName.contains("Ferry")) {
            return StopType.FERRY;
        }
        return StopType.BUS;
    }

    // Converts dp to px for a given application context
    static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static LatLngBounds getAklBounds() {
        LatLng northeast = new LatLng(-36.003522, 175.437063);
        LatLng southwest = new LatLng(-37.703045, 174.273933);
        return new LatLngBounds(southwest, northeast);
    }

    // Sets up the map the way we like it
    static void setupMap (Context context, GoogleMap map) {

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json));

        map.setLatLngBoundsForCameraTarget(getAklBounds());
        map.setMinZoomPreference(8F);
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

    static boolean isFavouriteRoute(Context context, String routeName) {

        SQLiteDatabase myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        Cursor resultSet = myDB.rawQuery("SELECT * FROM FavRoutes WHERE route='" + routeName + "'", null);
        boolean isFav = resultSet.getCount() > 0;
        resultSet.close();
        myDB.close();
        return isFav;
    }

    // Adds or removes route from database and array and redraws list
    static void changeFavRoute(Context context, String route) {

        // Query table for selected route
        SQLiteDatabase myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        Cursor resultSet = myDB.rawQuery("SELECT * FROM FavRoutes WHERE route='" + route + "'", null);
        resultSet.moveToFirst();

        // Check if route already exists
        if (resultSet.getCount() == 0) {
            // If not add entry
            myDB.execSQL("INSERT INTO FavRoutes VALUES('" + route + "');");
        } else {
            // If so remove entry
            myDB.execSQL("DELETE FROM FavRoutes WHERE route='" + route + "';");
        }

        resultSet.close();
        myDB.close();
    }

    static String generateErrorMessage(Context context, int statusCode) {

        if (!isNetworkAvailable(context)) {
            return "Internet connection interrupted";
        } else if (statusCode == 0) {
            return "Network error (no response)";
        } else if (statusCode >= 500) {
            return String.format(Locale.US, "Server error (HTTP response %d)", statusCode);
        } else {
            return String.format(Locale.US, "Network error (HTTP response %d)", statusCode);
        }
    }

}