/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

/**
 *
 * @author anton
 */
public class LdpcCirculantFactory {

    /**
     * Populates a subset of matrix with a square circulant matrix generated
     * from the given first row of the circulant. Each subsequent row of the
     * circulant is the same as the first row but shifted once to the right.
     * Values that overflow on the right of the circulant are inserted again
     * from the left.
     *
     * @param bitMatrix The matrix for which a square subset will be populated.
     * Each byte represents one bit.
     * @param startRow The starting row of the matrix where the circulant will
     * be inserted.
     * @param startColumn The starting column of the matrix where the circulant
     * will be inserted.
     * @param circulantSize The width and height, in bits, of the circulant.
     * @param circulantFirstRow The first row of the circulant. Each integer
     * represents one bit.
     */
    public void populate(byte[][] bitMatrix, int startRow, int startColumn, int circulantSize, int[] circulantFirstRow) {
        for (int row = 0; row < circulantSize; row++) {
            for (int column = 0; column < circulantFirstRow.length; column++) {
                int value = circulantFirstRow[column];
                setCirculantBit(bitMatrix, startRow, startColumn, circulantSize, row, column, value);
            }
        }
    }

    /**
     * Populates a subset of matrix with a sparse square circulant matrix
     * generated from the given first row of the circulant. Each subsequent row
     * of the circulant is the same as the first row but shifted once to the
     * right. Values that overflow on the right of the circulant are inserted
     * again from the left.
     *
     * The circulant is sparse, which means that it has a lot more unset (zero)
     * bits that set (one) bits. This also means that the first row of the
     * circulant is efficiently described by an array that only contains the
     * index of each set (one) bit.
     *
     *
     * @param bitMatrix The matrix for which a square subset will be populated.
     * Each byte represents one bit.
     * @param startRow The starting row of the matrix where the circulant will
     * be inserted.
     * @param startColumn The starting column of the matrix where the circulant
     * will be inserted.
     * @param circulantSize The width and height, in bits, of the circulant.
     * @param circulantFirstRowSetBits The first row descriptor of the
     * circulant. Each integer represents the index of a set (1) bit in the
     * first row.
     */
    public void populateSparse(byte[][] bitMatrix, int startRow, int startColumn, int circulantSize, int[] circulantFirstRowSetBits) {
        for (int row = 0; row < circulantSize; row++) {
            for (int column : circulantFirstRowSetBits) {
                setCirculantBit(bitMatrix, startRow, startColumn, circulantSize, row, column, 1);
            }
        }
    }

    private void setCirculantBit(byte[][] bitMatrix, int startRow, int startColumn, int circulantSize, int row, int column, int value) {
        int shiftedColumnIndex = startColumn + getShiftedColumn(column, row, circulantSize);
        bitMatrix[startRow + row][shiftedColumnIndex] = (byte) value;
    }

    private int getShiftedColumn(int column, int row, int columns) {
        return (column + row) % columns;
    }
}
