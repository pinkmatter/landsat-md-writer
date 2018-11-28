/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.encode;

import farearth.landsat.util.BitUtils;

/**
 *
 * @author anton
 */
public class LdpcCompactGeneratorMatrix {

    private final byte[][] matrix; // [column][byte in column]

    public LdpcCompactGeneratorMatrix(LdpcGeneratorMatrix genMatrix, int virtualBitCount) {
        byte[][] rawMatrix = genMatrix.getRawMatrix();
        int bytesPerColumn = (int) Math.ceil(genMatrix.getHeight() / 8.0);
        matrix = new byte[genMatrix.getWidth()][bytesPerColumn];
        for (int r = virtualBitCount; r < rawMatrix.length; r++) {
            byte[] row = rawMatrix[r];
            for (int c = 0; c < row.length; c++) {
                byte bitValue = row[c];
                if (bitValue != 0) {
                    BitUtils.setBit(matrix[c], r - virtualBitCount);
                }
            }
        }
    }

    public byte[][] getRawMatrix() {
        return matrix;
    }
}
