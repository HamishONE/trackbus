package local.hamish.trackbus;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

public abstract class AdvancedApiBoard {

    protected ServiceBoardActivity serviceBoardActivity;
    protected String stopID;
    protected String stopName;
    protected boolean showTerminating;
    protected Output out;
    public boolean active = true;

    // Constructor
    public AdvancedApiBoard(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName,
                       boolean showTerminating, Output out) {
        this.serviceBoardActivity = serviceBoardActivity;
        this.stopID = stopID;
        this.stopName = stopName;
        this.showTerminating = showTerminating;
        this.out = out;
    }

    public abstract void callAPIs();

    public abstract void updateData();

    public abstract void changeTerminating(boolean showTerminating);

    // Object for outputs
    public static class Output {

        public String listArray[] = new String[1000];
        public int stopSeqArray[] = new int[1000];
        public String routeArray[] = new String[1000];
        public String tripArray[] = new String[1000];
        public String headsignArray[] = new String[1000];
        public String schTimeArray[] = new String[1000];
        public String stopsAwayArray[] = new String[1000];
        public String dueTimeArray[] = new String[1000];
        public boolean terminatingArray[] = new boolean[1000];
        public boolean scheduledArray[] = new boolean[1000];
        public long dateSchArray[] = new long[1000];
        public int count = 0;

        public Output() {}

    }

    // Show snackbar and allow refreshing on HTTP failure
    protected void handleError(int statusCode) {
        serviceBoardActivity.prepareMap();
        serviceBoardActivity.allBusesHelper.handleError(statusCode);

        View circle = serviceBoardActivity.findViewById(R.id.loadingPanelNew);
        if (circle == null) {
            Log.e("Early exit", "from handleError in AdvancedApiBoard_private_api class");
            return;
        }
        circle.setVisibility(View.GONE);
        // Prepare message for snackbar
        String message;
        if (!Util.isNetworkAvailable(serviceBoardActivity.getSystemService(Context.CONNECTIVITY_SERVICE)))
            message = "Please connect to the internet";
        else if (statusCode == 0) message = "Network error (no response)";
        else if (statusCode >= 500) message = String.format("AT server error (HTTP response %d)", statusCode);
        else message = String.format("Network error (HTTP response %d)", statusCode);
        // Show snackbar
        if (serviceBoardActivity.snackbar != null && serviceBoardActivity.snackbar.isShown()) return;
        View view = serviceBoardActivity.findViewById(R.id.cordLayout);
        serviceBoardActivity.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        serviceBoardActivity.snackbar.setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {serviceBoardActivity.updateData(true);}
        });
        serviceBoardActivity.snackbar.show();
    }
}
