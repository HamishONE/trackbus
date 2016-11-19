package local.hamish.trackbus;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    private String[][] stops = new String[10000][4];
    private GoogleMap map = null;
    private Marker[] stopMarkers = new Marker[10000];
    private int len = 0;
    private boolean isVisible = true;
    private JSONArray stopListData;
    private ProgressDialog dialog;
    private RecentStops recentStops;

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup favourites button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toFavourites();
            }
        });

        // Setup hamburger menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //navigationView.getMenu().getItem(0).setChecked(true);
        //navigationView.getMenu().getItem(1).setChecked(false);

        // Check for google play services and prompt download if needed
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, 10);
            dialog.show();
            return;
        }

        // Link to map fragment and setup as desired
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Check if location permission has not yet been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check if permission has been requested before
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }

        ATApi.getDrift();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        Util.setupMap(this, map);
        map.setOnCameraChangeListener(getCameraChangeListener());

        loadStops(false);
        zoomToLoc();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    try {
                        map.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
                zoomToLoc();
                break;
            }
        }
    }

    @Override // Prevents state resetting
    protected void onResume() {
        super.onResume();

        // Setup recent stops
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        recentStops = new RecentStops(myDB, navigationView.getMenu());
        recentStops.readStops();
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.getMenu().getItem(1).setChecked(false);
    }

    // Listens for change in map zoom or location
    public GoogleMap.OnCameraChangeListener getCameraChangeListener() {
        return new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                float zoom = map.getCameraPosition().zoom;
                if (zoom < 15) {
                    if (isVisible) {
                        for (int i = 0; i < len; i++) {stopMarkers[i].setVisible(false);}
                        isVisible = false;
                    }
                } else {
                    findBounds();
                    if (!isVisible) {
                        for (int i = 0; i < len; i++) {stopMarkers[i].setVisible(true);}
                        isVisible = true;
                    }
                }
            }
        };
    }

    @Override // Creates actionbar
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override // On actionbar item selection
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refreshStops) {
            loadStops(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.go_main:
                // do nothing
                break;
            case R.id.go_favs:
                intent = new Intent(this, FavouritesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.bus1:
                intent = new Intent(this, ServiceBoardActivity.class);
                intent.putExtra(MainActivity.EXTRA_STOP, String.valueOf(recentStops.stopIDs[0]));
                intent.putExtra(MainActivity.EXTRA_STOP_NAME, recentStops.stopNames[0]);
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Transfers stopID to service board
    private void moveNext(String stopID, String stopName) {
        Intent intent = new Intent(this, ServiceBoardActivity.class);
        intent.putExtra(EXTRA_STOP, stopID);
        if (!stopName.isEmpty()) intent.putExtra(EXTRA_STOP_NAME, stopName);
        startActivity(intent);
    }

    // Opens the favourites activity
    private void toFavourites() {
        Intent intent = new Intent(this, FavouritesActivity.class);
        startActivity(intent);
    }

    // Adds stops from array to map
    private void fillMap(int length) {
        int i;
        for (i = 0; i < length; i++) {

            String stopID = stops[i][0];
            double lat = Double.valueOf(stops[i][1]);
            double longi = Double.valueOf(stops[i][2]);
            LatLng latLng = new LatLng(lat, longi);
            String name = stops[i][3];

            switch (Util.findStopType(name)) {
                case 0: // Bus
                    stopMarkers[len + i] =  map.addMarker(new MarkerOptions().position(latLng).snippet(name).title(stopID)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    break;
                case 1: // Train
                    stopMarkers[len + i] =  map.addMarker(new MarkerOptions().position(latLng).snippet(name).title(stopID)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    break;
                case 2: // Ferry
                    stopMarkers[len + i] =  map.addMarker(new MarkerOptions().position(latLng).snippet(name).title(stopID)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_for_map_purpul)));

            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker arg0) {
                    String stopID = arg0.getTitle();
                    String stopName = arg0.getSnippet();
                    moveNext(stopID, stopName);
                }
            });

        }
        len += i;
    }

    // Zooms map to current location or CBD if not available
    private void zoomToLoc() {

        Location location;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            location = null;
        } else {
            location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true)); //todo: confirm true
        }

        if (location != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-36.851, 174.765), 15));
        }
    }

    // Find map bounds and adds relevant stops to map
    private void findBounds() {

        VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();

        LatLng upperRight = visibleRegion.farRight;
        LatLng lowerLeft = visibleRegion.nearLeft;

        double minLat = lowerLeft.latitude;
        double maxLat = upperRight.latitude;
        double minLon = lowerLeft.longitude;
        double maxLon = upperRight.longitude;

        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        Cursor resultSet = myDB.rawQuery("SELECT * FROM Stops" +
                " WHERE lat BETWEEN " + String.valueOf(minLat) + " AND " + String.valueOf(maxLat) +
                " AND lon BETWEEN " + String.valueOf(minLon) + " AND " + String.valueOf(maxLon), null);
        resultSet.moveToFirst();

        int i;
        for (i = 0; i < resultSet.getCount(); i++) {
            stops[i][0] = String.valueOf(resultSet.getInt(0));
            stops[i][1] = String.valueOf(resultSet.getDouble(1));
            stops[i][2] = String.valueOf(resultSet.getDouble(2));
            stops[i][3] = resultSet.getString(3);
            resultSet.moveToNext();
        }

        resultSet.close();
        fillMap(i);

    }

    // Checks if database needs to be filled
    private void loadStops(boolean override) {

        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Stops(stopID INTEGER, lat FLOAT, lon FLOAT, stopName TEXT);");

        Cursor resultSet = myDB.rawQuery("SELECT * FROM Stops", null);
        resultSet.moveToFirst();
        int count = resultSet.getCount();
        resultSet.close();

        if (count == 0 || override) {
            String arg1 = !override ? "Loading stops (first run only)" : "Loading stops";
            dialog = ProgressDialog.show(this, arg1, "Please wait", true);

            myDB.execSQL("DELETE FROM Stops;");
            getStops(ATApi.data.apiRoot() + ATApi.data.stops + ATApi.getAuthorization());
        }
    }

    // Loads the stop list from JSON into the database
    private void fillDatabase() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);

                String values = "";
                for (int i = 0; i < stopListData.length(); i++) {
                    try {
                        int location_type = stopListData.getJSONObject(i).getInt("location_type");
                        if (location_type != 0) continue;

                        String stopID = stopListData.getJSONObject(i).getString("stop_id");
                        String lat = stopListData.getJSONObject(i).getString("stop_lat");
                        String lon = stopListData.getJSONObject(i).getString("stop_lon");
                        String stopName = stopListData.getJSONObject(i).getString("stop_name");

                        if (stopID.contains("_")) {
                            int end = stopID.indexOf("_");
                            stopID = stopID.substring(0, end);
                        }
                        values += "(" + stopID + "," + lat + "," + lon + ",'" + stopName.replace("'", "''") + "')";
                        if (i != stopListData.length()-1) values += ",";

                        final int num = i;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                dialog.setMessage("Processing data (" + num*100/stopListData.length() + "%)");
                            }
                        });

                    } catch (JSONException e) {e.printStackTrace();}
                }
                myDB.execSQL("INSERT INTO Stops VALUES" + values + ";");

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        dialog.dismiss();
                        findBounds();
                    }
                });
            }
        };

        //dialog.setTitle("Processing data");
        thread.start();

        //findBounds();
        //dialog.dismiss();
    }

    // Get trip data for one stop
    private void getStops(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    JSONObject jsonObject = new JSONObject(str);

                    stopListData = jsonObject.getJSONArray("response");
                    fillDatabase();
                } catch (JSONException e) {e.printStackTrace();}
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                long progressPercentage = (long)100*bytesWritten/totalSize;
                dialog.setMessage("Downloading from server (" + progressPercentage + "%)");
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
        dialog.dismiss();
        // Prepare message for snackbar
        String message;
        if (!Util.isNetworkAvailable(getSystemService(Context.CONNECTIVITY_SERVICE)))
            message = "Please connect to the internet";
        else if (statusCode == 0) message = "Network error (no response)";
        else if (statusCode >= 500) message = String.format(Locale.US, "AT server error (HTTP response %d)", statusCode);
        else message = String.format(Locale.US, "Network error (HTTP response %d)", statusCode);
        // Show snackbar
        View view = findViewById(R.id.cordLayout);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadStops(true);
            }
        });
        snackbar.show();
    }

}