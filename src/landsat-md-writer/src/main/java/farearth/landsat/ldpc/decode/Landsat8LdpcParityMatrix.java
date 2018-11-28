/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import static farearth.landsat.ldpc.Landsat8LdpcConstants.CIRCULANT_SIZE;
import farearth.landsat.ldpc.Landsat8LdpcConstants.Parity;
import farearth.landsat.ldpc.LdpcCirculantFactory;

/**
 *
 * @author anton
 */
public class Landsat8LdpcParityMatrix extends LdpcParityMatrix {

    public Landsat8LdpcParityMatrix() {
        super(Parity.MATRIX_WIDTH, Parity.MATRIX_HEIGHT);
    }

    @Override
    protected void populate(byte[][] matrix) {
        LdpcCirculantFactory circulantFactory = new LdpcCirculantFactory();
        for (int circulantsRow = 0; circulantsRow < Parity.CIRCULANTS.length; circulantsRow++) {
            int[][] firstRows = Parity.CIRCULANTS[circulantsRow];
            for (int circulantColumn = 0; circulantColumn < firstRows.length; circulantColumn++) {
                int[] circulantFirstRow = firstRows[circulantColumn];
                int matrixStartRow = circulantsRow * CIRCULANT_SIZE;
                int matrixStartColumn = circulantColumn * CIRCULANT_SIZE;
                circulantFactory.populateSparse(matrix, matrixStartRow, matrixStartColumn, CIRCULANT_SIZE, circulantFirstRow);
            }
        }
    }
}
