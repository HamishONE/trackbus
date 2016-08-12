package local.hamish.trackbus;

public class AdvancedApiBoard_private_api extends AdvancedApiBoard {

    public AdvancedApiBoard_private_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName, boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> origin/development
    protected String getTripDataUrl() {
        return ATApi.data.apiRoot + ATApi.data.stopInfo + stopID + ATApi.getAuthorization();
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> origin/development
    protected String getStopDataUrl() {
        return ATApi.data.apiRoot + ATApi.data.tripUpdates + ATApi.getAuthorization() + "&";
    }
}