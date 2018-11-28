/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.ldpc.decode.Landsat8LdpcDecoderFast;
import farearth.landsat.quality.CaduQualityMetric;
import farearth.landsat.quality.NoCaduLostPerTimeMetric;
import farearth.landsat.quality.QualityAwareFileHandler;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 *
 * @author Chris
 */
public class FrameSyncBuilder {

    private final Map<Integer, List<FileHandler>> _additionalHandlers = new HashMap<>();
    private final Properties _properties = new Properties();
    private File _passPropertyFile;
    private File _ssohFolder = null;
    private Properties _additionalPassProperties;
    private boolean _queueSsoh;
    private Landsat8LdpcDecoderFast _ldpcDecoder;
    private boolean _derandomize = false;
    private int _startRecordingAfterErrorFreeData = 0;

    public FrameSyncBuilder writePlaybackData(File folder, boolean queued) {
        if (folder != null) {
            addMissionDataWriter(VCID.PLAYBACK_OLI, folder, queued, false, null);
            addMissionDataWriter(VCID.PLAYBACK_TIRS, folder, queued, false, null);
        }
        return this;
    }

    public FrameSyncBuilder writeRealtimeData(File folder, boolean queued) {
        return writeRealtimeData(folder, queued, true);
    }

    public FrameSyncBuilder writeRealtimeData(File folder, boolean queued, boolean writePassProperties) {
        return writeRealtimeData(folder, queued, writePassProperties, this::getQualityMetric);
    }

    public FrameSyncBuilder writeRealtimeData(File folder, boolean queued, boolean writePassProperties, Function<Integer, CaduQualityMetric> metricFactory) {
        if (folder != null) {
            addMissionDataWriter(VCID.REALTIME_OLI, folder, queued, true, metricFactory.apply(VCID.REALTIME_OLI));
            addMissionDataWriter(VCID.REALTIME_TIRS, folder, queued, true, metricFactory.apply(VCID.REALTIME_TIRS));
            if (writePassProperties) {
                writePassProperties(new File(folder, "pass.properties"));
            }
        }
        return this;
    }

    public FrameSyncBuilder processRealtimeOli(FileHandler handler, boolean queued, String... bands) {
        return process(VCID.REALTIME_OLI, handler, queued, bands);
    }

    public FrameSyncBuilder processRealtimeTirs(FileHandler handler, boolean queued, String... bands) {
        return process(VCID.REALTIME_TIRS, handler, queued, bands);
    }

    public FrameSyncBuilder processPlaybackOli(FileHandler handler, boolean queued, String... bands) {
        return process(VCID.PLAYBACK_OLI, handler, queued, bands);
    }

    public FrameSyncBuilder processPlaybackTirs(FileHandler handler, boolean queued, String... bands) {
        return process(VCID.PLAYBACK_TIRS, handler, queued, bands);
    }

    public FrameSyncBuilder processLdpc(boolean derandomize) {
        return processLdpc(derandomize, true, 60);
    }

    public FrameSyncBuilder processLdpc(boolean derandomize, boolean detectErrors, int maxErrorsToFix) {
        _derandomize = derandomize;
        _ldpcDecoder = new Landsat8LdpcDecoderFast(detectErrors, maxErrorsToFix);
        return this;
    }

    private FrameSyncBuilder process(int vcID, FileHandler handler, boolean queued, String... bands) {
        if (queued) {
            handler = new QueuedFileHandler.Block(handler, 20, String.format("queued-processor-VC%02d", vcID));
        }
        return write(vcID, new ApidFilter.Bands(bands), handler);
    }

    public FrameSyncBuilder writeStateOfHealthData(File folder, boolean queued) {
        _ssohFolder = folder;
        _queueSsoh = queued;
        return this;
    }

    private void addMissionDataWriter(int vc, File folder, boolean queuedWriting, boolean handlePartials, CaduQualityMetric metric) {
        FileHandler handler;
        MissionDataWriter writer = new MissionDataWriter(folder, String.format("VC%02d", vc), createVcProperties(String.valueOf(vc)));
        writer.setIgnoreLeadingPartials(!handlePartials);
        writer.setDeleteTrailingPartials(!handlePartials);
        if (metric == null) {
            handler = writer;
        } else {
            QualityAwareFileHandler qafh = new QualityAwareFileHandler(writer, metric, folder);
            qafh.setProperties(_properties);
            handler = qafh;
        }
        if (queuedWriting) {
            add(vc, new QueuedFileHandler.Block(handler, 50, String.format("queued-md-writer-VC%02d", vc))); //TODO size
        } else {
            add(vc, handler);
        }
    }

    private FrameSyncBuilder write(int vc, ApidFilter filter, FileHandler handler) {
        add(vc, new FileHandler.Filtered(handler, filter));
        return this;
    }

    public FrameSyncBuilder writePassProperties(File file) {
        return writePassProperties(file, null);
    }

    public FrameSyncBuilder writePassProperties(File file, Properties additionalProperties) {
        _additionalPassProperties = additionalProperties;
        _passPropertyFile = file;
        return this;
    }

    private void add(int vc, FileHandler handler) {
        List<FileHandler> handlers = _additionalHandlers.get(vc);
        if (handlers == null) {
            handlers = new LinkedList<>();
            _additionalHandlers.put(vc, handlers);
        }
        handlers.add(handler);
    }

    public FrameSynchronizer build() {
        FrameSynchronizer result;
        Map<Integer, PayloadHandler> handlers = new HashMap<>();
        for (Map.Entry<Integer, List<FileHandler>> entry : _additionalHandlers.entrySet()) {
            int vc = entry.getKey();
            List<FileHandler> fileHandlers = entry.getValue();
            if (fileHandlers != null) {
                FileMultiplexer mux = new FileMultiplexer(fileHandlers.toArray(new FileHandler[fileHandlers.size()]));
                CaduHandler caduHandler = createMDStack(mux);
                handlers.put(vc, caduHandler);
            }
        }
        if (_ssohFolder != null) {
            PayloadHandler writer0 = new SsohFileWriter(0, _ssohFolder);
            PayloadHandler writer1 = new SsohFileWriter(1, _ssohFolder);
            if (_queueSsoh) {
                writer0 = new QueuedPayloadHandler.Block(writer0, 20);
                writer1 = new QueuedPayloadHandler.Block(writer1, 20);
            }
            handlers.put(VCID.STATE_OF_HEALTH_0, writer0);
            handlers.put(VCID.STATE_OF_HEALTH_1, writer1);
        }
        FrameSynchronizer sync = new Landsat8FrameSynchronizer(handlers);
        if (_ldpcDecoder != null) {
            sync = new Landsat8LdpcFrameSynchronizer(_ldpcDecoder, _derandomize, sync);
        }
        if (_passPropertyFile != null) {
            ReportingFrameSynchronizer wrapper = new ReportingFrameSynchronizer(sync);
            wrapper.setOutputPropertiesFile(_passPropertyFile);
            if (_additionalPassProperties != null) {
                _properties.putAll(_additionalPassProperties);
            }
            wrapper.setAdditionalProperties(_properties);
            result = wrapper;
        } else {
            result = sync;
        }
        return result;
    }

    private static Properties createVcProperties(String vc) {
        Properties p = new Properties();
        p.put("vc", vc);
        return p;
    }

    private CaduHandler createMDStack(FileHandler fileHandler) {
        CaduHandler handler = new CaduHandler(
                new MpduHandler(
                        new MpduPayloadAssembler(
                                new SpacePacketHandler(
                                        new CfdpHandler(fileHandler)
                                )
                        )
                )
        );
        return handler;
    }

    private CaduQualityMetric getQualityMetric(int vcID) {
        if (_startRecordingAfterErrorFreeData <= 0) {
            return null;
        } else {
            return new NoCaduLostPerTimeMetric(vcID, _startRecordingAfterErrorFreeData);
        }
    }

    public void setStartRecordingAfterErrorFreeData(int millis) {
        _startRecordingAfterErrorFreeData = millis;
    }
}
