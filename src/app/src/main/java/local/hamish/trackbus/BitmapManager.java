package local.hamish.trackbus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class BitmapManager {

    private Cursor favStopsResultSet;
    private SQLiteDatabase myDB;

    private Bitmap busBitmap;
    private Bitmap trainBitmap;
    private Bitmap ddBitmap;
    private Bitmap favBusBitmap;
    private Bitmap favTrainBitmap;

    BitmapManager(Context context) {

        busBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_bus_blue);
        trainBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_train_purple);
        ddBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_bus_brown);
        favBusBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_bus_red);
        favTrainBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_train_red);

        int width = (int) context.getResources().getDimension(R.dimen.bitmap_width);
        int regHeight = (int) context.getResources().getDimension(R.dimen.bitmap_height_reg);
        int trainHeight = (int) context.getResources().getDimension(R.dimen.bitmap_height_train);

        busBitmap = Bitmap.createScaledBitmap(busBitmap, width, regHeight, false);
        trainBitmap = Bitmap.createScaledBitmap(trainBitmap, width, trainHeight, false);
        ddBitmap = Bitmap.createScaledBitmap(ddBitmap, width, regHeight, false);
        favBusBitmap = Bitmap.createScaledBitmap(favBusBitmap, width, regHeight, false);
        favTrainBitmap = Bitmap.createScaledBitmap(favTrainBitmap, width, trainHeight, false);

        myDB = context.openOrCreateDatabase("main", Context.MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS FavRoutes(route TEXT);");
        favStopsResultSet = myDB.rawQuery("SELECT route FROM FavRoutes", null);
    }

    void close() {
        favStopsResultSet.close();
        myDB.close();
    }

    Bitmap getVehicleBitmap(boolean isTrain, String route, String vehicle_id) {

        boolean isFav = false;
        favStopsResultSet.moveToFirst();
        while (!favStopsResultSet.isAfterLast()) {
            if (favStopsResultSet.getString(0).equals(route)) {
                isFav = true;
            }
            favStopsResultSet.moveToNext();
        }

        if (isTrain) {
            if (isFav) {
                return favTrainBitmap;
            } else {
                return trainBitmap;
            }
        } else {
            if (isFav) {
                return favBusBitmap;
            } else if (ATApi.isDoubleDecker(vehicle_id)) {
                return ddBitmap;
            } else {
                return busBitmap;
            }
        }
    }
}
