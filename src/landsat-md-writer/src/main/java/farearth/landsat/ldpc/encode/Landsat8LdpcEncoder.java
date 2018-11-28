/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.encode;

import farearth.landsat.ldpc.Landsat8LdpcConstants.Parity;

/**
 *
 * @author anton
 */
public class Landsat8LdpcEncoder extends LdpcEncoder<Landsat8LdpcGeneratorMatrix> {

    public Landsat8LdpcEncoder() {
        super(new Landsat8LdpcGeneratorMatrix(), Parity.MATRIX_HEIGHT, Parity.MATRIX_WIDTH);
    }
}
