package local.hamish.trackbus;

public class AdvancedApiBoard_public_api extends AdvancedApiBoard {

    public AdvancedApiBoard_public_api(ServiceBoardActivity serviceBoardActivity, String stopID, String stopName, boolean showTerminating, Output out) {
        super(serviceBoardActivity, stopID, stopName, showTerminating, out);
    }

    @Override
    protected String getTripDataUrl() {
        return PublicApiV2.data.apiRoot + PublicApiV2.data.stopInfo + stopID;
    }

    @Override
    protected String getStopDataUrl() {
        //return PublicApiV2.data.apiRoot + PublicApiV2.data.realtime + "?";
        return PublicApiV2.data.apiRoot + PublicApiV2.data.tripUpdates + "?";
    }
}
