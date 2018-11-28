/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.Landsat8LdpcConstants.Parity;

/**
 *
 * @author anton
 */
public class Landsat8LdpcParityGraph extends LdpcParityGraph<Landsat8LdpcParityMatrix> {

    public Landsat8LdpcParityGraph(Landsat8LdpcParityMatrix matrix) {
        //rowBitCol = [1022][32] = [row][0-32] = column
        //colBitNum = [1022][32] = [row][0-32] = column set bit index
        //colBitRow = [8176][4] = [col][0-4] = row
        //rowBitNum = [8176][4] = [col][0-4] = row set bit index
        super(matrix, Parity.ROW_WEIGHT, Parity.COL_WEIGHT);
    }
}
