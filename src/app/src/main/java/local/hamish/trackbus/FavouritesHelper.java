package local.hamish.trackbus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class FavouritesHelper {

    private ServiceBoardActivity serviceBoardActivity;

    FavouritesHelper(ServiceBoardActivity serviceBoardActivity) {
        this.serviceBoardActivity = serviceBoardActivity;
    }

    // Queries database to see if route is a favourite
    boolean isFavRoute(String route) {

        SQLiteDatabase myDB = serviceBoardActivity.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        Cursor resultSet = myDB.rawQuery("SELECT * FROM FavRoutes WHERE route='" + route + "'", null);
        int count = resultSet.getCount();
        resultSet.close();
        myDB.close();

        return count > 0;
    }

    // Adds or removes route from database and array and redraws list
    void changeFavRoute(String route) {

        // Query table for selected route
        SQLiteDatabase myDB = serviceBoardActivity.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
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
        myDB.close();
        serviceBoardActivity.produceView();
        serviceBoardActivity.produceViewOld();
    }

}