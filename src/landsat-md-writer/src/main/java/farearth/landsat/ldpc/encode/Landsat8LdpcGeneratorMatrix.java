/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.encode;

import static farearth.landsat.ldpc.Landsat8LdpcConstants.CIRCULANT_SIZE;
import farearth.landsat.ldpc.Landsat8LdpcConstants.Generator;
import farearth.landsat.ldpc.LdpcCirculantFactory;
import farearth.landsat.util.BitUtils;

/**
 *
 * @author anton
 */
public class Landsat8LdpcGeneratorMatrix extends LdpcGeneratorMatrix {

    public Landsat8LdpcGeneratorMatrix() {
        super(Generator.MATRIX_WIDTH, Generator.MATRIX_HEIGHT);
    }

    @Override
    protected void populateCirculants(byte[][] matrix) {
        final int circRows = Generator.IDENTITY_SIZE;
        final int circCols = circRows + Generator.CIRCULANT_COLUMNS;
        LdpcCirculantFactory circulantFactory = new LdpcCirculantFactory();
        for (int circRow = 0; circRow < circRows; circRow++) { // 0; < 14
            for (int circCol = circRows; circCol < circCols; circCol++) { // 14; < 16
                String firstRowHex = Generator.CIRCULANTS[circRow][circCol - Generator.IDENTITY_SIZE];
                int[] firstRow = BitUtils.expandFromHexString(firstRowHex, 1, CIRCULANT_SIZE);
                int startRow = circRow * CIRCULANT_SIZE;
                int startColumn = circCol * CIRCULANT_SIZE;
                circulantFactory.populate(matrix, startRow, startColumn, CIRCULANT_SIZE, firstRow);
            }
        }
    }
}
