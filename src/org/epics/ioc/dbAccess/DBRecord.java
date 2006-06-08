/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbProcess.*;


/**
 * interface for a record instance.
 * @author mrk
 *
 */
public interface DBRecord extends DBStructure {
    /**
     * get the record instance name.
     * @return the name.
     */
    String getRecordName();
    /**
     * get the record support for this record instance.
     * @return the RecordSupport or null if no support has been set.
     */
    RecordSupport getRecordSupport();
    /**
     * set the record support.
     * @param support the support.
     * @return true if the support was set and false if the support already was set.
     */
    boolean setRecordSupport(RecordSupport support);
    /**
     * lock record for reading.
     */
    void readLock();
    /**
     * unlock record for reading.
     */
    void readUnlock();
    /**
     * lock record for writing.
     */
    void writeLock();
    /**
     * unlock record for writing.
     */
    void writeUnlock();
    /**
     * insert a master listener.
     * When a master listener is active postPut() calls only the master listener.
     * The masterListener calls postPut(dbData) or postPut(iterator)to call the other listeners.
     * @param listener the listener.
     * @return true if that caller is now the master listener and false if a master listener is
     * already active.
     */
    boolean insertMasterListener(DBMasterListener listener);
    /**
     * remove the master listener.
     * @param listener the listener.
     * @throws IllegalStateException if the caller is not the master.
     */
    void removeMasterListener(DBMasterListener listener);
    /**
     * called by putPost() to see if only the master listener should be called.
     * @param dbData the actual DBData object to be posted.
     * @return true if a master exists and was called.
     * If false is returned than the caller should call putPost(this).
     */
    boolean postMaster(DBData dbData);
    /**
     * master is beginning a set of synchronous puts.
     * @throws IllegalStateException if the caller is not the master.
     */
    void beginSynchronous();
    /**
     * end of synchronous puts from the master.
     * @throws IllegalStateException if the caller is not the master.
     */
    void stopSynchronous();
    /**
     * post puts for the master.
     * @param dbData the data to post.
     * @throws IllegalStateException if the caller is not the master.
     */
    void postForMaster(DBData dbData);
    /**
     * ONLY for use by DBData.
     * This is called by DBData when its addListener is called.
     * @param listener the listener.
     */
    void addListener(DBListenerPvt listener);
    /**
     * ONLY for use by DBData.
     * This is called by DBData when its removeListener is called.
     * @param listener the listener.
     */
    void removeListener(DBListenerPvt listener);
}
