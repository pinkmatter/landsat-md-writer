/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.ByteSizeFormatter;
import farearth.landsat.util.Slice;
import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public interface FileHandler extends AutoCloseable, Closeable {

    void startFile(String name) throws IOException;

    void endFile(long filesize, int checksum) throws IOException;

    void fileData(Slice data, APID apid, int length, long offset) throws IOException;

    @Override
    void close() throws IOException;

    void dataLost();

    public static class Null implements FileHandler {

        @Override
        public void startFile(String name) {
        }

        @Override
        public void endFile(long filesize, int checksum) {
        }

        @Override
        public void fileData(Slice data, APID apid, int length, long offset) {
        }

        @Override
        public void close() {
        }

        @Override
        public void dataLost() {
        }

    }

    public static abstract class Abstract implements FileHandler {

        private final Logger LOG = LoggerFactory.getLogger(Abstract.class);
        private long _bytesReceived = 0;
        private long _bytesLost = 0;
        private long _lastOffset = -1;
        private int _lastBytesReceived = 0;
        private boolean _ignoreLeadingPartials = false;

        private String _currentFilename = null;
        private Date _fileStart;
        private boolean _isWriting = false;
        private Properties _defaultProperties;

        public Abstract() {
            this(null);
        }

        public Abstract(Properties defaultProperties) {
            _defaultProperties = defaultProperties;
        }

        @Override
        public final void startFile(String name) throws IOException {
            if (_isWriting) {
                endFile(-1, 0, -1, true);
                clearCounters();
            }
            _currentFilename = name;
            _fileStart = new Date();
            LOG.debug("Starting file {}", name);
        }

        private void clearCounters() {
            _bytesReceived = 0;
            _bytesLost = 0;
            _lastOffset = -1;
            _fileStart = null;
            _isWriting = false;
        }

        @Override
        public final void endFile(long filesize, int checksum) throws IOException {
            LOG.debug("Ending file {}, size={}, received={}, lost={}, checksum={}",
                    _currentFilename, filesize, _bytesReceived, filesize - _bytesReceived, checksum);
            if (_isWriting) {
                endFile(filesize, checksum, filesize - _bytesReceived, false);
            }
            clearCounters();
        }

        @Override
        public final void fileData(Slice data, APID apid, int length, long offset) throws IOException {
            if (length == data.remaining()) {
                if (!isIgnoreLeadingPartials() || !isLeadingPartial()) {
                    _bytesReceived += length;
                    if (_lastOffset >= 0) {
                        long delta = offset - _lastOffset;
                        long lost = delta - _lastBytesReceived;
                        if (delta > 0 && lost > 0) {
                            _bytesLost += lost;
                            LOG.debug("Lost data {}", lost);
                        } else if (lost != 0) {
                            LOG.debug("Lost data", lost);
                        }
                    }
                    _lastBytesReceived = length;
                    _lastOffset = offset;
                    if (!_isWriting) {
                        startFile(_currentFilename, _fileStart, new Date(), isLeadingPartial());
                        _isWriting = true;
                    }
                    writeData(data, offset);
                }
            } else {
                LOG.warn(String.format("Data MISMATCH! length=%d, remaining=%d", length, data.remaining()));
            }
        }

        private boolean isLeadingPartial() {
            return _currentFilename == null;
        }

        @Override
        public final void close() throws IOException {
            LOG.debug("Closing open files");
            if (_isWriting) {
                endFile(-1, 0, -1, true);
            }
            clearCounters();
            onClose();
        }

        protected abstract void onClose();

        public long getBytesReceived() {
            return _bytesReceived;
        }

        public long getBytesLost() {
            return _bytesLost;
        }

        public boolean isIgnoreLeadingPartials() {
            return _ignoreLeadingPartials;
        }

        public void setIgnoreLeadingPartials(boolean ignoreLeadingPartials) {
            _ignoreLeadingPartials = ignoreLeadingPartials;
        }

        public Properties getDefaultProperties() {
            return _defaultProperties;
        }

        public void setDefaultProperties(Properties defaultProperties) {
            _defaultProperties = defaultProperties;
        }

        protected abstract void startFile(String name, Date fileStart, Date dataStart, boolean isLeadingPartial) throws IOException;

        protected abstract void endFile(long filesize, int checksum, long lostBytes, boolean isTrailingPartial) throws IOException;

        protected abstract void writeData(Slice data, long offset) throws IOException;

    }

    public static class Printer extends Abstract {

        private final Logger LOG = LoggerFactory.getLogger(Printer.class);
        private long _dataReceived = 0;
        private final long _printThreshold = 100 * 1024 * 1024;
        private final String _prefix;

        public Printer() {
            this("");
        }

        public Printer(String prefix) {
            super();
            _prefix = prefix;
        }

        @Override
        protected void startFile(String name, Date fileStart, Date dataStart, boolean isLeadingPartial) throws IOException {
            if (isLeadingPartial) {
                LOG.info("{}Starting leading partial", _prefix);
            } else {
                LOG.info("{}Starting file {}", _prefix, name);
            }
            _dataReceived = 0;
        }

        @Override
        protected void endFile(long filesize, int checksum, long lostBytes, boolean isTrailingPartial) throws IOException {
            if (isTrailingPartial) {
                LOG.info("{}Closing trailing partial", _prefix);
            } else {
                LOG.info("{}Closing file with {} lost bytes", _prefix, lostBytes);
            }
            _dataReceived = 0;
        }

        @Override
        protected void writeData(Slice data, long offset) throws IOException {
            _dataReceived += data.remaining();
            if (_dataReceived >= _printThreshold) {
                LOG.info("{}Received {}", _prefix, ByteSizeFormatter.format(getBytesReceived()));
                _dataReceived = 0;
            }
        }

        @Override
        public void dataLost() {
            LOG.info("Data lost...");
        }

        @Override
        protected void onClose() {
        }

    }

    public static class Filtered implements FileHandler {

        private final ApidFilter _filter;
        private final FileHandler _delegate;

        public Filtered(FileHandler delegate, ApidFilter filter) {
            _delegate = delegate;
            _filter = filter;
        }

        @Override
        public void startFile(String name) throws IOException {
            _delegate.startFile(name);
        }

        @Override
        public void endFile(long filesize, int checksum) throws IOException {
            _delegate.endFile(filesize, checksum);
        }

        @Override
        public void fileData(Slice data, APID apid, int length, long offset) throws IOException {
            if (_filter.pass(apid)) {
                _delegate.fileData(data, apid, length, offset);
            }
        }

        @Override
        public void close() throws IOException {
            _delegate.close();
        }

        @Override
        public void dataLost() {
            _delegate.dataLost();
        }

    }

}
