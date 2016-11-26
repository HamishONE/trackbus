package local.hamish.trackbus;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
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
        CombinedApiRequest, RoutesReadyCallback {

    private GoogleMap map;
    private RecentStops recentStops;
    private Vector<Marker> markers = new Vector<>(); //todo: needed?
    private boolean areRoutesDone = false;

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

        // Setup map
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Get data
        CombinedApiBoard combinedApiBoard = new CombinedApiBoard(this, this);
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

        navigationView.getMenu().findItem(R.id.go_all_buses).setChecked(true);
    }

    @Override
    public void done() {

        if (!areRoutesDone) {
            return;
        }

        markers.clear();
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        String sql = "SELECT latitude, longitude, start_time, trip_id, route_short_name FROM LocData INNER JOIN Routes " +
                "ON LocData.route_id = Routes.route_id";
        Cursor resultSet = myDB.rawQuery(sql, null);
        resultSet.moveToFirst();
        for (int i = 0; i < resultSet.getCount(); i++) {
            double latitude = resultSet.getDouble(0);
            double longitude = resultSet.getDouble(1);
            String start_time = resultSet.getString(2);
            String trip_id = resultSet.getString(3);
            String route = resultSet.getString(4);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(route);

            LatLng latLng;
            if (!start_time.equals("")) {
                latLng = new LatLng(latitude, longitude);
            } else {
                latLng = Util.fixTrainLocation(latitude, longitude);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }
            markerOptions.position(latLng);

            Marker marker = map.addMarker(markerOptions);
            marker.setTag(trip_id);

            /*
            if (/*favouritesHelper.isFavRoute(out.get(i).route) false) {
                //marker =  map.addMarker(new MarkerOptions().position(latLng).title(out.get(i).route)
                        //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.heart_icon_pink)));
            } else {
                marker =  map.addMarker(new MarkerOptions().position(latLng).title(route));
            }
            markers.add(marker);
            */


            resultSet.moveToNext();
        }
        resultSet.close();
        myDB.close();
    }

    @Override
    public void routesReady() {
        areRoutesDone = true;
        done();
    }
}
