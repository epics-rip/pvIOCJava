/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * A listener for asynchronous modification of a field.
 * The listener is called at the beginning and end of a synchronous modification.
 * The listener can take whatever actions are necessary to protect the field from other
 * code accessing the field while a modification is in progress.
 * For example device support for a field of a javaIOC record can call
 * dbRecord.lock() and dbRecord.unlock(); 
 * @author mrk
 *
 */
public interface AsynAccessListener {
    /**
     * Begin of a synchronous modification.
     */
    void beginSyncAccess();
    /**
     * End of a synchronous modification.
     */
    void endSyncAccess();
}
