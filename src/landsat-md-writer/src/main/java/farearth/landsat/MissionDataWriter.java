/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public class MissionDataWriter extends FileHandler.Abstract {

    private static final Logger LOG = LoggerFactory.getLogger(MissionDataWriter.class);

    private WritableByteChannel _out;
    private final File _outputFolder;
    private final String _leadingPartialPrefix;
    private final SimpleDateFormat _suffixFormat;
    private File _file;
    private boolean _deleteTrailingPartials = false;
    private boolean _writeProperties = true;

    public MissionDataWriter(File outputFolder, String leadingPartialPrefix) {
        this(outputFolder, leadingPartialPrefix, null);
    }

    public MissionDataWriter(File outputFolder, String leadingPartialPrefix, Properties defaultProperties) {
        super(defaultProperties);
        if (!outputFolder.exists()) {
            throw new IllegalArgumentException("Folder " + outputFolder.getAbsolutePath() + " does not exist.");
        }
        _outputFolder = outputFolder;
        _leadingPartialPrefix = leadingPartialPrefix;
        _suffixFormat = new SimpleDateFormat("yyyyDDDHHmmssSSS");
        _suffixFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected String getFilename(String filename, Date startDate, boolean isLeadingPartial) {
        if (startDate == null) {
            startDate = new Date();
        }
        if (isLeadingPartial) {
            filename = _leadingPartialPrefix;
        }
        return String.format("%s.%s", filename, formatDate(startDate));
    }

    @Override
    protected void startFile(String name, Date fileStart, Date dataStart, boolean isLeadingPartial) throws IOException {
        _file = new File(_outputFolder, getFilename(name, fileStart, isLeadingPartial));
        if (isLeadingPartial) {
            LOG.info("Starting leading partial {} for {}", _file, _leadingPartialPrefix);
        } else {
            LOG.info("Starting file {} for {}", _file, _leadingPartialPrefix);
        }
        _out = Channels.newChannel(new FileOutputStream(_file));
        if (isWriteProperties()) {
            Properties properties = new Properties();
            properties.putAll(getDefaultProperties());

            if (fileStart != null) {
                properties.put("file-start-time", formatDate(fileStart));
            }
            if (dataStart != null) {
                properties.put("data-start-time", formatDate(dataStart));
            }
            properties.put("leading-partial", String.valueOf(isLeadingPartial));
            writeProperties(properties, _file);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return _suffixFormat.format(date);
        }
    }

    private void writeProperties(Properties properties, File forFile) throws IOException {
        String filename = forFile.getName() + ".properties";
        File file = new File(_outputFolder, filename);
        LOG.debug("Writing properties file to {}", file);
        try (FileOutputStream os = new FileOutputStream(file)) {
            properties.store(os, null);
        }
    }

    private Properties readProperties(File forFile) throws IOException {
        String filename = forFile.getName() + ".properties";
        File file = new File(_outputFolder, filename);
        Properties properties = new Properties(getDefaultProperties());
        try (FileInputStream is = new FileInputStream(file)) {
            properties.load(is);
        }
        return properties;
    }

    @Override
    protected void endFile(long filesize, int checksum, long lostBytes, boolean isTrailingPartial) throws IOException {
        String caduCount = "";
        if (!isTrailingPartial || !isDeleteTrailingPartials()) {
            if (isWriteProperties()) {
                Properties properties = readProperties(_file);
                properties.put("end-time", formatDate(new Date()));
                properties.put("trailing-partial", String.valueOf(isTrailingPartial));
                if (!isTrailingPartial) {
                    properties.put("cfdp-checksum", String.valueOf(checksum));
                    properties.put("cfdp-filesize", String.valueOf(filesize));
                    properties.put("bytes-lost", String.valueOf(lostBytes));
                }
                writeProperties(properties, _file);
            }
        }
        if (isTrailingPartial) {
            LOG.info("Closing trailing partial {} for {} {}", _file, _leadingPartialPrefix, caduCount);
        } else {
            LOG.info("Closing file {} for {} with {} lost bytes{}", _file, _leadingPartialPrefix, lostBytes, caduCount);
        }
        _out.close();
        if (isTrailingPartial && isDeleteTrailingPartials()) {
            LOG.info("Deleting trailing partial {}", _file);
            _file.delete();
        }
    }

    @Override
    protected void writeData(Slice data, long offset) throws IOException {
        data.read(_out);
    }

    @Override
    protected void onClose() {
    }

    @Override
    public void dataLost() {
        LOG.debug("Data lost...");
    }

    public boolean isDeleteTrailingPartials() {
        return _deleteTrailingPartials;
    }

    public void setDeleteTrailingPartials(boolean deleteTrailingPartials) {
        _deleteTrailingPartials = deleteTrailingPartials;
    }

    protected boolean isWriteProperties() {
        return _writeProperties;
    }

    protected void setWriteProperties(boolean writeProperties) {
        _writeProperties = writeProperties;
    }

    public static class EmptyFileWriter extends MissionDataWriter {

        public EmptyFileWriter(File outputFolder, String leadingPartialPrefix) throws FileNotFoundException {
            super(outputFolder, leadingPartialPrefix);
        }

        public EmptyFileWriter(File outputFolder, String leadingPartialPrefix, Properties defaultProperties) throws FileNotFoundException {
            super(outputFolder, leadingPartialPrefix, defaultProperties);
        }

        @Override
        protected void writeData(Slice data, long offset) throws IOException {
        }

    }

}
