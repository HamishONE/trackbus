package local.hamish.trackbus;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.bluelinelabs.logansquare.annotation.OnJsonParseComplete;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nullwire.trace.ExceptionHandler;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final float INITIAL_ZOOM = 17;

    private String[][] stops = new String[10000][4];
    private GoogleMap map = null;
    private Marker[] stopMarkers = new Marker[10000];
    private int len = 0;
    private boolean isVisible = true;
    private Response response;
    private ProgressDialog dialog;
    private RecentStops recentStops;

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExceptionHandler.register(this, "http://hamishserver.ddns.net/crash_log/");

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
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //navigationView.getMenu().getItem(0).setChecked(true);
        //navigationView.getMenu().getItem(1).setChecked(false);

        // Check for google play services and prompt download if needed
        if (!Util.checkPlayServices(this)) return;

        // Link to map fragment and setup as desired
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Check if location permission has not yet been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        ATApi.getDrift();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        Util.setupMap(this, map);
        map.setOnCameraIdleListener(getCameraChangeListener());

        loadStops(false);
        zoomToLoc(map, INITIAL_ZOOM);
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
                zoomToLoc(map, INITIAL_ZOOM);
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
        myDB.close();

        navigationView.getMenu().findItem(R.id.go_main).setChecked(true);
    }

    // Listens for change in map zoom or location
    public GoogleMap.OnCameraIdleListener getCameraChangeListener() {
        return new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                float zoom = map.getCameraPosition().zoom;
                if (zoom < 14) {
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() != R.id.go_main) {
            startActivity(getHamburgerIntent(recentStops, item));
        }

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

    // Find map bounds and adds relevant stops to map
    private void findBounds() {

        VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
        final LatLng upperRight = visibleRegion.farRight;
        final LatLng lowerLeft = visibleRegion.nearLeft;

        Thread thread = new Thread() {
            @Override
            public void run() {

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
                myDB.close();

                final int i_f = i;
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        fillMap(i_f);
                    }
                });
            }
        };
        thread.start();

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
            getStops(ATApi.getUrl(ATApi.API.stops, null));
        }

        myDB.close();
    }

    // Loads the stop list from JSON into the database
    private void fillDatabase() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);

                String values = "";
                for (int i = 0; i < response.response.size(); i++) {
                    try {
                        Stop stop = response.response.get(i);
                        if (stop.location_type != 0) continue;

                        if (stop.stop_id.contains("_")) {
                            int end = stop.stop_id.indexOf("_");
                            stop.stop_id = stop.stop_id.substring(0, end);
                        }
                        values += "(" + stop.stop_id + "," + stop.stop_lat + "," + stop.stop_lon + ",'" +
                                stop.stop_name.replace("'", "''") + "')";
                        if (i != response.response.size()-1) values += ",";

                        final int num = i;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                dialog.setMessage("Processing data (" + num*100/response.response.size() + "%)");
                            }
                        });

                    } catch (Exception e) {e.printStackTrace();}
                }
                myDB.execSQL("INSERT INTO Stops VALUES" + values + ";");
                myDB.close();

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        dialog.dismiss();
                        findBounds();
                    }
                });
            }
        };
        thread.start();
    }

    // Get trip data for one stop
    private void getStops(String urlString) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(urlString, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String str = new String(responseBody);
                    response = LoganSquare.parse(str, Response.class);

                    fillDatabase();

                } catch (IOException e) {e.printStackTrace();}
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                //long progressPercentage = (long)100*bytesWritten/totalSize;
                dialog.setMessage("Downloading from server (" + bytesWritten*100/4670000 + "%)");
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

@JsonObject
class Stop {

    @JsonField
    public int location_type;

    @JsonField
    public String stop_id;

    @JsonField
    public String stop_lat;

    @JsonField
    public String stop_lon;

    @JsonField
    public String stop_name;

    @OnJsonParseComplete
    void onParseComplete() {

        String hi = this.stop_name;
    }

}

@JsonObject
class Response {

    @JsonField
    public List<Stop> response;

}