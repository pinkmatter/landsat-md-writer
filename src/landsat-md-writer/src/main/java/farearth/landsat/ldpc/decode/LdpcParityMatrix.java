/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.LdpcMatrix;

/**
 *
 * @author anton
 */
public abstract class LdpcParityMatrix extends LdpcMatrix {

    public LdpcParityMatrix(int width, int height) {
        super(width, height);
    }
}
