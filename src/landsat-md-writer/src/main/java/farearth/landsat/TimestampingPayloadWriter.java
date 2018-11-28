/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 *
 * @author Chris
 */
public abstract class TimestampingPayloadWriter implements PayloadHandler {

    private WritableByteChannel _channel;
    private File _tempFile;
    private Date _startTime;
    private Date _endTime;
    private long _caduCount = 0;
    private boolean _started = false;

    private final File _outputFolder;
    private final String _suffix;

    public TimestampingPayloadWriter(File outputFolder, String suffix) {
        _outputFolder = outputFolder;
        _suffix = suffix;
    }

    @Override
    public void payload(Slice data) throws IOException {
        if (!_started) {
            _startTime = new Date();
            _endTime = new Date();
            _tempFile = File.createTempFile(getFilenamePrefix(_startTime), _suffix, _outputFolder);
            _channel = FileChannel.open(_tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            _started = true;
            _caduCount = 0;
        }
        _endTime.setTime(System.currentTimeMillis());
        data.read(_channel);
        _caduCount++;
    }

    @Override
    public void dataLost() {
    }

    @Override
    public void close() throws IOException {
        try {
            if (_channel != null) {
                _channel.close();
                fileComplete(_tempFile, _caduCount, _startTime, _endTime);
            }
        } finally {
            _started = false;
            _channel = null;
            _tempFile = null;
            _startTime = null;
            _endTime = null;
        }
    }

    protected String getFilenamePrefix(Date startTime) {
        return "";
    }

    protected abstract void fileComplete(File file, long caduCount, Date startTime, Date endTime);

}
