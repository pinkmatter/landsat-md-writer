/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class can be used to write a binary byte matrix to an image file, where
 * each byte represents one bit.
 *
 * @author anton
 */
public class BinaryMatrixImageWriter {

    public void write(byte[][] matrix, String filePath) throws IOException {
        write(matrix, new File(filePath));
    }

    public void write(byte[][] matrix, File file) throws IOException {
        write(matrix, file, "png");
    }

    public void write(byte[][] matrix, File file, String imageType) throws IOException {
        write(matrix, file, imageType, Color.WHITE, Color.BLACK);
    }

    /**
     * Write the matrix to an image file, where each matrix entry represents one
     * bit.
     *
     * @param matrix The matrix, with each byte being 0 or 1. May not be empty.
     * @param file The output file.
     * @param imageType One of "png", "jpg" or "bmp" etc.
     * @param zeroColor The color for zero bit pixels.
     * @param oneColor The color for one bit pixels.
     *
     * @throws IOException If the matrix is empty or a write error occurs.
     */
    public void write(byte[][] matrix, File file, String imageType, Color zeroColor, Color oneColor) throws IOException {
        if (matrix.length == 0) {
            throw new IOException("Could not write image, matrix is empty.");
        }
        BufferedImage img = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < matrix.length; y++) {
            byte[] row = matrix[y];
            for (int x = 0; x < row.length; x++) {
                byte value = row[x];
                img.setRGB(x, y, (value == 0 ? zeroColor : oneColor).getRGB());
            }
        }
        ImageIO.write(img, imageType, file);
    }
}
