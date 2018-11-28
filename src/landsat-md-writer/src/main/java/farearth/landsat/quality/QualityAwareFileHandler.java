/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.quality;

import farearth.landsat.APID;
import farearth.landsat.FileHandler;
import farearth.landsat.util.Slice;
import java.io.File;
import java.io.IOException;
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
public class QualityAwareFileHandler implements FileHandler {

    private final Logger LOG = LoggerFactory.getLogger(QualityAwareFileHandler.class);
    private final FileHandler _goodCaduHandler;
    private boolean _writingStarted = false;
    private final CaduQualityMetric _metric;
    private final FileHandler _badCaduHandler;
    private long _writingStartedTime = 0;
    private long _lostBytes = 0;
    private long _startTime = 0;
    private Properties _properties;
    private boolean _initialized = false;

    public QualityAwareFileHandler(FileHandler goodCaduHandler) {
        this(goodCaduHandler, new NoCaduQualityMetric(), new FileHandler.Null());
    }

    public QualityAwareFileHandler(FileHandler goodCaduHandler, CaduQualityMetric metric, File outputFolder) {
        this(goodCaduHandler, metric, new JunkFileHandler(outputFolder));
    }

    public QualityAwareFileHandler(FileHandler goodCaduHandler, CaduQualityMetric metric, FileHandler badCaduHandler) {
        _goodCaduHandler = goodCaduHandler;
        _badCaduHandler = badCaduHandler;
        _metric = metric;
    }

    @Override
    public void startFile(String name) throws IOException {
        updateStartTime();
        if (shouldWrite()) {
            _goodCaduHandler.startFile(name);
        } else {
            _badCaduHandler.startFile(name);
        }
    }

    @Override
    public void endFile(long filesize, int checksum) throws IOException {
        updateStartTime();
        if (shouldWrite()) {
            _goodCaduHandler.endFile(filesize, checksum);
        } else {
            _badCaduHandler.endFile(filesize, checksum);
        }
    }

    @Override
    public void fileData(Slice data, APID apid, int length, long offset) throws IOException {
        updateStartTime();
        if (shouldWrite()) {
            _goodCaduHandler.fileData(data, apid, length, offset);
        } else {
            _lostBytes += length;
            _badCaduHandler.fileData(data, apid, length, offset);
        }
    }

    @Override
    public void close() throws IOException {
        if (_properties != null) {
            _metric.updateProperties(_properties);
            SimpleDateFormat format = new SimpleDateFormat("yyyyDDDHHmmssSSS");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            _properties.put("quality-recording-start-time", format.format(new Date(_writingStartedTime)));
            _properties.put("quality-lost-bytes", String.valueOf(_lostBytes));
            _properties.put("quality-lost-time", String.valueOf(_writingStartedTime - _startTime));
        }
        closeMetricSafely(_metric);
        try {
            _goodCaduHandler.close();
        } finally {
            _badCaduHandler.close();
        }
    }

    private void closeMetricSafely(AutoCloseable closable) {
        try {
            closable.close();
        } catch (Throwable t) {
            LOG.error("Exception on attempting to close metric safely: " + t.getMessage(), t);
        }
    }

    private boolean shouldWrite() {
        if (_writingStarted) {
            return true;
        } else {
            if (!_initialized) {
                _metric.init();
                _initialized = true;
            }
            if (_metric.isHealthy()) {
                LOG.info("Starting MD recording - {}", _metric.getDescription());
                _writingStartedTime = System.currentTimeMillis();
                _writingStarted = true;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void dataLost() {
        _goodCaduHandler.dataLost();
    }

    private void updateStartTime() {
        if (_startTime == 0) {
            _startTime = System.currentTimeMillis();
        }
    }

    public void setProperties(Properties properties) {
        _properties = properties;
    }

    public Properties getProperties() {
        return _properties;
    }

}
