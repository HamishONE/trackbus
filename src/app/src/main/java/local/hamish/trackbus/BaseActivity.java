package local.hamish.trackbus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class BaseActivity extends AppCompatActivity {

    // Intents
    public final static String EXTRA_TRIP_ID = "local.hamish.trackbus.TRIP_ID";
    public final static String EXTRA_STOP_SEQ = "local.hamish.trackbus.STOP_SEQ";
    public final static String EXTRA_ROUTE = "local.hamish.trackbus.ROUTE";
    public final static String EXTRA_SCH_DATE = "local.hamish.trackbus.SCH_DATE";
    public final static String EXTRA_STOP = "local.hamish.trackbus.STOP_ID";
    public final static String EXTRA_STOP_NAME = "local.hamish.trackbus.STOP_NAME";

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    protected Intent getHamburgerIntent(RecentStops recentStops, MenuItem item) {

        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.go_main:
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.go_all_buses:
                intent = new Intent(this, AllBusesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.go_favs:
                intent = new Intent(this, FavouritesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.go_settings:
                intent = new Intent(this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                break;
            case R.id.go_about:
                intent = new Intent(this, AboutActivity.class);
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
        return intent;
    }

    // Zooms map to current location or CBD if not available
    protected void zoomToLoc(GoogleMap map, float zoom) {

        Location location;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            location = null;
        } else {
            location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true)); //todo: confirm true
        }

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (Util.getAklBounds().contains(latLng)) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                return;
            }
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-36.851, 174.765), 15));
    }

}
