/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class CfdpHeader {

    private static final int FIXED_PART_LENGTH = 8;
    private final PacketType _type;
    private final SourceID _sourceID;

    private static final Logger LOG = LoggerFactory.getLogger(CfdpHeader.class);

    public CfdpHeader(PacketType type, SourceID sourceID) {
        _type = type;
        _sourceID = sourceID;
    }

    public PacketType getType() {
        return _type;
    }

    public SourceID getSourceID() {
        return _sourceID;
    }

    public static CfdpHeader parse(Slice slice) throws IOException {

        if (slice.remaining() < FIXED_PART_LENGTH) {
            return null;
        }

        int typeByte = slice.getUnsignedByte(); //1

        boolean isData;
        switch (typeByte) {
            case 0x04:
                isData = false;
                break;
            case 0x14:
                isData = true;
                break;
            default:
                LOG.warn("Invalid PDU-byte: {}", typeByte);
                return null;
        }

        int length = slice.getUnsignedByte() << 8 | slice.getUnsignedByte(); //2 and 3
        int reserved = slice.getUnsignedByte(); //4
        assert (reserved == 1);

        SourceID sourceID;
        int sourceIdValue = slice.getUnsignedByte(); //5
        if (sourceIdValue == 0) {
            sourceID = SourceID.OLI;
        } else if (sourceIdValue == 1) {
            sourceID = SourceID.TIRS;
        } else if (sourceIdValue == 3) {
            sourceID = SourceID.Ancillary;
        } else {
            LOG.warn("Invalid CFDP Source Entity ID: {}", sourceIdValue);
            return null;
        }
        slice.skip(3); //skip over the remaining 3 bytes:6,7,8

        if (isData) {
            return readDataPacket(sourceID, length, slice);
        } else {
            return readDirectivePacket(sourceID, length, slice);
        }

    }

    private static CfdpHeader readDirectivePacket(SourceID sourceID, int length, Slice data) throws IOException {
        int fileDirectiveNumber = data.getUnsignedByte();
        if (fileDirectiveNumber < 0) {
            return null;
        }
        switch (fileDirectiveNumber) {
            case 0x07:
                return Metadata.parse(sourceID, length, data);
            case 0x04:
                return EOF.parse(sourceID, length, data);
            default:
                LOG.error("Unsupported CDFP file directive: {}", fileDirectiveNumber);
                return null;
        }

    }

    private static CfdpHeader readDataPacket(SourceID sourceID, int length, Slice data) throws IOException {
        return Data.parse(sourceID, length, data);
    }

    public static enum PacketType {

        Metadata, Data, EOF
    }

    public static enum SourceID {

        OLI, TIRS, Ancillary
    }

    public static class Metadata extends CfdpHeader {

        private final String _filename;

        private Metadata(SourceID sourceID, String filename) {
            super(PacketType.Metadata, sourceID);
            _filename = filename;
        }

        public String getDestinationFilename() {
            return _filename;
        }

        public static CfdpHeader parse(SourceID sourceID, int length, Slice data) throws IOException {
            if (data.remaining() < 21) {
                return null;
            }
            assert length == 0x16;
            data.skip(14);
            return new Metadata(sourceID, new String(data.getBytes(7), "US-ASCII"));
        }
    }

    static class Data extends CfdpHeader {

        private final long _offset;
        private final int _payloadLength;

        private Data(SourceID sourceID, long offset, int payloadLength) {
            super(PacketType.Data, sourceID);
            _payloadLength = payloadLength;
            _offset = offset;
        }

        public long getOffset() {
            return _offset;
        }

        public int getPayloadLength() {
            return _payloadLength;
        }

        private static CfdpHeader parse(SourceID sourceID, int length, Slice data) {
            if (data.remaining() < 4) {
                return null;
            }
            long offset = data.getUnsignedByte() << 24
                    | data.getUnsignedByte() << 16
                    | data.getUnsignedByte() << 8
                    | data.getUnsignedByte();
            return new Data(sourceID, offset, length - 4);
        }

    }

    static class EOF extends CfdpHeader {

        private final int _checksum;
        private final long _fileSize;

        private EOF(SourceID sourceID, long fileSize, int checksum) {
            super(PacketType.EOF, sourceID);
            _checksum = checksum;
            _fileSize = fileSize;
        }

        public int getChecksum() {
            return _checksum;
        }

        public long getFilesize() {
            return _fileSize;
        }

        public static CfdpHeader parse(SourceID sourceID, int length, Slice data) {
            data.skip(1);

            int checksum = data.getUnsignedByte() << 24
                    | data.getUnsignedByte() << 16
                    | data.getUnsignedByte() << 8
                    | data.getUnsignedByte();

            long fileSize = data.getUnsignedByte() << 24;
            fileSize = fileSize | data.getUnsignedByte() << 16;
            fileSize = fileSize | data.getUnsignedByte() << 8;
            fileSize = fileSize | data.getUnsignedByte();
            return new EOF(sourceID, fileSize, checksum);
        }
    }

}
