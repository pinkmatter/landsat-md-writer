/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import java.util.List;

/**
 *
 * @author anton
 */
public interface LdpcDecoderContext {

    public void setParityErrorRows(List<Integer> rows);

    public List<Integer> getFinalErrorBits();
}
