/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.encode;

import farearth.landsat.ldpc.LdpcMatrix;

/**
 *
 * @author anton
 */
public abstract class LdpcGeneratorMatrix extends LdpcMatrix {

    public LdpcGeneratorMatrix(int width, int height) {
        super(width, height);
    }

    protected abstract void populateCirculants(byte[][] matrix);

    @Override
    protected void populate(byte[][] matrix) {
        populateIdentity(matrix);
        populateCirculants(matrix);
    }

    private void populateIdentity(byte[][] matrix) {
        for (int i = 0; i < getHeight(); i++) {
            matrix[i][i] = 1;
        }
    }
}
