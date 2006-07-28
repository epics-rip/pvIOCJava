/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * interface that must be implemented by record or structure support.
 * @author mrk
 *
 */
public interface RecordSupport extends Support {
    /**
     * Perform record or structure processing.
     * @param listener The listener to call when returning active.
     * @return The result of processing.
     */
    ProcessReturn process(ProcessListener listener);
}
