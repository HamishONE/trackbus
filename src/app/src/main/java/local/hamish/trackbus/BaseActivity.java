package local.hamish.trackbus;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity  extends AppCompatActivity {

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
    }

}
