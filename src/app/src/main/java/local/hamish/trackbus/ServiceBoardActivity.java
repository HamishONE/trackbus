package local.hamish.trackbus;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.nullwire.trace.ExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ServiceBoardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Miscellaneous vars
    public String stopID = null;
    public static String stopName = null; // static to support changing fragment header
    private Menu myMenu = null;
    private boolean showTerminating = false;
    static private GoogleMap map;
    private boolean firstTime = true;
    RecentStops recentStops;
    private boolean active = true;

    // Helper objects
    private AdvancedApiBoard newApiBoard;
    private TraditionalApiBoard oldApiBoard;
    public AllBusesHelper allBusesHelper;
    public ArrayList<AdvancedApiBoard.OutputItem> out = new ArrayList<>();

    private ViewPager mViewPager;
    public Snackbar snackbar = null;

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_board);

        ExceptionHandler.register(this, "http://hamishserver.ddns.net/crash_log/");

        // Setup tabs
        DemoCollectionPagerAdapter mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // Setup action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup hamburger menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Extract stopID and stopName
        Intent intent = getIntent();
        stopID = intent.getStringExtra(MainActivity.EXTRA_STOP);
        stopName = intent.getStringExtra(MainActivity.EXTRA_STOP_NAME);

        // Change recent stops
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        recentStops = new RecentStops(myDB, navigationView.getMenu());
        recentStops.addStop(stopID, stopName);
        myDB.close();

        // Setup update button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateData(false);
            }
        });

        if (Util.findStopType(stopName) == Util.StopType.BUS) {
            mViewPager.setCurrentItem(1, false); // Show new data first for buses
        } else {
            mViewPager.setCurrentItem(0, false); // Show old data first for train/ferry
        }

        // Change title
        if (stopName != null) {
            setTitle(stopName);
        } else {
            setTitle("Stop No. " + stopID);
        }

        // Read terminating visibility
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        showTerminating = settings.getBoolean("showTerminating", false);

        //newApiBoard = new AdvancedApiBoard_private_api(this, stopID, stopName, showTerminating, out);
        newApiBoard = new AdvancedApiBoard(this, stopID, stopName, showTerminating, out);
        newApiBoard.callAPIs();

        oldApiBoard = new TraditionalApiBoard(this, stopID);
        oldApiBoard.updateData();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        boolean useMaxx = settings.getBoolean("useMaxx", false);
        if (useMaxx) menu.findItem(R.id.useMaxx).setChecked(true);
        else menu.findItem(R.id.useAT).setChecked(true);

        return true;
    }

    @Override // Update data on restart
    protected void onRestart() {
        super.onRestart();

        oldApiBoard.active = true;
        newApiBoard.active = true;
        updateData(false);
    }

    @Override // On exit prevent helper objects from calling back
    protected void onStop() {
        super.onStop();
        oldApiBoard.active = false;
        newApiBoard.active = false;
        active = false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() != R.id.bus1) {
            startActivity(getHamburgerIntent(recentStops, item));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override // Loads menu bar
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_service_board, menu);
        myMenu = menu;
        changeHeartIcon();

        myMenu.getItem(1).setChecked(showTerminating);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        switch (position) {
                            case 0:
                                myMenu.getItem(1).setVisible(false);
                                myMenu.getItem(2).setVisible(true);
                                myMenu.getItem(3).setVisible(true);
                                break;
                            case 1:
                                myMenu.getItem(1).setVisible(true);
                                myMenu.getItem(2).setVisible(false);
                                myMenu.getItem(3).setVisible(false);
                                break;
                            case 2:
                                myMenu.getItem(1).setVisible(false);
                                myMenu.getItem(2).setVisible(false);
                                myMenu.getItem(3).setVisible(false);
                        }
                    }
                });

        return true;
    }

    @Override // Responds to action bar selection
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favourites:
                changeFavourite();
                return true;

            case R.id.action_terminating:
                changeTerminating();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.useMaxx:
                changeTraditionalSource(true);
                item.setChecked(true);
                return true;

            case R.id.useAT:
                changeTraditionalSource(false);
                item.setChecked(true);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override // Prevents state resetting
    protected void onResume() {
        super.onResume();

        new DoReset().updateTime();
    }

    private void changeTraditionalSource(boolean useMaxx) {

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("useMaxx", useMaxx);
        editor.apply();

        /*if (useMaxx) {
            oldApiBoard = new TraditionalApiBoard_maxx_co_nz(this, stopID);
        } else {
            oldApiBoard = new TraditionalApiBoard_at_govt_nz(this, stopID);
        }*/

        updateData(false);
    }

    // Add current stop to favourites table in database
    private void changeFavourite() {
        FavStopsHelper favStopsHelper = new FavStopsHelper(getApplicationContext(), this, stopID, stopName, null);
        favStopsHelper.changeFavourite();
    }

    // Produces the list view and waits for click
    public void produceView(boolean... b) {
        boolean incStops = true;
        if (b.length > 0) incStops = b[0];

        ListView mListView = (ListView) findViewById(R.id.new_list);
        mListView.setAdapter(new CustomArrayAdapter(this, new String[out.size()])); //todo: listArray is just blank!

        // Remove loading bars
        if (incStops) {
            findViewById(R.id.loadingPanelNew).setVisibility(View.GONE);
            //swipeLayout.setRefreshing(false);
        }

        // Open tracker on item click if location available
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*if (out.tripArray[position] == null) {
                    Toast.makeText(getApplicationContext(), "Location not available", Toast.LENGTH_LONG).show();
                } else*/ { //todo: replace
                    callTracker(out.get(position).trip, out.get(position).stopSequence,
                            out.get(position).route, out.get(position).dateScheduled);
                }
            }
        });

        // Add routes to favourites on long click
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                String route = out.get(pos).route;
                Util.changeFavRoute(getApplicationContext(), route);
                produceView();
                produceViewOld();
                return true;
            }
        });
    }

    // Sets up the all buses view
    public void prepareMap() {

        if (allBusesHelper == null) {

            View circle = findViewById(R.id.loadingPanelMap);
            allBusesHelper = new AllBusesHelper(this, circle, map);
        }

        allBusesHelper.callAPI();
    }

    // Produces the list view and waits for click
    public void produceViewOld() {

        if (Util.findStopType(stopName) != Util.StopType.BUS) {
            Collections.sort(oldApiBoard.items);
        }

        String[] fake = new String[oldApiBoard.items.size()];
        ListView mListView = (ListView) findViewById(R.id.old_list);
        mListView.setAdapter(new OldArrayAdapter(this, fake));

        // Remove loading bars
        findViewById(R.id.loadingPanelOld).setVisibility(View.GONE);
        //swipeLayout.setRefreshing(false);

        // Open tracker on item click if location available
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "Location not available from old data", Toast.LENGTH_LONG).show();
            }
        });

        // Add routes to favourites on long click
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                String route = oldApiBoard.items.get(pos).route;
                Util.changeFavRoute(getApplicationContext(), route);
                produceView();
                produceViewOld();
                return true;
            }
        });
    }

    // Creates intent and starts tracker activity
    public void callTracker(String tripID, int stopSeq, String route, long schDate) {
        Intent intent = new Intent(this, TrackerActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripID);
        intent.putExtra(EXTRA_STOP_SEQ, stopSeq);
        intent.putExtra(EXTRA_ROUTE, route);
        intent.putExtra(EXTRA_STOP, stopID);
        intent.putExtra(EXTRA_SCH_DATE, schDate);
        startActivity(intent);
    }

    // Refreshes data
    public void updateData(boolean fromSnackbar) {

        //if (!active) return;

        if (!fromSnackbar && snackbar != null && snackbar.isShown()) {
            View snackbarView = snackbar.getView();
            Button snackbarActionButton = (Button) snackbarView.findViewById(android.support.design.R.id.snackbar_action);
            snackbarActionButton.performClick();
        }

        findViewById(R.id.loadingPanelMap).setVisibility(View.VISIBLE);
        findViewById(R.id.loadingPanelOld).setVisibility(View.VISIBLE);
        findViewById(R.id.loadingPanelNew).setVisibility(View.VISIBLE);
        newApiBoard.updateData();
        oldApiBoard.updateData();
    }

    // Change to appropriate heart icon
    public void changeHeartIcon() {
        // Open or create database and create favourites table if needed
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Favourites(stopID INTEGER, stopName TEXT, userName TEXT);");
        // Query table for current stop
        Cursor resultSet = myDB.rawQuery("SELECT * FROM Favourites WHERE stopID=" + stopID, null);
        resultSet.moveToFirst();
        // Check if stop exists
        if (resultSet.getCount() != 0) {
            // If so use filled icon
            myMenu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.heart_icon_filled));
        } else {
            // If not use hollow icon
            myMenu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.heart_icon_hollow));
        }
        // Close cursor
        resultSet.close();
        myDB.close();
    }

    // Changes visibility of terminating services
    private void changeTerminating() {
        myMenu.getItem(1).setChecked(showTerminating = !showTerminating);

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("showTerminating", showTerminating);
        editor.apply();

        newApiBoard.changeTerminating(showTerminating);
    }

    // Custom adapter for list
    private class OldArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;

        OldArrayAdapter(Context context, String[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.board_row_layout, parent, false);

            TextView route = (TextView) rowView.findViewById(R.id.col1);
            TextView headsign = (TextView) rowView.findViewById(R.id.col2);
            TextView scheduled = (TextView) rowView.findViewById(R.id.col3);
            TextView due = (TextView) rowView.findViewById(R.id.col4);

            TraditionalApiBoard.Items item = oldApiBoard.items.get(position);

            // Show pier/platform for ferry/train
            if (Util.findStopType(stopName) == Util.StopType.BUS) {
                route.setText(item.route);
            } else {
                route.setText(item.platform);
            }

            headsign.setText(item.headsign);
            scheduled.setText(item.scheduled);
            due.setText(item.dueTime);

            /*
            // Change colour of row if terminating
            if (oldOut.terminatingArray[position]) {
                rowView.setBackgroundColor(0x604D4D4D); //Dark grey
            } else if (oldOut.scheduledArray[position]) {
                textView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            }
            */

            // Check if route is in database and if so show heart icon
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            if (Util.isFavouriteRoute(getApplicationContext(), item.route)) {
                imageView.setImageResource(R.drawable.heart_icon_pink);
            }

            return rowView;
        }
    }

    // Custom adapter for list
    private class CustomArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;

        CustomArrayAdapter(Context context, String[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.board_row_layout, parent, false);

            TextView route = ((TextView) rowView.findViewById(R.id.col1));
            TextView scheduled = ((TextView) rowView.findViewById(R.id.col2));
            TextView dueTime = ((TextView) rowView.findViewById(R.id.col3));
            TextView stopsAway = ((TextView) rowView.findViewById(R.id.col4));

            route.setText(out.get(position).route);
            scheduled.setText(out.get(position).schTime);

            if (!out.get(position).stopsAway.equals("") && Integer.valueOf(out.get(position).stopsAway) < 1) {
                stopsAway.setText("**");
                dueTime.setText(getString(R.string.message_negative_stops_away));
            } else {
                stopsAway.setText(out.get(position).stopsAway);
                dueTime.setText(out.get(position).dueTime);
            }

            // Change colour of row if terminating
            if (out.get(position).isTerminating) {
                rowView.setBackgroundColor(0x604D4D4D); //Dark grey
            } else if (out.get(position).isScheduled) {
                scheduled.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            }

            // Check if route is in database and if so show heart icon
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            String routeName = out.get(position).route;
            String vehicle_id = out.get(position).vehicle_id;

            if (Util.isFavouriteRoute(getApplicationContext(), routeName)) {
                imageView.setImageResource(R.drawable.heart_icon_pink);
            } else if (ATApi.isDoubleDecker(vehicle_id)) {
                imageView.setImageResource(R.drawable.double_decker_bus);
            }

            return rowView;
        }
    }

    // Sets up tabs
    public class DemoCollectionPagerAdapter extends FragmentPagerAdapter {

        String[] titles = {"Older Source", "Newer Source", "Map View"};

        DemoCollectionPagerAdapter(FragmentManager fm) {super(fm);}

        @Override // Links tab with class
        public Fragment getItem(int i) {
            Fragment fragment = null;
            Bundle args = new Bundle();
            if (i==1) {
                fragment = new NewBoardFragment();
                args.putInt(NewBoardFragment.ARG_OBJECT, i + 1);
            } else if (i==0) {
                fragment = new OldBoardFragment();
                args.putInt(OldBoardFragment.ARG_OBJECT, i + 1);
            } else if (i==2) {
                fragment = new MapFragment();
                args.putInt(MapFragment.ARG_OBJECT, i + 1);
            }
            //fragment.setArguments(args);
            return fragment;
        }

        @Override // Provides number of tabs
        public int getCount() {return 3;}

        @Override // Provides page title
        public CharSequence getPageTitle(int position) {return titles[position];}
    }

    // Sets up new api tab
    public static class NewBoardFragment extends Fragment {
        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_api, container, false);
            //Bundle args = getArguments();
            //((TextView) rootView.findViewById(R.id.header)).setText(Integer.toString(args.getInt(ARG_OBJECT)));
            return rootView;
        }
    }

    // Sets up old api tab
    public static class OldBoardFragment extends Fragment implements View.OnClickListener {
        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_old_api, container, false);
            //Bundle args = getArguments();

            // Change column header to pier/platform as appropriate
            switch (Util.findStopType(ServiceBoardActivity.stopName)) {
                case TRAIN:
                    ((TextView) rootView.findViewById(R.id.routeHed)).setText(getString(R.string.train_platform_label));
                    break;
                case FERRY:
                    ((TextView) rootView.findViewById(R.id.routeHed)).setText(getString(R.string.ferry_pier_label));
            }

            /*
            // Setup swipe down
            SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    updateData();
                }
            });
            swipeLayout.setColorSchemeColors(0xFF0000FF, 0xFFFF0000); // Red + Blue (full alpha)
            */

            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.layout_history_selector);
            rl.setOnClickListener(this);

            return rootView;
        }

        @Override
        public void onClick(final View view) {

            final CharSequence shortNames[] = new CharSequence[] {"Live", "Today", "Tomorrow", "<date>"};
            CharSequence longNames[] = new CharSequence[] {"Live (next 6 hours)", "Today", "Tomorrow", "Pick a date..."};

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select timeframe");
            builder.setItems(longNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    TextView tv = (TextView) view.findViewById(R.id.tv_history_selector);
                    tv.setText(shortNames[which]);
                }
            });
            builder.show();
        }
    }

    // Sets up map tab
    public static class MapFragment extends Fragment {
        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_all_buses, container, false);
            //Bundle args = getArguments();
            //((TextView) rootView.findViewById(R.id.header)).setText(Integer.toString(args.getInt(ARG_OBJECT)));

            // Setup map
            ((com.google.android.gms.maps.MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    map = googleMap;
                    Util.setupMap(getActivity(), map);
                }
            });


            return rootView;
        }

    }

    // Zooms to show all markers plus current location on screen
    public void zoomToAll() {

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

        for (Marker marker : Arrays.copyOf(allBusesHelper.markers, allBusesHelper.markLen)) {
            builder.include(marker.getPosition());
        }

        // Trial to mix marshmellow problems
        if (location == null && allBusesHelper.markLen == 0) {
            builder.include(new LatLng(-36.87, 174.79)); //Auckland centre todo: use lib
        }

        LatLngBounds bounds = builder.build();

        int padding = 100; // offset from edges of the map in pixels HAS TO BE LOW FOR COMPATIBILITY
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.moveCamera(cu);

        if (map.getCameraPosition().zoom > 16) {
            map.moveCamera(CameraUpdateFactory.zoomTo(16));
        }
    }

}