package local.hamish.trackbus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class AllBusesActivity extends BaseActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener,
        CombinedApiRequest, RoutesReadyCallback, FerrysReadyCallback, View.OnClickListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, BearingsReadyCallback, GoogleMap.OnInfoWindowCloseListener,
        GoogleMap.OnInfoWindowLongClickListener {

    private static final String SETTING_SHOW_BUSES = "showBuses";
    private static final String SETTING_SHOW_TRAINS = "showTrains";
    private static final String SETTING_SHOW_FERRYS = "showFerrys";

    private static int refreshRate = 3000; //ms

    private GoogleMap map;
    private RecentStops recentStops;
    private CombinedApiBoard combinedApiBoard;
    private GetFerrys getFerrys;
    private GetBearings getBearings;
    private ArrayList<Marker> mainMarkers = new ArrayList<>();
    private ArrayList<Marker> ferryMarkers = new ArrayList<>();
    private CopyOnWriteArrayList<String> trip_ids = new CopyOnWriteArrayList<>();
    private boolean isVisible = true;
    private String selectedTrip = null;
    private CountDownTimer timer = null;
    private final Object mySync = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Setup layout
        setContentView(R.layout.activity_all_buses);

        // Setup action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup hamburger menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup refresh FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // Setup map
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Get data
        GetRoutes getRoutes = new GetRoutes(this, this);
        getRoutes.updateData();
        combinedApiBoard = new CombinedApiBoard(this, this);
        getFerrys = new GetFerrys(this, this);
        GetBearings.createTable(this);
        getBearings = new GetBearings(this, this);
        onClick(null);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onClick(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_buses, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        switch (item.getItemId()) {

            case R.id.action_bus:
                boolean showBuses = !settings.getBoolean(SETTING_SHOW_BUSES, true);
                editor.putBoolean(SETTING_SHOW_BUSES, showBuses);
                if (showBuses) {
                    item.setIcon(R.drawable.bus_icon_white);
                } else {
                    item.setIcon(R.drawable.bus_icon_grey);
                }
                editor.apply();
                done(true);
                return true;

            case R.id.action_train:
                boolean showTrains = !settings.getBoolean(SETTING_SHOW_TRAINS, true);
                editor.putBoolean(SETTING_SHOW_TRAINS, showTrains);
                if (showTrains) {
                    item.setIcon(R.drawable.train_icon_white);
                } else {
                    item.setIcon(R.drawable.train_icon_grey);
                }
                editor.apply();
                done(true);
                return true;

            case R.id.action_ferry:
                boolean showFerrys = !settings.getBoolean(SETTING_SHOW_FERRYS, true);
                editor.putBoolean(SETTING_SHOW_FERRYS, showFerrys);
                if (showFerrys) {
                    item.setIcon(R.drawable.ferry_icon_white);
                } else {
                    item.setIcon(R.drawable.ferry_icon_grey);
                }
                editor.apply();
                ferrysReady();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        SharedPreferences settings = getPreferences(MODE_PRIVATE);

        boolean showBuses = settings.getBoolean(SETTING_SHOW_BUSES, true);
        MenuItem item = menu.findItem(R.id.action_bus);
        if (showBuses) {
            item.setIcon(R.drawable.bus_icon_white);
        } else {
            item.setIcon(R.drawable.bus_icon_grey);
        }

        boolean showTrains = settings.getBoolean(SETTING_SHOW_TRAINS, true);
        item = menu.findItem(R.id.action_train);
        if (showTrains) {
            item.setIcon(R.drawable.train_icon_white);
        } else {
            item.setIcon(R.drawable.train_icon_grey);
        }

        boolean showFerrys = settings.getBoolean(SETTING_SHOW_FERRYS, true);
        item = menu.findItem(R.id.action_ferry);
        if (showFerrys) {
            item.setIcon(R.drawable.ferry_icon_white);
        } else {
            item.setIcon(R.drawable.ferry_icon_grey);
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() != R.id.go_all_buses) {
            startActivity(getHamburgerIntent(recentStops, item));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        Util.setupMap(this, map);
        zoomToLoc(map, 14);
        map.setOnInfoWindowClickListener(this);
        map.setOnCameraIdleListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnInfoWindowCloseListener(this);
        map.setOnInfoWindowLongClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup recent stops
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        recentStops = new RecentStops(myDB, navigationView.getMenu());
        recentStops.readStops();
        myDB.close();

        navigationView.getMenu().findItem(R.id.go_all_buses).setChecked(true);
    }

    @Override
    public void done(final boolean doReplaceMarkers) {

        synchronized (mySync) {

            final SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
            Cursor resultSet = myDB.rawQuery("SELECT DISTINCT tbl_name from sqlite_master WHERE tbl_name = 'Routes';", null);
            boolean doRoutesExist = resultSet.getCount() > 0;
            resultSet.close();
            if (!doRoutesExist) {
                myDB.close();
                return;
            }

            final ArrayList<Marker> tempMarkers;
            if (doReplaceMarkers) {
                trip_ids.clear(); //todo: use temp trip_id list
                tempMarkers = new ArrayList<>();
            } else {
                tempMarkers = mainMarkers;
            }

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

                    String sql = "SELECT latitude, longitude, start_time, LocData.trip_id, route_short_name," +
                            " Bearings.bearing, LocData.bearing, timestamp, vehicle_id FROM LocData " +
                            "INNER JOIN Routes ON LocData.route_id = Routes.route_id " +
                            "LEFT JOIN Bearings ON LocData.trip_id = Bearings.trip_id " +
                            "WHERE latitude BETWEEN " + minLat + " AND " + maxLat +
                            " AND longitude BETWEEN " + minLon + " AND " + maxLon;
                    Cursor resultSet = myDB.rawQuery(sql, null);

                    resultSet.moveToFirst();
                    for (int i = 0; i < resultSet.getCount(); i++) {

                        SharedPreferences settings = getPreferences(MODE_PRIVATE);
                        boolean showBuses = settings.getBoolean(SETTING_SHOW_BUSES, true);
                        boolean showTrains = settings.getBoolean(SETTING_SHOW_TRAINS, true);

                        double latitude = resultSet.getDouble(0);
                        double longitude = resultSet.getDouble(1);
                        String start_time = resultSet.getString(2);
                        final String trip_id = resultSet.getString(3);
                        final String route = resultSet.getString(4);
                        int bearing = resultSet.getInt(5);
                        int bearing_live = resultSet.getInt(6);
                        final long timestamp = resultSet.getLong(7);
                        final String vehicle_id = resultSet.getString(8);

                        if (bearing_live != 0) {
                            bearing = bearing_live;
                        }

                        boolean isTrain = start_time.equals("");
                        if ((!doReplaceMarkers && trip_ids.contains(trip_id)) || (isTrain && !showTrains) || (!isTrain && (!showBuses || !isVisible))) {
                            resultSet.moveToNext();
                            continue;
                        }

                        final MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.title(Util.beautifyRouteName(route));
                        markerOptions.rotation(bearing);
                        markerOptions.position(new LatLng(latitude, longitude));
                        markerOptions.anchor(0.5F, 0.5F);

                        SetBitmap setBitmap = new SetBitmap(isTrain, route, vehicle_id);
                        Integer height_dp = setBitmap.height_dp;
                        Bitmap vehicleBitmap = setBitmap.vehicleBitmap;

                        Bitmap imageBitmap = vehicleBitmap.copy(vehicleBitmap.getConfig(), true);
                        Canvas canvas = new Canvas(imageBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.WHITE);
                        paint.setTextSize((float) (canvas.getDensity() / 6));
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);

                        int x = canvas.getWidth() / 2;
                        int y = canvas.getHeight() / 2;
                        if (bearing > 180) {
                            canvas.rotate(90, x, y);
                        } else {
                            canvas.rotate(-90, x, y);
                        }

                        x = (canvas.getWidth() / 2);
                        y = (int) (canvas.getHeight() / 2 - (paint.descent() + paint.ascent()) / 2);
                        canvas.drawText(route, x, y, paint);

                        int width = (int) Util.convertDpToPixel(20, getApplicationContext());
                        int height = (int) Util.convertDpToPixel(height_dp, getApplicationContext());
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

                        AllBusesActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Marker marker = map.addMarker(markerOptions);
                                marker.setTag(new Tag(trip_id, route, timestamp));
                                tempMarkers.add(marker);
                                trip_ids.add(trip_id);

                                if (trip_id.equals(selectedTrip)) {
                                    onMarkerClick(marker);
                                    marker.showInfoWindow();
                                    selectedTrip = trip_id;
                                }
                            }
                        });

                        resultSet.moveToNext();
                    }

                    resultSet.close();
                    myDB.close();

                    AllBusesActivity.this.runOnUiThread(new Runnable() {
                        public void run() {

                            if (doReplaceMarkers) {
                                for (Marker marker : mainMarkers) {
                                    marker.remove();
                                }
                                mainMarkers.clear();
                                mainMarkers = tempMarkers;

                            /*
                            //todo: make glob null?
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    int hi = 30;
                                    onClick(null);
                                }
                            }, refreshRate);
                            */
                            }

                            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        }
                    });
                }
            };
            thread.start();

        }
    }

    @Override
    public void onCombinedApiError(int statusCode) {

        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        String message = Util.generateErrorMessage(this, statusCode);
        View view = findViewById(R.id.cordLayout);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        final AllBusesActivity allBusesActivity = this;
        snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allBusesActivity.onClick(null);
            }
        });
        snackbar.show();
    }

    @Override
    public void routesReady() {
        done(true);
    }

    @Override // Refresh button
    public void onClick(View view) {

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        if (settings.getBoolean(SETTING_SHOW_BUSES, true) || settings.getBoolean(SETTING_SHOW_TRAINS, true)) {
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE); //todo: should be one for ferrys too
            combinedApiBoard.updateData();
            getBearings.updateData();
        }
        if (settings.getBoolean(SETTING_SHOW_FERRYS, true)) {
            getFerrys.updateData();
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        Tag tag = (Tag) marker.getTag();
        if (tag == null) return;

        String route = tag.route;
        String tripID = tag.trip_id;

        Intent intent = new Intent(this, TrackerActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripID);
        intent.putExtra(EXTRA_ROUTE, route);
        startActivity(intent);
    }

    @Override
    public void ferrysReady() {

        for (Marker marker : ferryMarkers) {
            marker.remove();
        }

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        if (!settings.getBoolean(SETTING_SHOW_FERRYS, true)) return;

        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        String sql = "SELECT latitude, longitude, vessel FROM Ferrys";
        Cursor resultSet = myDB.rawQuery(sql, null);
        resultSet.moveToFirst();

        for (int i = 0; i < resultSet.getCount(); i++) {

            double latitude = resultSet.getDouble(0);
            double longitude = resultSet.getDouble(1);
            String vessel = resultSet.getString(2);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(vessel);
            markerOptions.position(new LatLng(latitude, longitude));

            int width = (int) Util.convertDpToPixel(20, this);
            int height = (int) Util.convertDpToPixel(20, this);

            Bitmap resizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resizedBitmap);
            Drawable shape = ContextCompat.getDrawable(this, R.drawable.marker_ferry);
            shape.setBounds(0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
            shape.draw(canvas);

            //Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_ferry);
            //Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            Marker marker = map.addMarker(markerOptions);
            marker.setTag(null);
            ferryMarkers.add(marker);

            resultSet.moveToNext();
        }
        resultSet.close();
        myDB.close();
    }

    @Override
    public void onCameraIdle() {

        float zoom = map.getCameraPosition().zoom;
        boolean isVisible = zoom > 12;

        boolean hasChanged = isVisible != this.isVisible;
        this.isVisible = isVisible;
        done(hasChanged);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        final Tag tag = (Tag) marker.getTag();
        if (tag == null) return false;

        selectedTrip = tag.trip_id;
        long diff = System.currentTimeMillis() / 1000 - tag.timestamp;
        marker.setSnippet(diff + "s ago");

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(1000, 20) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (!marker.isInfoWindowShown()) {
                    timer = null;
                    return;
                }
                long diff = System.currentTimeMillis() / 1000 - tag.timestamp;
                marker.setSnippet(diff + "s ago");
                marker.showInfoWindow();
                timer.start();
            }
        }.start();

        return false; //so that default behaviour occurs
    }

    @Override
    public void bearingsReady() {
        //do nothing
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        selectedTrip = null;
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {

        Tag tag = (Tag) marker.getTag();
        Util.changeFavRoute(this, tag.route);
        done(true);
    }

    private Bitmap busBitmap;
    private Bitmap trainBitmap;
    private Bitmap ddBitmap;
    private Bitmap favBusBitmap;
    private Bitmap favTrainBitmap;

    private class SetBitmap {

        Bitmap vehicleBitmap;
        int height_dp;

        SetBitmap(boolean isTrain, String route, String vehicle_id) {

            if (busBitmap == null) {
                busBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_bus_blue);
                trainBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_train_purple);
                ddBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_bus_brown);
                favBusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_bus_red);
                favTrainBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_train_red);
            }

            boolean isFav = Util.isFavouriteRoute(getApplicationContext(), route);
            if (isTrain) {
                height_dp = 50;
                if (isFav) {
                    vehicleBitmap = favTrainBitmap;
                } else {
                    vehicleBitmap = trainBitmap;
                }
            } else {
                height_dp = 40;
                if (isFav) {
                    vehicleBitmap = favBusBitmap;
                } else if (ATApi.isDoubleDecker(vehicle_id)) {
                    vehicleBitmap = ddBitmap;
                } else {
                    vehicleBitmap = busBitmap;
                }
            }
        }
    }

    static private class Tag {

        String trip_id;
        String route;
        long timestamp;

        Tag (String trip_id, String route, long timestamp) {
            this.trip_id = trip_id;
            this.route = route;
            this.timestamp = timestamp;
        }
    }
}
