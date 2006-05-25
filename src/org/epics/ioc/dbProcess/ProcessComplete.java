/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * A callback for announcing completion on record processing.
 * @author mrk
 *
 */
public interface ProcessComplete {
    /**
     * processing done.
     * @param result why done.
     */
    void complete(ProcessReturn result);
}
