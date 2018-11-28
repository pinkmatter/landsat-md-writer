/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.util.EnumSet;
import java.util.HashSet;

/**
 *
 * @author Chris
 */
public interface ApidFilter {

    boolean pass(APID apid);

    public static class All implements ApidFilter {

        @Override
        public boolean pass(APID apid) {
            return true;
        }
    }

    public static class Set implements ApidFilter {

        private final EnumSet<APID> _apids;

        public Set(EnumSet<APID> apids) {
            _apids = apids;
        }

        @Override
        public boolean pass(APID apid) {
            return _apids.contains(apid);
        }
    }

    public static class Bands extends Set {

        private static final String PAN = "pan";
        private static final String COASTAL = "coastal";
        private static final String RED = "red";
        private static final String GREEN = "green";
        private static final String BLUE = "blue";
        private static final String NIR = "nir";
        private static final String SWIR1 = "swir1";
        private static final String SWIR2 = "swir2";
        private static final String CIRRUS = "cirrus";
        private static final String THERMAL1 = "thermal1";
        private static final String THERMAL2 = "thermal2";

        public Bands(String bands) {
            this(bands.split(","));
        }

        public Bands(String bands, boolean includeAncillary) {
            this(bands.split(","), includeAncillary);
        }

        public Bands(String bands, boolean includeAncillary, boolean includeCfdpApids) {
            this(bands.split(","), includeAncillary, includeCfdpApids);
        }

        public Bands(String[] bands) {
            this(bands, true);
        }

        public Bands(String[] bands, boolean includeAncillary) {
            this(bands, includeAncillary, false);
        }

        public Bands(String[] bands, boolean includeAncillary, boolean includeCfdpApids) {
            super(createSet(bands, includeAncillary, includeCfdpApids));
        }

        private static EnumSet<APID> createSet(String[] bands, boolean includeAncillary, boolean includeCfdpApids) {
            java.util.Set<String> names = new HashSet<>();
            java.util.Set<APID> apids = new HashSet<>();
            for (String s : bands) {
                names.add(s.trim());
            }
            if (includeAncillary) {
                apids.add(APID.AncillaryData);
            }
            if (includeCfdpApids) {
                if (containsOli(names)) {
                    apids.add(APID.OliCfdpMetaPdu);
                    apids.add(APID.OliCfdpEofPdu);
                    apids.add(APID.OliLineHeader);
                    apids.add(APID.OliCRC);
                    apids.add(APID.OliFrmHdr);
                }
                if (containsTirs(names)) {
                    apids.add(APID.TirsCfdpMetaPdu);
                    apids.add(APID.TirsCfdpCfdpEofPdu);
                    apids.add(APID.TirsLineHeader);
                    apids.add(APID.TirsCRC);
                }
            }

            if (containsOli(names)) {
                apids.add(APID.OliLineHeader);
                apids.add(APID.OliCRC);
                apids.add(APID.OliFrmHdr);
            }
            if (containsTirs(names)) {
                apids.add(APID.TirsLineHeader);
                apids.add(APID.TirsCRC);
            }

            for (String band : names) {
                switch (band) {
                    case PAN:
                        apids.add(APID.OliPxPAN1Evn);
                        apids.add(APID.OliPxPAN1EvnComp);
                        apids.add(APID.OliPxPAN2Evn);
                        apids.add(APID.OliPxPAN2EvnComp);
                        apids.add(APID.OliPxPAN1Odd);
                        apids.add(APID.OliPxPAN1OddComp);
                        apids.add(APID.OliPxPAN2Odd);
                        apids.add(APID.OliPxPAN2OddComp);
                        break;
                    case COASTAL:
                        apids.add(APID.OliPxCA);
                        apids.add(APID.OliPxCaComp);
                        break;
                    case RED:
                        apids.add(APID.OliPxRed);
                        apids.add(APID.OliPxRedComp);
                        break;
                    case GREEN:
                        apids.add(APID.OliPxGreen);
                        apids.add(APID.OliPxGreenComp);
                        break;
                    case BLUE:
                        apids.add(APID.OliPxBlue);
                        apids.add(APID.OliPxBlueComp);
                        break;
                    case NIR:
                        apids.add(APID.OliPxNir);
                        apids.add(APID.OliPxNirComp);
                        break;
                    case SWIR1:
                        apids.add(APID.OliPxSwir1);
                        apids.add(APID.OliPxSwir1Comp);
                        break;
                    case SWIR2:
                        apids.add(APID.OliPxSwir2);
                        apids.add(APID.OliPxSwir2Comp);
                        break;
                    case CIRRUS:
                        apids.add(APID.OliPxCirrus);
                        apids.add(APID.OliPxCirrusComp);
                        break;
                    case THERMAL1:
                        apids.add(APID.TirsPx10);
                        break;
                    case THERMAL2:
                        apids.add(APID.TirsPx12);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown band name: " + band);
                }
            }
            return EnumSet.copyOf(apids);
        }

        private static boolean containsTirs(java.util.Set<String> names) {
            return names.contains(THERMAL1) || names.contains(THERMAL2);
        }

        private static boolean containsOli(java.util.Set<String> names) {
            return names.contains(PAN) || names.contains(COASTAL)
                    || names.contains(RED) || names.contains(GREEN)
                    || names.contains(BLUE) || names.contains(NIR)
                    || names.contains(SWIR1) || names.contains(SWIR2)
                    || names.contains(CIRRUS);

        }

    }
}
