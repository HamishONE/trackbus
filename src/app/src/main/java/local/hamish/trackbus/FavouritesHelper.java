package local.hamish.trackbus;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class FavouritesHelper {

    private SQLiteDatabase myDB;
    private ServiceBoardActivity serviceBoardActivity;

    FavouritesHelper(SQLiteDatabase db, ServiceBoardActivity serviceBoardActivity) {
        myDB = db;
        this.serviceBoardActivity = serviceBoardActivity;
    }

    // Queries database to see if route is a favourite
    boolean isFavRoute(String route) {
        Cursor resultSet = myDB.rawQuery("SELECT * FROM FavRoutes WHERE route='" + route + "'", null);
        if (resultSet.getCount() > 0) {
            resultSet.close();
            return true;
        } else {
            resultSet.close();
            return false;
        }
    }

    // Adds or removes route from database and array and redraws list
    void changeFavRoute(String route) {

        // Query table for selected route
        Cursor resultSet = myDB.rawQuery("SELECT * FROM FavRoutes WHERE route='" + route + "'", null);
        resultSet.moveToFirst();

        // Check if route already exists
        if (resultSet.getCount() == 0) {
            // If not add entry
            myDB.execSQL("INSERT INTO FavRoutes VALUES('" + route + "');");
        } else {
            // If so remove entry
            myDB.execSQL("DELETE FROM FavRoutes WHERE route='" + route + "';");
        }

        resultSet.close();
        serviceBoardActivity.produceView();
        serviceBoardActivity.produceViewOld();
    }

}