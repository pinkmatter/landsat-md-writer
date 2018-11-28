/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.util.HashMap;
import java.util.Map;

/**
 * Application Process Identifier
 *
 * @author Sonja
 */
public enum APID {

    Fill(2047),
    AncillaryData(5),
    TirsCfdpMetaPdu(1024),
    TirsCfdpCfdpEofPdu(1025),
    TirsLineHeader(1026),
    TirsCRC(1027),
    TirsPx10(1792),
    TirsPxDark(1793),
    TirsPx12(1794),
    OliCfdpMetaPdu(0),
    OliCfdpEofPdu(1),
    OliLineHeader(2),
    OliCRC(3),
    OliFrmHdr(4),
    OliPxPAN1OddComp(256),
    OliPxPAN1EvnComp(257),
    OliPxBlueComp(258),
    OliPxCaComp(259),
    OliPxNirComp(260),
    OliPxRedComp(261),
    OliPxGreenComp(262),
    OliPxPAN2OddComp(263),
    OliPxPAN2EvnComp(264),
    OliPxSwir2Comp(265),
    OliPxSwir1Comp(266),
    OliPxCirrusComp(267),
    OliPxDarkComp(268),
    OliPxPAN1Odd(768),
    OliPxPAN1Evn(769),
    OliPxBlue(770),
    OliPxCA(771),
    OliPxNir(772),
    OliPxRed(773),
    OliPxGreen(774),
    OliPxPAN2Odd(775),
    OliPxPAN2Evn(776),
    OliPxSwir2(777),
    OliPxSwir1(778),
    OliPxCirrus(779),
    OliPxDark(780);

    private static final Map<Integer, APID> APID_MAP;
    private final int _value;

    static {
        APID_MAP = new HashMap<>();
        for (APID val : APID.values()) {
            APID_MAP.put(val._value, val);
        }
    }

    private APID(int value) {
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    public static APID forValue(int value) {
        return APID_MAP.get(value);
    }

}
