/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.quality;

import java.util.Properties;

/**
 *
 * @author Chris
 */
public class NoCaduQualityMetric implements CaduQualityMetric {

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getDescription() {
        return "no quality metric";
    }

    @Override
    public void updateProperties(Properties props) {
    }

    @Override
    public void init() {
    }

    @Override
    public void close() throws Exception {

    }

}
