package local.hamish.trackbus;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Vector;

public class AllBusesActivity extends BaseActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener,
        CombinedApiRequest, RoutesReadyCallback, View.OnClickListener, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap map;
    private RecentStops recentStops;
    //private Vector<Marker> markers = new Vector<>();
    private boolean areRoutesDone = false;
    private CombinedApiBoard combinedApiBoard;

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
        combinedApiBoard = new CombinedApiBoard(this, this);
        combinedApiBoard.updateData();
        GetRoutes getRoutes = new GetRoutes(this, this);
        getRoutes.updateData();
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
        zoomToLoc(map);
        map.setOnInfoWindowClickListener(this);
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
    public void done() {

        if (!areRoutesDone) {
            return;
        }

        map.clear();

        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        String sql = "SELECT latitude, longitude, start_time, trip_id, route_short_name, bearing FROM LocData INNER JOIN Routes " +
                "ON LocData.route_id = Routes.route_id";
        Cursor resultSet = myDB.rawQuery(sql, null);
        resultSet.moveToFirst();
        for (int i = 0; i < resultSet.getCount(); i++) {
            double latitude = resultSet.getDouble(0);
            double longitude = resultSet.getDouble(1);
            String start_time = resultSet.getString(2);
            String trip_id = resultSet.getString(3);
            String route = resultSet.getString(4);
            int bearing = resultSet.getInt(5);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(route);
            markerOptions.rotation(bearing);

            int img_id;
            int height_dp;
            if (!start_time.equals("")) {
                markerOptions.position(new LatLng(latitude, longitude));
                img_id = R.drawable.marker_bus_blue;
                height_dp = 40;
            } else {
                markerOptions.position(Util.fixTrainLocation(latitude, longitude));
                img_id = R.drawable.marker_train_purple;
                height_dp = 50;
            }

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inMutable = true;
            Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), img_id, opt);

            Canvas canvas = new Canvas(imageBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(/*Util.convertDpToPixel(30, this)*/ (float)(canvas.getDensity()/6));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            int x = canvas.getWidth()/2;
            int y = canvas.getHeight()/2;
            if (bearing > 180) {
                canvas.rotate(90, x, y);
            } else {
                canvas.rotate(-90, x, y);
            }

            x = (canvas.getWidth() / 2);
            y = (int) (canvas.getHeight()/2 - (paint.descent() + paint.ascent())/2);
            canvas.drawText(route, x, y, paint);

            int width = (int) Util.convertDpToPixel(20, this);
            int height = (int) Util.convertDpToPixel(height_dp, this);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            Marker marker = map.addMarker(markerOptions);
            marker.setTag(trip_id);

            resultSet.moveToNext();
        }
        resultSet.close();
        myDB.close();

        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    @Override
    public void routesReady() {
        areRoutesDone = true;
        done();
    }

    @Override // Refresh button
    public void onClick(View view) {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        combinedApiBoard.updateData();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String route = marker.getTitle();
        String tripID = (String) marker.getTag();

        int stopSeq = 100;
        String stopID = null;
        long schDate = 400;

        Intent intent = new Intent(this, TrackerActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripID);
        intent.putExtra(EXTRA_STOP_SEQ, stopSeq);
        intent.putExtra(EXTRA_ROUTE, route);
        intent.putExtra(EXTRA_STOP, stopID);
        intent.putExtra(EXTRA_SCH_DATE, schDate);
        startActivity(intent);
    }
}
