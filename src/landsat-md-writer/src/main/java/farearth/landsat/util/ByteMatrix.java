/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

/**
 *
 * @author anton
 */
public interface ByteMatrix {

    public int getWidth();

    public int getHeight();

    /**
     * Provide access to the raw matrix for when high performance is required.
     *
     * @return The raw inner matrix.
     */
    public byte[][] getRawMatrix();

    default public byte[] getRow(int row) {
        return getRawMatrix()[row];
    }

    default public byte getValue(int row, int column) {
        return getRawMatrix()[row][column];
    }
}
