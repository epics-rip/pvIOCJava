/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;
import org.epics.pvData.pv.*;

/**
 * Create a PVStructure
 * @author mrk
 *
 */
public interface CreatePVStructure {
    /**
     * Create the request
     */
    PVStructure create();
}
