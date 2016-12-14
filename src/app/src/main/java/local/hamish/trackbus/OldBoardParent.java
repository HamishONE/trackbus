package local.hamish.trackbus;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.Date;

abstract class OldBoardParent {

    ArrayList<Items> items;
    boolean active = true;

    ServiceBoardActivity serviceBoardActivity;
    String stopID;

    OldBoardParent(ServiceBoardActivity serviceBoardActivity, String stopID) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;
    }

    abstract void updateData();

    // Show snackbar and allow refreshing on HTTP failure
    void handleError(int statusCode) {
        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelOld);
        if (circle == null) {
            Log.e("Early exit", "from handleError in AdvancedApiBoard_private_api class");
            return;
        }
        circle.setVisibility(View.GONE);

        String message = Util.generateErrorMessage(serviceBoardActivity, statusCode);

        if (serviceBoardActivity.snackbar != null && serviceBoardActivity.snackbar.isShown()) return;
        View view = serviceBoardActivity.findViewById(R.id.cordLayout);
        serviceBoardActivity.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        serviceBoardActivity.snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {serviceBoardActivity.updateData(true);}
        });
        serviceBoardActivity.snackbar.show();
    }

    // To store output data
    static class Items implements Comparable<Items> {
        String route;
        String headsign;
        String scheduled;
        String dueTime;
        String platform;
        Date scheduledDate;

        // Constructor sets all scheduled times to well into the future
        Items() {
            scheduledDate = new Date(Long.MAX_VALUE);
        }

        @Override // Allows sorting by scheduled times
        public int compareTo(@NonNull Items o) {
            return scheduledDate.compareTo(o.scheduledDate);
        }
    }
}
