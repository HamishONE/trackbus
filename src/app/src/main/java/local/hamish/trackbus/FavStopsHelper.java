package local.hamish.trackbus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FavStopsHelper {

    private Context app;
    private ServiceBoardActivity parent;
    private SQLiteDatabase myDB;
    private String stopID;
    private String stopName;
    private FavouritesActivity favouritesActivity;
    private int pos;

    public FavStopsHelper(Context app, SQLiteDatabase myDB, ServiceBoardActivity parent, String stopID,
                          String stopName, FavouritesActivity favouritesActivity) {
        this.app = app;
        this.myDB = myDB;
        this.parent = parent;
        this.stopID = stopID;
        this.stopName = stopName;
        this.favouritesActivity = favouritesActivity;
    }

    public void changeFavourite() {

        // Open or create database and create favourites table if needed
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Favourites(stopID INTEGER ,stopName TEXT, userName TEXT);");

        // Query table for current stop
        Cursor resultSet = myDB.rawQuery("SELECT * FROM Favourites WHERE stopID=" + stopID, null);
        resultSet.moveToFirst();

        if (resultSet.getCount() == 0) {
            resultSet.close();

            showDialog(0, stopName);

        } else {
            resultSet.close();

            myDB.execSQL("DELETE FROM Favourites WHERE stopID=" + stopID + ";");
            Toast.makeText(app, "Removed from favourites", Toast.LENGTH_LONG).show();
            parent.changeHeartIcon();
        }
    }

    private void finishAddFavourite(String userName) {

        // If rename
        if (parent == null) {
            favouritesActivity.userNameArray[pos] = userName;

            myDB.execSQL("DELETE FROM Favourites");
            for (int i=0; i<favouritesActivity.listCount; i++) {
                myDB.execSQL("INSERT INTO Favourites VALUES(" + favouritesActivity.stopArray[i] + ",'"
                        + favouritesActivity.nameArray[i] + "','" + favouritesActivity.userNameArray[i] + "');");
            }
            favouritesActivity.readFavourites();

            return;
        }

        myDB.execSQL("INSERT INTO Favourites VALUES(" + stopID + ",'" + stopName + "','" + userName + "');");
        Toast.makeText(app, "Added to favourites", Toast.LENGTH_LONG).show();
        parent.changeHeartIcon();
    }

    public void showDialog(int pos, String oldUserName) {
        this.pos = pos;

        AlertDialog.Builder builder;
        final EditText input;

        if (parent==null) builder = new AlertDialog.Builder(favouritesActivity);
        else builder = new AlertDialog.Builder(parent);
        builder.setTitle("Enter name for stop:");

        if (parent==null) input = new EditText(favouritesActivity);
        else input = new EditText(parent);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(oldUserName);
        input.setSelection(oldUserName.length());

        FrameLayout container = new FrameLayout(app);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) Util.convertDpToPixel(20, app);
        params.rightMargin = (int) Util.convertDpToPixel(10, app);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userName = input.getText().toString();
                finishAddFavourite(userName);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

}