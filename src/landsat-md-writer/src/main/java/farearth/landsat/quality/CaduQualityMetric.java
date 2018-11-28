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
public interface CaduQualityMetric extends AutoCloseable {

    void init();

    boolean isHealthy();

    String getDescription();

    void updateProperties(Properties props);

}
