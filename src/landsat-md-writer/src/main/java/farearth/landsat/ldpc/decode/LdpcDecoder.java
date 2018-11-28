/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.LdpcFrameSizes;
import farearth.landsat.util.BitUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author anton
 * @param <ParityGraph>
 * @param <DecoderContext>
 */
public abstract class LdpcDecoder<ParityGraph extends LdpcParityGraph, DecoderContext extends LdpcDecoderContext> {

    private static final Logger LOG = LoggerFactory.getLogger(LdpcDecoder.class);
    private final ParityGraph graph;
    private final boolean detectErrors;
    private final int maxFixes;
    private final LdpcFrameSizes frameSizes;

    /**
     * Constructs an LDPC decoder.
     *
     * @param graph The parity graph.
     * @param detectErrors If false, only return the info bits without checking
     * for any errors. If true, detect and attempt to correct errors.
     * @param maxErrorsToFix The maximum amount of bit errors that will be
     * attempted to be fixed. The amount of errors that must be fixed in a
     * codeword are directly proportional to the performance of the decoder, so
     * a limit on the amount of bits to fix puts a limit on the maximum amount
     * of time before the decoder gives up and returns null for the decoded
     * bytes.
     * @param encoderBitBoundary Determines what amount of padding bits were
     * required before and after encoding.
     */
    public LdpcDecoder(ParityGraph graph, boolean detectErrors, int maxErrorsToFix, int encoderBitBoundary) {
        this.graph = graph;
        this.detectErrors = detectErrors;
        this.maxFixes = maxErrorsToFix;
        this.frameSizes = new LdpcFrameSizes(graph.getRows(), graph.getColumns(), encoderBitBoundary);
    }

    protected abstract DecoderContext createContext(byte[] encodedBytes);

    protected abstract Integer getNextBitToFlip(DecoderContext context);

    public ParityGraph getGraph() {
        return graph;
    }

    public LdpcFrameSizes getFrameSizes() {
        return frameSizes;
    }

    public LdpcDecodeResult decode(byte[] encodedBytes) {
        byte[] decoded = null;
        int decodedByteCount = frameSizes.getDecodedByteCount();
        int loop = 0;
        List<Integer> errorBits = Collections.EMPTY_LIST;
        if (!detectErrors) {
            LOG.debug("No error detection performed");
            decoded = Arrays.copyOf(encodedBytes, decodedByteCount);
        } else {
            List<Integer> rows;
            int[] expanded = BitUtils.expand(encodedBytes, 0, frameSizes.getEncodedBitCount());
            rows = getParityRowsWithErrors(expanded);
            if (rows.isEmpty()) {
                LOG.debug("No errors");
                decoded = Arrays.copyOf(encodedBytes, decodedByteCount);
            } else if (maxFixes > 0) {
                DecoderContext context = createContext(encodedBytes);
                context.setParityErrorRows(rows);

                do {
                    Integer bitIndex = getNextBitToFlip(context);
                    if (bitIndex == null) {
                        LOG.debug("No convergence");
                        break;
                    }
                    expanded[bitIndex] = expanded[bitIndex] == 0 ? 1 : 0;
                    rows = getParityRowsWithErrors(rows, expanded, bitIndex + frameSizes.getVirtualBitCount());
                    context.setParityErrorRows(rows);
                    loop++;
                } while (!rows.isEmpty() && loop < maxFixes);

                if (rows.isEmpty()) {
                    int decodedBitCount = frameSizes.getDecodedBitCount();
                    decoded = Arrays.copyOf(encodedBytes, decodedByteCount);
                    errorBits = context.getFinalErrorBits();
                    for (Integer bitIndex : errorBits) {
                        if (bitIndex < decodedBitCount) {
                            BitUtils.flipBit(decoded, bitIndex);
                        }
                    }
                    LOG.debug("Converged after {} loops", loop);
                } else {
                    LOG.debug("No convergence after {} loops", loop);
                }
            }
        }
        return new LdpcDecodeResult(encodedBytes, decoded, loop, errorBits);
    }

    private List<Integer> getParityRowsWithErrors(List<Integer> previousRows, int[] expandedBits, int changedBitNum) {
        int[][] rowColumnMatrix = graph.getRowColumnMatrix();
        int virtualBitCount = frameSizes.getVirtualBitCount();
        for (int i = 0; i < graph.getColumnWeight(); i++) {
            int row = graph.getRow(changedBitNum, i);
            int parity = getParity(rowColumnMatrix, row, virtualBitCount, expandedBits);
            if (parity != 0) {
                previousRows.add(row);
            } else {
                previousRows.remove((Integer) row);
            }
        }
        return previousRows;
    }

    private List<Integer> getParityRowsWithErrors(int[] expandedBits) {
        List<Integer> errorRows = new ArrayList<>();
        int[][] rowColumnMatrix = graph.getRowColumnMatrix();
        int virtualBitCount = frameSizes.getVirtualBitCount();
        int rows = graph.getRows();
        int rowsDiv2 = rows / 2;
        for (int r = 0; r < rows; r++) {
            int parity = getParity(rowColumnMatrix, r, virtualBitCount, expandedBits);
            if (parity != 0) {
                errorRows.add(r);
            }

            if (r == rowsDiv2 && errorRows.isEmpty()) {
                // If the first half of the rows are error free we can assume there are no errors.
                break;
            }
        }
        return errorRows;
    }

    private int getParity(int[][] rowColumnMatrix, int row, int virtualBitCount, int[] expandedBits) {
        int parity = 0;
        int[] matrixRow = rowColumnMatrix[row];
        for (int n = 0; n < graph.getRowWeight(); n++) {
            int bitNum = matrixRow[n];
            if (bitNum >= virtualBitCount) {
                parity ^= expandedBits[bitNum - virtualBitCount];
            }
        }
        return parity;
    }
}
