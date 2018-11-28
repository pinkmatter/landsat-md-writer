/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.decode.LdpcDecoderFast.LdpcDecoderFastContext;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author anton
 * @param <ParityGraph>
 */
public class LdpcDecoderFast<ParityGraph extends LdpcParityGraph> extends LdpcDecoder<ParityGraph, LdpcDecoderFastContext> {

    public LdpcDecoderFast(ParityGraph graph, boolean detectErrors, int maxErrorsToFix, int encoderBitBoundary) {
        super(graph, detectErrors, maxErrorsToFix, encoderBitBoundary);
    }

    @Override
    protected LdpcDecoderFastContext createContext(byte[] encodedBytes) {
        return new LdpcDecoderFastContext();
    }

    @Override
    protected Integer getNextBitToFlip(LdpcDecoderFastContext context) {
        byte[] errorCountForBit = new byte[getFrameSizes().getEncodedBitCount()];
        int virtualBitCount = getFrameSizes().getVirtualBitCount();
        int highest = 0;
        int highestIndex = 0;
        int[][] rowColumnMatrix = getGraph().getRowColumnMatrix();
        int rowWeight = getGraph().getRowWeight();
        List<Integer> rows = context.getParityErrorRows();
        for (Integer row : rows) {
            for (int i = 0; i < rowWeight; i++) {
                int bitIndex = rowColumnMatrix[row][i] - virtualBitCount;
                if (bitIndex >= 0) { // virtual bits all zero
                    int count = ++errorCountForBit[bitIndex];
                    if (count > highest) {
                        highest = count;
                        highestIndex = bitIndex;
                    }
                }
            }
        }
        List<Integer> lastFlipped = context.getLastFlipped();
        if (!lastFlipped.remove((Integer) highestIndex)) {
            lastFlipped.add(highestIndex);
        }
        return highestIndex;
    }

    public static class LdpcDecoderFastContext implements LdpcDecoderContext {

        private final List<Integer> lastFlipped = new ArrayList<>();
        private List<Integer> parityErrorRows;

        public List<Integer> getLastFlipped() {
            return lastFlipped;
        }

        @Override
        public void setParityErrorRows(List<Integer> rows) {
            parityErrorRows = rows;
        }

        public List<Integer> getParityErrorRows() {
            return parityErrorRows;
        }

        @Override
        public List<Integer> getFinalErrorBits() {
            return getLastFlipped();
        }

    }
}
