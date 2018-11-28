/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;

/**
 *
 * @author Thinus
 */
class CaduHeader {

    public static final int ASM = 0x352EF853;
    public static final int CADU_SIZE = 1034;
    public static final int ASM_SIZE = 4;
    public static final int CADU_SIZE_EXCL_ASM = CADU_SIZE - ASM_SIZE;
    public static final int MPDU_SIZE = 2;
    public static final int PRIMARY_PAYLOAD_SIZE = CADU_SIZE - ASM_SIZE - CaduHeader.SIZE;
    public static final int PAYLOAD_SIZE = PRIMARY_PAYLOAD_SIZE - MPDU_SIZE;

    public static final int SIZE = 6;

    private final int _version;
    private final int _spacecraftId;
    private final int _vcID;
    private final long _vcFrameCount;
    private final boolean _replayFlag;
    private final boolean _vcFrameCountUsage;
    private final int _vcFrameCountCycle; //not used on Landsat 8

    private CaduHeader(
            int version,
            int spacecraftId,
            int vcId,
            long frameCount,
            boolean replay,
            boolean frameCountUsage,
            int frameCountCycle) {
        _version = version;
        _spacecraftId = spacecraftId;
        _vcID = vcId;
        _vcFrameCount = frameCount;
        _replayFlag = replay;
        _vcFrameCountUsage = frameCountUsage;
        _vcFrameCountCycle = frameCountCycle;
    }

    public int getVersion() {
        return _version;
    }

    public int getSpacecraftID() {
        return _spacecraftId;
    }

    public int getVcID() {
        return _vcID;
    }

    public long getVcFrameCount() {
        return _vcFrameCount;
    }

    public boolean isReplay() {
        return _replayFlag;
    }

    public boolean isVcFrameCountUsage() {
        return _vcFrameCountUsage;
    }

    public int getVcFrameCountCycle() {
        return _vcFrameCountCycle;
    }

    public static int getVcAndCheck(int idByte1, int idByte2, int version, int spacecraft) {
        int frameVersionNumber = (idByte1 & 0xC0) >> 6;
        if (frameVersionNumber == version) {
            int spacecraftId = (idByte1 & 0x3F) << 2 | (idByte2 & 0xC0) >> 6;
            if (spacecraftId == spacecraft) {
                int vcId = (idByte2 & 0x3F);
                return vcId;
            }
        }
        return -1;
    }

    public static CaduHeader parseFull(Slice slice) {

        int idByte1 = slice.getUnsignedByte();
        int idByte2 = slice.getUnsignedByte();

        int frameVersionNumber = (idByte1 & 0xC0) >> 6;
        int spacecraftId = (idByte1 & 0x3F) << 2 | (idByte2 & 0xC0) >> 6;
        int vcId = (idByte2 & 0x3F);

        long vcFrameCount = slice.getUnsignedByte();
        vcFrameCount = vcFrameCount << 16;
        vcFrameCount = vcFrameCount | slice.getUnsignedByte() << 8;
        vcFrameCount = vcFrameCount | slice.getUnsignedByte();

        int signalingField = slice.getUnsignedByte();
        boolean replay = (signalingField & 0x80) > 0;
        boolean vcFrameCountUsage = (signalingField & 0x40) > 0;
        int vcFrameCountCycle = (signalingField & 0x0F);

        CaduHeader header = new CaduHeader(
                frameVersionNumber, spacecraftId, vcId,
                vcFrameCount, replay, vcFrameCountUsage, vcFrameCountCycle);

        return header;
    }
}
