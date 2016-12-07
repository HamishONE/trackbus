package local.hamish.trackbus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String value = settings.getString("start_page", "find_a_stop");

        Intent intent;
        switch (value) {
            case "find_a_stop":
                intent = new Intent(this, MainActivity.class);
                break;
            case "all_vehicles":
                intent = new Intent(this, AllBusesActivity.class);
                break;
            case "favourite_stops":
                intent = new Intent(this, FavouritesActivity.class);
                break;
            default:
                intent = new Intent(this, MainActivity.class);
                break;
        }

        startActivity(intent);
        finish();
    }
}