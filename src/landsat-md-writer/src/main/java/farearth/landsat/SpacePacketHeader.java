/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

/**
 *
 * @author Sonja
 */
class SpacePacketHeader {

    public static final int SIZE = 6;

    //6 bytes
    private boolean _isTelecommand; //Telemetry (report);Telecommand (request)
    private boolean _hasSecondaryHeader;
    private int _version; //3 bits
    private APID _apid; //11 bits
    private int _sequenceFlags; //2 bits
    private int _sequenceCount; //14 bits
    private int _dataLength; // 16 bits, packet length equals datalength +1;

    private int _apidNumber;

    private SpacePacketHeader() {
    }

    public static int parseApidNumber(long headerData) {
        return (int) (headerData >>> 32 & 0x7FF);
    }

    public static APID parseApid(long headerData) {
        return APID.forValue(parseApidNumber(headerData));
    }

    public static int parseDataLength(long headerData) {
        return (int) (headerData & 0xFFFF) + 1; //see CCSDS 133.0-B-1 Page 4-2, section 4.1.2
    }

    public static int parseDataLengthAndVersion(long headerData) {

        return (int) (headerData & 0xFFFF) + 1; //see CCSDS 133.0-B-1 Page 4-2, section 4.1.2
    }

    public static SpacePacketHeader parse(long headerData) {
        SpacePacketHeader header = new SpacePacketHeader();

        header._dataLength = parseDataLength(headerData);
        headerData >>>= 16;
        header._sequenceCount = (int) (headerData & 0x3FFF);
        headerData >>>= 14;
        header._sequenceFlags = (int) (headerData & 0x3);
        headerData >>>= 2;
        int appProcIdInt = (int) (headerData & 0x7FF);
        header._apidNumber = appProcIdInt;
        header._apid = APID.forValue(appProcIdInt);
        headerData >>>= 11;
        header._hasSecondaryHeader = ((headerData & 0x1) == 1);
        headerData >>>= 1;
        header._isTelecommand = ((headerData & 0x1) == 1);
        headerData >>>= 1;
        header._version = (int) (headerData & 0x7);

        return header;
    }

    public boolean isTelecommand() {
        return _isTelecommand;
    }

    public boolean hasSecondaryHeader() {
        return _hasSecondaryHeader;
    }

    public int getVersion() {
        return _version;
    }

    public int getApidNumber() {
        return _apidNumber;
    }

    public int getSequenceFlags() {
        return _sequenceFlags;
    }

    public int getSequenceCount() {
        return _sequenceCount;
    }

    public int getDataLength() {
        return _dataLength;
    }

    public APID getApid() {
        return _apid;
    }

}
