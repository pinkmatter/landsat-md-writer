/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import static farearth.landsat.ldpc.Landsat8LdpcConstants.ENCODER_BIT_BOUNDARY;

/**
 *
 * @author anton
 */
public class Landsat8LdpcDecoderFast extends LdpcDecoderFast<Landsat8LdpcParityGraph> {

    public Landsat8LdpcDecoderFast() {
        this(true, 60);
    }

    public Landsat8LdpcDecoderFast(Landsat8LdpcParityGraph graph) {
        this(graph, true, 60);
    }

    public Landsat8LdpcDecoderFast(boolean detectErrors, int maxErrorsToFix) {
        super(new Landsat8LdpcParityGraph(new Landsat8LdpcParityMatrix()), detectErrors, maxErrorsToFix, ENCODER_BIT_BOUNDARY);
    }

    /**
     * Constructs a Landsat 8 LDPC decoder.
     *
     * @param graph The parity graph.
     * @param detectErrors Whether to detect and potentially fix errors, or just
     * return the (potentially erroneous) info bits.
     * @param maxErrorsToFix The maximum amount of bit errors that will be
     * attempted to be fixed (see LdpcDecoder).
     */
    public Landsat8LdpcDecoderFast(Landsat8LdpcParityGraph graph, boolean detectErrors, int maxErrorsToFix) {
        super(graph, detectErrors, maxErrorsToFix, ENCODER_BIT_BOUNDARY);
    }
}
