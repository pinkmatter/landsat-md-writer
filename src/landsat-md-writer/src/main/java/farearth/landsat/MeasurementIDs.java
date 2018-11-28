/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

/**
 *
 * @author Chris
 */
public interface MeasurementIDs {

    public static final String BYTES_RECEIVED = "total-bytes-received";
    public static final String CADUS_RECEIVED = "total-cadus-received";
    public static final String CADUS_DROPPED = "total-cadus-dropped";
    public static final String FORMAT_VC_PREFIX = "vc.%d";

    public static String getCadusDroppedMeasurementID(int vcID) {
        return getPrefixedMeasurmentID(vcID, CADUS_DROPPED);
    }

    public static String getPrefixedMeasurmentID(int vcID, String measurementID) {
        return String.format(FORMAT_VC_PREFIX + "." + measurementID, vcID);
    }

    public static String getCaduMeasurementPrefix(int vcID) {
        return String.format(FORMAT_VC_PREFIX, vcID);
    }

}
