package local.hamish.trackbus;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

public class TrackerActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener  {

    private static final int timeDelay = 2000; //ms

    private JSONArray newData;
    private JSONArray pointData;
    private String tripID;
    private Double lat = 0.0;
    private Double longi = 0.0;
    private int stopSeq;
    private TextView tvStops;
    private TextView tvDue;
    private TextView tvTimestamp;
    private GoogleMap map;
    private boolean isFirstTime = true;
    private boolean showTraffic = false;
    private RecentStops recentStops;
    private Marker marker;
    private double bearing = 0.0;
    private long schTime;
    private Snackbar snackbar;
    private String title;
    private boolean showNotification = false;
    private boolean showNotificationChecked;
    private String mapUrl;
    private NotificationManager mNotifyMgr;
    private boolean active = true;
    static boolean notificationDismissed = false;

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Setup action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup hamburger menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Change recent stops
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        recentStops = new RecentStops(myDB, navigationView.getMenu());
        recentStops.readStops();

        // Link to header TextViews
        tvStops = (TextView) findViewById(R.id.stop_info);
        tvDue = (TextView) findViewById(R.id.due_info);
        tvTimestamp = (TextView) findViewById(R.id.timestamp_info);

        // Get tripID and stopSeq from intent
        Intent intent = getIntent();
        tripID = intent.getStringExtra(ServiceBoardActivity.EXTRA_TRIP_ID);
        stopSeq = intent.getIntExtra(ServiceBoardActivity.EXTRA_STOP_SEQ, -1);
        schTime = intent.getLongExtra(ServiceBoardActivity.EXTRA_SCH_DATE, -1);
        String route = intent.getStringExtra(ServiceBoardActivity.EXTRA_ROUTE);
        String stopID = intent.getStringExtra(ServiceBoardActivity.EXTRA_STOP);

        // Set activity title using special names if possible
        if (route.equals("SKY")) {
            title = "SkyBus";
        } else if (route.equals("INN")) {
            title = "Inner Link";
        } else if (route.equals("CTY")) {
            title = "City Link";
        } else if (route.equals("OUT")) {
            title = "Outer Link";
        } else if (route.equals("NEX")) {
            title = "Northern Express";
        } else {
            title = "Route " + route;
        }
        setTitle(title);

        // Link to map fragment and show location if allowed
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);

        // Add stop location to map
        Cursor resultSet = myDB.rawQuery("SELECT lat, lon FROM Stops WHERE stopID = " + stopID, null);
        resultSet.moveToFirst();
        double lat = resultSet.getDouble(0);
        double lon = resultSet.getDouble(1);
        resultSet.close();
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));

        // Call API
        callApi();
    }

    @Override // Creates action bar
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracker, menu);
        return true;
    }

    @Override // On action bar item selection
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_show_traffic) {
            map.setTrafficEnabled(showTraffic = !showTraffic);
            item.setChecked(showTraffic);
            return true;
        } else if (id == R.id.action_show_notification) {
            item.setChecked(showNotification = !showNotification);
            showNotificationChecked = showNotification;

            mapUrl = getMapUrl(lat, longi);
            new NotificationTask().execute();

            return true;
        } else if (id == android.R.id.home) {
            //Call the back button's method
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.go_main:
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.go_favs:
                intent = new Intent(this, FavouritesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.bus1:
                onBackPressed();
                break;
            case R.id.bus2:
                intent = new Intent(this, ServiceBoardActivity.class);
                intent.putExtra(MainActivity.EXTRA_STOP, String.valueOf(recentStops.stopIDs[1]));
                intent.putExtra(MainActivity.EXTRA_STOP_NAME, recentStops.stopNames[1]);
                break;
            case R.id.bus3:
                intent = new Intent(this, ServiceBoardActivity.class);
                intent.putExtra(MainActivity.EXTRA_STOP, String.valueOf(recentStops.stopIDs[2]));
                intent.putExtra(MainActivity.EXTRA_STOP_NAME, recentStops.stopNames[2]);
                break;
            case R.id.bus4:
                intent = new Intent(this, ServiceBoardActivity.class);
                intent.putExtra(MainActivity.EXTRA_STOP, String.valueOf(recentStops.stopIDs[3]));
                intent.putExtra(MainActivity.EXTRA_STOP_NAME, recentStops.stopNames[3]);
                break;
            case R.id.bus5:
                intent = new Intent(this, ServiceBoardActivity.class);
                intent.putExtra(MainActivity.EXTRA_STOP, String.valueOf(recentStops.stopIDs[4]));
                intent.putExtra(MainActivity.EXTRA_STOP_NAME, recentStops.stopNames[4]);
                break;
        }
        if (intent!=null) startActivity(intent);

        stopCode();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override // Prevents state resetting
    protected void onResume() {
        super.onResume();

        if (new DoReset().doReset()) startActivity(new Intent(this, MainActivity.class));

        active = true;
        showNotification = showNotificationChecked;

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(false);
        navigationView.getMenu().getItem(1).setChecked(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        new DoReset().updateTime();
    }

    void stopCode() {
        active = false;
        mNotifyMgr.cancelAll();
        showNotification = false;
    }

    // Calls APIs
    private void callApi() {

        getPointData(ATApi.data.apiRoot() + ATApi.data.shapeByTripId + tripID + ATApi.getAuthorization());
        getRealtimeData(ATApi.data.apiRoot() + ATApi.data.realtime + ATApi.getAuthorization() + "&tripid=" + tripID);
    }

    // Calls the API regularly and redraws map
    private void main() {

        if (!active) return;

        // Remove loading bar
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);

        double lat2 = 0.0;
        double long2 = 0.0;
        int stopsAway = 0;
        int secsAgo = 0;
        int delay = 0;
        JSONObject stopDict = null;
        JSONObject locDict = null;
        Double bearingNew = null;

        try {
            // Get JSON Arrays
            stopDict = newData.getJSONObject(0);
            locDict = newData.getJSONObject(1);

            // Retrieve and calculate values
            lat2 = locDict.getJSONObject("vehicle").getJSONObject("position").getDouble("latitude");
            long2 = locDict.getJSONObject("vehicle").getJSONObject("position").getDouble("longitude");
            stopsAway = stopSeq - stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update")
                    .getInt("stop_sequence");
            int timestamp = locDict.getJSONObject("vehicle").getInt("timestamp");
            secsAgo = (int) (new Date().getTime() / 1000) - timestamp;

            //** MUST BE LAST **//
            bearingNew = locDict.getJSONObject("vehicle").getJSONObject("position").getDouble("bearing");

        } catch (JSONException e) {e.printStackTrace();}

        try {
            delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update")
                    .getJSONObject("arrival").getInt("delay");
        } catch (JSONException e) {
            try {
                delay = stopDict.getJSONObject("trip_update").getJSONObject("stop_time_update")
                        .getJSONObject("departure").getInt("delay");
            } catch (JSONException f) {f.printStackTrace();}
        }

        String notiText = "";

        // Set timestamp header
        String timestamp = String.format("%2ds ago", secsAgo);
        tvTimestamp.setText(timestamp);
        notiText += timestamp + "\n";

        // Set due time header
        long dueSec = schTime + delay - new Date().getTime()/1000;
        if (dueSec < 60) {
            tvDue.setText("Due: *");
            notiText += "Due: *" + "\n";
        } else {
            tvDue.setText("Due: " + String.valueOf(dueSec/60) + "'");
            notiText += "Due: " + String.valueOf(dueSec/60) + "'" + "\n";
        }

        // Set stops away and stop thread if bus departed
        if (stopsAway < 1) {
            tvStops.setText("Stops: *");
            notiText += "Stops: *";
            if (stopsAway < 0) return;
        } else {
            tvStops.setText("Stops: " + Integer.toString(stopsAway));
            notiText += "Stops: " + Integer.toString(stopsAway);
        }

        //if (showNotification) showNotification(notiText);

        // If location has changed create new marker
        if ((lat == null) || (lat2 != lat) || (long2 != longi) || (bearingNew != null && bearing != bearingNew)) {
            lat = lat2;
            longi = long2;
            if (bearingNew != null) bearing = bearingNew;
            LatLng latLng = new LatLng(lat, longi);
            drawMap(latLng);
        }

        // Call API again after 5 seconds has passed
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                getRealtimeData(ATApi.data.apiRoot() + ATApi.data.realtime + ATApi.getAuthorization() + "&tripid=" + tripID);
            }
        }, timeDelay);
    }

    // Moves the market and repositions map
    private void drawMap(LatLng latLng) {

        if (marker != null) marker.remove();
        marker = map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_arrow)).rotation((float) bearing));

        if (isFirstTime) {
            isFirstTime = false;
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(14.0f).build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            map.moveCamera(cameraUpdate);
        }

        if (notificationDismissed) {
            notificationDismissed = false;
            showNotification = false;
            showNotificationChecked = false;
            Log.e("Notification", "Dismissed successfully!!");
            return;
        }

        if (showNotification) {
            mapUrl = getMapUrl(latLng.latitude, latLng.longitude);
            new NotificationTask().execute();
        }
    }

    // Draws bus route onto map
    private void drawRoute() {

        double pointLat = 0.0;
        double pointLong = 0.0;
        LatLng[] routePoints = new LatLng[10000];
        int j;
        double dist;
        double distMax = 1000;
        int len = -1;

        for (j = 0; j < pointData.length(); j++) {
            try {
                pointLat = pointData.getJSONObject(j).getDouble("shape_pt_lat");
                pointLong = pointData.getJSONObject(j).getDouble("shape_pt_lon");
            } catch (JSONException e) {e.printStackTrace();}

            if (len != -1) {
                distMax = 1000;
                for (int i = 0; i <= len; i++) {
                    dist = Math.sqrt(Math.pow(pointLat - routePoints[i].latitude, 2)
                            + Math.pow(pointLong - routePoints[i].longitude, 2)); // in degrees
                    dist *= 60 * 1.85 * 1000; // in metres
                    if (dist < distMax) distMax = dist;
                }
            }
            if (distMax > 0.001) routePoints[++len] = new LatLng(pointLat, pointLong);
        }

        map.addPolyline(new PolylineOptions()
                .add(Arrays.copyOf(routePoints, len+1))
                .width(5)
                .color(Color.RED));
    }

    // Get realtime data for trip
    private void getRealtimeData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    newData = jsonObject.getJSONObject("response").getJSONArray("entity");
                    main();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                handleError(statusCode);
            }
        });
    }

    // Show snackbar and allow refreshing on HTTP failure
    private void handleError(int statusCode) {

        final View circle = findViewById(R.id.loadingPanel);
        if (circle == null) {
            Log.e("Early exit", "from handleError in TrackerActivity class");
            return;
        }
        circle.setVisibility(View.GONE);
        // Prepare message for snackbar
        String message;
        if (!Util.isNetworkAvailable(getSystemService(Context.CONNECTIVITY_SERVICE)))
            message = "Please connect to the internet";
        else if (statusCode == 0) message = "Network error (no response)";
        else if (statusCode >= 500) message = String.format("AT server error (HTTP response %d)", statusCode);
        else message = String.format("Network error (HTTP response %d)", statusCode);
        // Show snackbar
        if (snackbar != null && snackbar.isShown()) return;
        View view = findViewById(R.id.cordLayoutTracker);
        snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                circle.setVisibility(View.VISIBLE);
                getRealtimeData(ATApi.data.apiRoot() + ATApi.data.realtime + ATApi.getAuthorization() + "&tripid=" + tripID);
            }
        });
       snackbar.show();
   }

    // Get points of bus route
    private void getPointData(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    pointData = jsonObject.getJSONArray("response");
                    drawRoute();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP Error", statusCode + " " + error.getMessage());
                // Todo: Implement way for user to know and relaunch
            }
        });
    }

    public static String getMapUrl(Double lat, Double lon) {
        final String coordPair = lat + "," + lon;
        int width = 400, height = 400;
        return "http://maps.googleapis.com/maps/api/staticmap?"
                + "&zoom=17"
                + "&size=" + width + "x" + height
                + "&maptype=roadmap&sensor=true"
                + "&center=" + coordPair
                + "&markers=color:red|" + coordPair;
    }

    private class NotificationTask extends AsyncTask<URL, Integer, Long> {
        protected Long doInBackground(URL... urls) {
            Bitmap snapshot = null;

            try {
                URL url = new URL(mapUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream in = connection.getInputStream();
                snapshot = BitmapFactory.decodeStream(in);
            } catch (IOException e) {e.printStackTrace();}

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.at_logo)
                            .setContentTitle(title)
                            //.setContentText("test")
                            .setDeleteIntent(createOnDismissedIntent(getApplicationContext(), 47))
                            .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(snapshot));

            mNotifyMgr.notify(47, mBuilder.build());

            return new Long(0);
        }
    }

    /*// Zooms to show all markers plus current location on screen todo: IMPLEMENT
    private void zoomToAll() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        for (Marker marker : Arrays.copyOf(markers, markLen)) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        int padding = 400; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.moveCamera(cu);

        if (map.getCameraPosition().zoom > 16) {
            map.moveCamera(CameraUpdateFactory.zoomTo(16));
        }
    }*/

    public static class NotificationDismissedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getExtras().getInt("local.hamish.trackbus.notificationId");
            if (notificationId == 47) notificationDismissed = true;
        }
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationDismissedReceiver.class);
        intent.putExtra("local.hamish.trackbus.notificationId", notificationId);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context.getApplicationContext(),
                        notificationId, intent, 0);
        return pendingIntent;
    }

}