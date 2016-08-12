package local.hamish.trackbus;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;

public class RecentStops {

    private SQLiteDatabase myDB;
    public int[] stopIDs = new int[5];
    public String[] stopNames = new String[5];
    private int count;
    private MenuItem[] menuItems = new MenuItem[5];

    public RecentStops(SQLiteDatabase myDB, Menu menu) {
        this.myDB = myDB;
        myDB.execSQL("CREATE TABLE IF NOT EXISTS RecentStops(stopID INTEGER, stopName TEXT);");

        menuItems[0] = menu.findItem(R.id.bus1);
        menuItems[1] = menu.findItem(R.id.bus2);
        menuItems[2] = menu.findItem(R.id.bus3);
        menuItems[3] = menu.findItem(R.id.bus4);
        menuItems[4] = menu.findItem(R.id.bus5);
    }

    public void readStops() {
        Cursor resultSet = myDB.rawQuery("SELECT * FROM RecentStops", null);
        resultSet.moveToFirst();
        int i;
        for (i = 0; i < resultSet.getCount(); i++) {
            stopIDs[i] = resultSet.getInt(0);
            stopNames[i] = resultSet.getString(1);
            resultSet.moveToNext();

            menuItems[i].setTitle(stopNames[i]);
            menuItems[i].setVisible(true);
        }
        count = i;
        resultSet.close();
    }

    public void addStop(String stopID, String stopName) {
        readStops();

        for (int i=0; i<5; i++) {
            if (stopIDs[i] == Integer.valueOf(stopID)) {
                for (int j=i; j<4; j++) {
                    stopIDs[j] = stopIDs[j+1];
                    stopNames[j] = stopNames[j+1];
                }
                count--;
            }
        }

        myDB.execSQL("DELETE FROM RecentStops");
        myDB.execSQL("INSERT INTO RecentStops VALUES(" + stopID + ",'" + stopName + "');");
        for (int i=0; i<4; i++) {
            myDB.execSQL("INSERT INTO RecentStops VALUES(" + stopIDs[i] + ",'" + stopNames[i] + "');");
        }

        readStops();
    }

}
