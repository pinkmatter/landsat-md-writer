/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public class SsohFileWriter extends TimestampingPayloadWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SsohFileWriter.class);
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy_D_HH_mm_ss");
    private final int _fileID;

    public SsohFileWriter(int fileID, File outputFolder) {
        super(outputFolder, ".ssoh");
        _fileID = fileID;
    }

    @Override
    protected void fileComplete(File file, long caduCount, Date startTime, Date endTime) {
        String filename = String.format("Data%d.Start%s.Stop%s", _fileID,
                FORMAT.format(startTime), FORMAT.format(endTime));
        File newFile = new File(file.getParent(), filename);
        try {
            Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Completed SSOH file {} with {} CADUs", newFile, caduCount);
        } catch (IOException ex) {
            LOG.error("Failed to rename file from " + file + " to " + newFile, ex);
        }
    }

    @Override
    protected String getFilenamePrefix(Date startTime) {
        return String.format("Data%d.Start%s_", _fileID, FORMAT.format(startTime));
    }

}
