/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.util.ByteMatrix;

/**
 *
 * @author anton
 */
public abstract class LdpcMatrix implements ByteMatrix {

    private final int width;
    private final int height;
    private final byte[][] matrix; // each byte represents a bit in the matrix

    public LdpcMatrix(int width, int height) {
        this.width = width;
        this.height = height;
        this.matrix = new byte[height][width];
        populate(matrix);
    }

    protected abstract void populate(byte[][] matrix);

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public byte[][] getRawMatrix() {
        return matrix;
    }
}
