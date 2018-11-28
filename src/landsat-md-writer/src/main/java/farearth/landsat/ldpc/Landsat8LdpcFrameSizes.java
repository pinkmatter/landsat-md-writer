/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.ldpc.Landsat8LdpcConstants.Parity;

/**
 *
 * @author anton
 */
public class Landsat8LdpcFrameSizes extends LdpcFrameSizes {

    public Landsat8LdpcFrameSizes() {
        super(Parity.MATRIX_HEIGHT, Parity.MATRIX_WIDTH);
    }
}
