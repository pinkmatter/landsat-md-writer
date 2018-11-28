/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
class ReportingFrameSynchronizer implements FrameSynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(ReportingFrameSynchronizer.class);

    private final FrameSynchronizer _delegate;
    private File _outputProperties;
    private Properties _additionalProperties;

    public ReportingFrameSynchronizer(FrameSynchronizer delegate) {
        _delegate = delegate;
    }

    @Override
    public void process(ByteBuffer buffer) throws IOException {
        _delegate.process(buffer);
    }

    @Override
    public void close() throws IOException {
        try {
            _delegate.close();
        } finally {
            if (isWriteProperties()) {
                writeProperties();
            }
        }
    }

    public boolean isWriteProperties() {
        return getOutputPropertiesFile() != null;
    }

    public File getOutputPropertiesFile() {
        return _outputProperties;
    }

    public void setOutputPropertiesFile(File outputProperties) {
        _outputProperties = outputProperties;
    }

    private void writeProperties() throws IOException {

    }

    public Properties getAdditionalProperties() {
        return _additionalProperties;
    }

    public void setAdditionalProperties(Properties additionalProperties) {
        _additionalProperties = additionalProperties;
    }

}
