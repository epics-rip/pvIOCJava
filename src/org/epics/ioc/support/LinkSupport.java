/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;

/**
 * Interface that must be implemented by support for a channel access link field.
 * @author mrk
 *
 */
public interface LinkSupport extends Support {
    /**
     * Get the configuration structure for this support.
     * @param structureName The expected struvture name.
     * @param reportFailure Issue an error message if no configStructure.
     * @return The PVStructure or null if the structure is not located.
     */
    PVStructure getConfigStructure(String structureName, boolean reportFailure);
}
