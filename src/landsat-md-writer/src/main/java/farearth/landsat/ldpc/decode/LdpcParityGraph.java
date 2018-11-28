/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

/**
 *
 * @author anton
 * @param <T> A check matrix, where each byte represents one bit.
 */
public class LdpcParityGraph<T extends LdpcParityMatrix> {

    private final int rowWeight;
    private final int columnWeight;
    // "set bit index..." means the index of the set bit in that column/row
    // where the first set bit is index 0, the next set bit index 1 etc.
    private int[][] rowBitCol; // [row][set bit index for row] = column index
    private int[][] colBitNum; // [row][set bit index for row] = set bit index in column
    private int[][] colBitRow; // [col][set bit index for col] = row index
    private int[][] rowBitNum; // [col][set bit index for col] = set bit index in row

    public LdpcParityGraph(T parityMatrix, int rowWeight, int columnWeight) {
        this.rowWeight = rowWeight;
        this.columnWeight = columnWeight;
        init(parityMatrix, rowWeight, columnWeight);
    }

    public int getRows() {
        return rowBitCol.length;
    }

    public int getColumns() {
        return colBitRow.length;
    }

    public int getRowWeight() {
        return rowWeight;
    }

    public int getColumnWeight() {
        return columnWeight;
    }

    // Allow access to matrix directly for performance
    public int[][] getRowColumnMatrix() {
        return rowBitCol;
    }

    public int getRow(int column, int columnBitNum) {
        return colBitRow[column][columnBitNum];
    }

    public int getRowBitNumber(int column, int columnBitNum) {
        return rowBitNum[column][columnBitNum];
    }

    public int getColumn(int row, int rowBitNum) {
        return rowBitCol[row][rowBitNum];
    }

    public int getColumnBitNumber(int row, int rowBitNum) {
        return colBitNum[row][rowBitNum];
    }

    private void init(T parityMatrix, int rowWeight, int columnWeight) {
        int height = parityMatrix.getHeight();
        int width = parityMatrix.getWidth();
        rowBitCol = new int[height][rowWeight];
        colBitNum = new int[height][rowWeight];
        colBitRow = new int[width][columnWeight];
        rowBitNum = new int[width][columnWeight];

        int[] columnBitNums = new int[width];
        byte[][] matrix = parityMatrix.getRawMatrix(); // for performance
        for (int r = 0; r < matrix.length; r++) { // rows
            byte[] row = matrix[r];
            int rBitNum = 0;
            for (int c = 0; c < row.length; c++) { // columns
                byte value = row[c];
                if (value != 0) {
                    int cBitNum = columnBitNums[c];

                    rowBitCol[r][rBitNum] = c;
                    colBitNum[r][rBitNum] = cBitNum;

                    colBitRow[c][cBitNum] = r;
                    rowBitNum[c][cBitNum] = rBitNum;

                    rBitNum++;
                    columnBitNums[c]++;
                }
            }
        }
    }
}
