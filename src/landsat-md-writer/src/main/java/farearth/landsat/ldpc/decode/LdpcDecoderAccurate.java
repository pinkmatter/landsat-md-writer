/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.decode.LdpcDecoderAccurate.LdpcDecoderAccurateContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TODO: The algorithm here should be replace with the proper probability
 * propagating LDPC algorithm.
 *
 * @author anton
 * @param <ParityGraph>
 */
public class LdpcDecoderAccurate<ParityGraph extends LdpcParityGraph> extends LdpcDecoder<ParityGraph, LdpcDecoderAccurateContext> {

    public LdpcDecoderAccurate(ParityGraph graph, boolean detectErrors, int maxErrorsToFix, int encoderBitBoundary) {
        super(graph, detectErrors, maxErrorsToFix, encoderBitBoundary);
    }

    @Override
    protected LdpcDecoderAccurateContext createContext(byte[] encodedBytes) {
        return new LdpcDecoderAccurateContext();
    }

    @Override
    protected Integer getNextBitToFlip(LdpcDecoderAccurateContext context) {
        Map<Integer, Integer> errorBitConfidence = getBitConfidence(context.getParityErrorRows());
        Map<Integer, List<Integer>> bitsByConfidence = getBitsByConfidence(errorBitConfidence);
        Integer bitIndex = getNextBitToFlip(context.getPreviousFlippedStates(), bitsByConfidence);
        return bitIndex;
    }

    private Map<Integer, Integer> getBitConfidence(List<Integer> rows) {
        Map<Integer, Integer> errorBitConfidence = new HashMap<>();
        ParityGraph graph = getGraph();
        int virtualBitCount = getFrameSizes().getVirtualBitCount();
        for (Integer row : rows) {
            for (int i = 0; i < graph.getRowWeight(); i++) {
                int bitIndex = graph.getColumn(row, i) - virtualBitCount;
                if (bitIndex >= 0) { // virtual bits all zero
                    Integer confidence = errorBitConfidence.get(bitIndex);
                    if (confidence == null) {
                        confidence = 10;
                    } else {
                        confidence--;
                    }
                    errorBitConfidence.put(bitIndex, confidence);
                }
            }
        }
        return errorBitConfidence;
    }

    private Map<Integer, List<Integer>> getBitsByConfidence(Map<Integer, Integer> errorBitConfidence) {
        Map<Integer, List<Integer>> bitsByConfidence = new TreeMap<>(); // natural order
        for (Map.Entry<Integer, Integer> entry : errorBitConfidence.entrySet()) {
            Integer bitIndex = entry.getKey();
            Integer confidence = entry.getValue();
            List<Integer> bits = bitsByConfidence.get(confidence);
            if (bits == null) {
                bits = new ArrayList<>();
                bitsByConfidence.put(confidence, bits);
            }
            bits.add(bitIndex);
        }
        return bitsByConfidence;
    }

    private Integer getNextBitToFlip(List<List<Integer>> previousStates, Map<Integer, List<Integer>> bitsByConfidence) {
        List<Integer> lastState = previousStates.isEmpty()
                ? Collections.EMPTY_LIST
                : previousStates.get(previousStates.size() - 1);
        for (Map.Entry<Integer, List<Integer>> entry : bitsByConfidence.entrySet()) {
            List<Integer> bits = entry.getValue();
            List<Integer> unFlippedBits = new ArrayList<>();
            // First check flipped bits, to keep total flipped bits low
            for (Integer index : bits) {
                List<Integer> newState = new ArrayList<>(lastState);
                if (newState.remove(index)) {
                    if (!previousStates.contains(newState)) {
                        previousStates.add(newState);
                        return index;
                    }
                } else {
                    unFlippedBits.add(index);
                }
            }
            for (Integer index : unFlippedBits) {
                List<Integer> newState = new ArrayList<>(lastState);
                newState.add(index);
                if (!previousStates.contains(newState)) {
                    previousStates.add(newState);
                    return index;
                }
            }
        }
        return null;
    }

    public static class LdpcDecoderAccurateContext implements LdpcDecoderContext {

        private final List<List<Integer>> previousFlippedStates = new ArrayList<>();
        private List<Integer> parityErrorRows;

        public List<List<Integer>> getPreviousFlippedStates() {
            return previousFlippedStates;
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
            return previousFlippedStates.get(previousFlippedStates.size() - 1);
        }
    }
}
