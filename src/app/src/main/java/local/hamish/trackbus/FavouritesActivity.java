package local.hamish.trackbus;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.mobeta.android.dslv.DragSortListView;
import java.util.Arrays;

public class FavouritesActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    public int listCount = 0;
    public String[] stopArray = new String[100];
    public String[] nameArray = new String[100];
    public String[] userNameArray = new String[100];
    private RecentStops recentStops;
    private FavouritesArrayAdapter mAdapter;

    @Override // On activity creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        // Setup action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup drag callbacks
        final DragSortListView lv = (DragSortListView) findViewById(R.id.list);
        lv.setDropListener(mDropListener);
        lv.setRemoveListener(mRemoveListener);
        lv.setDragScrollProfile(mDragScrollProfile);

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

        readFavourites();
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
                //do nothing
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

    @Override // Creates action bar
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favourites, menu);
        return true;
    }

    @Override // On action bar item selection
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_favourites) {
            deleteAll();
            return true;
        } else if (id == android.R.id.home) {
            //Call the back button's method
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Deletes all favourites
    private void deleteAll() {
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        myDB.execSQL("DELETE FROM Favourites;");
        // Reset view
        listCount = 0;
        readFavourites();
    }

    @Override // Reset state on resume
    protected void onResume() {
        super.onResume();

        listCount = 0;
        readFavourites();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(false);
        navigationView.getMenu().getItem(1).setChecked(true);
    }

    // Reads from favourites table in database into array
    public void readFavourites() {

        // Open or create database and create favourites table if needed
        SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Favourites(stopID INTEGER ,stopName TEXT, userName TEXT);");

        Cursor resultSet = myDB.rawQuery("SELECT * FROM Favourites", null);
        resultSet.moveToFirst();
        if (resultSet.getCount() == 0)
            Toast.makeText(getApplicationContext(), "No favourites saved", Toast.LENGTH_LONG).show();

        int i;
        for (i = 0; i < resultSet.getCount(); i++) {
            stopArray[i] = String.valueOf(resultSet.getInt(0));
            nameArray[i] = resultSet.getString(1);
            userNameArray[i] = resultSet.getString(2);
            resultSet.moveToNext();
        }
        listCount = i;

        resultSet.close();
        produceView();
    }

    // Produces the list view and waits for click
    private void produceView() {
        ListView mListView = (ListView) findViewById(R.id.list);
        mAdapter = new FavouritesArrayAdapter(this, Arrays.copyOf(stopArray, listCount));
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                moveNext(stopArray[position], nameArray[position]);
            }
        });

        final FavouritesActivity favouritesActivity = this;
        final SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);

        // Change userName on long click
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                String stopID = stopArray[pos];
                String stopName = nameArray[pos];
                String oldUserName = userNameArray[pos];
                FavStopsHelper favStopsHelper = new FavStopsHelper(getApplicationContext(), myDB, null, stopID,
                        stopName, favouritesActivity);
                favStopsHelper.showDialog(pos, oldUserName);
                return true;
            }
        });
    }

    // Transfers stopID to service board
    private void moveNext(String stopID, String stopName) {
        Intent intent = new Intent(this, ServiceBoardActivity.class);
        intent.putExtra(EXTRA_STOP, stopID);
        if (!stopName.isEmpty()) intent.putExtra(EXTRA_STOP_NAME, stopName);
        startActivity(intent);
    }

    // Custom adapter for list
    private class FavouritesArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;

        FavouritesArrayAdapter(Context context, String[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.favourites_row_layout, parent, false);

            TextView stopID = ((TextView) rowView.findViewById(R.id.stop_number));
            TextView stopName = ((TextView) rowView.findViewById(R.id.stop_name));
            TextView userName = ((TextView) rowView.findViewById(R.id.user_name));

            stopID.setText(stopArray[position]);
            stopName.setText(nameArray[position]);
            userName.setText(userNameArray[position]);

            return rowView;
        }

        @Override
        public void remove(String stopID) {
            SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
            myDB.execSQL("CREATE TABLE IF NOT EXISTS Favourites(stopID INTEGER ,stopName TEXT, userName TEXT);");
            myDB.execSQL("DELETE FROM Favourites WHERE stopID=" + stopID + ";");
        }

        @Override
        public void insert(String stopID, int position) {

        }

    }

    private final DragSortListView.DropListener mDropListener =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
                    myDB.execSQL("DELETE FROM Favourites");
                    int index;
                    for (int i=0; i<listCount; i++) {
                        if (i == to) {
                            index = from;
                        } else if (i >= from && i<to) {
                            index = i+1;
                        } else if (i <= from && i>to) {
                            index = i-1;
                        } else {
                            index = i;
                        }
                        myDB.execSQL("INSERT INTO Favourites VALUES(" + stopArray[index] + ",'" + nameArray[index] + "','"
                                + userNameArray[index] + "');");
                    }
                    readFavourites();
                }
            };

    private final DragSortListView.RemoveListener mRemoveListener =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    SQLiteDatabase myDB = openOrCreateDatabase("main", MODE_PRIVATE, null);
                    myDB.execSQL("DELETE FROM Favourites WHERE stopID=" + stopArray[which] + ";");
                    readFavourites();
                }
            };

    private final DragSortListView.DragScrollProfile mDragScrollProfile =
            new DragSortListView.DragScrollProfile() {
                @Override
                public float getSpeed(float w, long t) {
                    if (w > 0.8f) {
                        // Traverse all views in a millisecond
                        return ((float) mAdapter.getCount()) / 0.001f;
                    } else {
                        return 10.0f * w;
                    }
                }
            };

}