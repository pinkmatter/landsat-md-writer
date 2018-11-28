/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.quality;

import farearth.landsat.util.OneShotTimeout;
import java.util.Properties;

/**
 *
 * @author Chris
 */
public class NoCaduLostPerTimeMetric implements CaduQualityMetric {

    private final int _minimumNoDropTime;
    private final int _vcID;
    private final OneShotTimeout _timeout;
    private boolean _healthy;

    public NoCaduLostPerTimeMetric(int vcID, int minimumNoDropTimeMillis) {
        _minimumNoDropTime = minimumNoDropTimeMillis;
        _vcID = vcID;
        _timeout = new OneShotTimeout(String.format("cadus-lost-timeout-vc%d", vcID), this::setHealthy);
    }

    @Override
    public boolean isHealthy() {
        return _healthy;
    }

    @Override
    public String getDescription() {
        return String.format("no CADU drops in %dms", getMinimumNoDropTime());
    }

    private void setHealthy(boolean healthy) {
        _healthy = healthy;
    }

    @Override
    public void updateProperties(Properties props) {
        props.put("quality-minimum-no-drop-time", String.valueOf(_minimumNoDropTime));
    }

    public int getMinimumNoDropTime() {
        return _minimumNoDropTime;
    }

    @Override
    public void init() {
        _timeout.restart(_minimumNoDropTime); // if we never encounter any cadu drops, the lambda above will never trigger - hence force timer on init to ensure we react at least once
    }

    @Override
    public void close() throws Exception {
        _timeout.close();
    }

}
