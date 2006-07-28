/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.pvAccess.*;

/**
 * interface that must be implemented by link support.
 * @author mrk
 *
 */
public interface LinkSupport extends Support {
    /**
     * set the field for which the link support should get/put data.
     * @param field the field.
     * @return true if the support can access the field
     * and false if the support does not know how to access the field.
     */
    boolean setField(PVData field);
    /**
     * Request processing.
     * @param listener The listener to call when returning active.
     * @return The result.
     */
    LinkReturn process(LinkListener listener);
}
