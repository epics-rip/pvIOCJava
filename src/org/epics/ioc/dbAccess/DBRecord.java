/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.concurrent.locks.*;

import org.epics.ioc.dbProcess.*;



/**
 * interface for a record instance.
 * @author mrk
 *
 */
public interface DBRecord extends DBStructure {
    /**
     * Get the record instance name.
     * @return The name.
     */
    String getRecordName();
    /**
     * Get the RecordProcess for this record instance.
     * @return The RecordProcess or null if  has been set.
     */
    RecordProcess getRecordProcess();
    /**
     * Set the RecordProcess.
     * @param recordProcess The RecordProcess for this record instance.
     * @return true if the support was set and false if the support already was set.
     */
    boolean setRecordProcess(RecordProcess recordProcess);
    /**
     * Get the record support for this record instance.
     * @return The RecordSupport or null if no support has been set.
     */
    RecordSupport getRecordSupport();
    /**
     * Set the record support.
     * @param support The support.
     * @return true if the support was set and false if the support already was set.
     */
    boolean setRecordSupport(RecordSupport support);
    /**
     * Get the lock for the record instance.
     * @return A reentrant lock.
     */
    ReentrantLock getLock();
    /**
     * While holding lock on this record lock another record.
     * If the other record is already locked than this record may be unlocked.
     * @param otherRecord the other record.
     * @return TODO
     */
    ReentrantLock lockOtherRecord(DBRecord otherRecord);
    /**
     * Get the id for this record instance.
     * Each instance is assigned a unique integer id.
     * @return The id.
     */
    int getRecordID();
    /**
     * Begin a set of synchronous puts.
     */
    void beginSynchronous();
    /**
     * End of synchronous puts.
     */
    void endSynchronous();
    /**
     * create a Listener.
     * This must be called by a client that wants to call DBData.addListener for one or more
     * fields of this record instance.
     * @param listener the DBListener interface.
     * @return a Listener interface.
     */
    Listener createListener(DBListener listener);
    /**
     * destroy a Listener interface.
     * Before calling this the client must call DBData.removeListener for each
     * DBData.addListener it has called.
     * @param listener the Listen interface returned by the call to createListener.
     */
    void destroyListener(Listener listener);
}
