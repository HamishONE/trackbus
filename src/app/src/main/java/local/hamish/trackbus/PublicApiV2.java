package local.hamish.trackbus;

public final class PublicApiV2 {

    // Blank
    private PublicApiV2() {}

    // Important strings from AT Track My Bus App
    public final static class data {
        final static String apiRoot = "https://api.at.govt.nz/v2/public/";

        final static String headerTerm = "Ocp-Apim-Subscription-Key: ";
        final static String primaryKey = "fd2c776a9b0543d9b5e3fba4a2e25576";
        final static String secondaryKey = "8f65bac859494abf9a8808983a3c8ab5";

        final static String realtime = "realtime/";

        //final static String stopInfo = "gtfs/stops/stopinfo/";
        //final static String vehicleLocations = "public-restricted/realtime/vehiclelocations/";
        //final static String tripUpdates = "public-restricted/realtime/tripUpdates/";
        //final static String realtime = "public-restricted/realtime/";
        //final static String shapeByTripId = "gtfs/shapes/tripId/";
        //final static String stops = "gtfs/stops";
        //final static String departures = "public-restricted/departures/";
    }

}
