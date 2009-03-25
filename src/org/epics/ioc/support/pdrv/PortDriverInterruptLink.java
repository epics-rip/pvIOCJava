/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;
import org.epics.ioc.support.Support;

/**
 * @author mrk
 *
 */
public interface PortDriverInterruptLink extends Support {
    /**
     * Is this the record processor?
     * @return (true,false) if it (is ,is not) the record processor.
     */
    boolean isProcess();
}
