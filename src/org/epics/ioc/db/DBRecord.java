/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;



/**
 * Interface for a record instance.
 * @author mrk
 *
 */
public interface DBRecord extends DBData, PVRecord {
    /**
     * Lock the record instance.
     * This must be called before accessing anything contained in the record.
     */
    void lock();
    /**
     * Unlock the record.
     */
    void unlock();
    /**
     * While holding lock on this record lock another record.
     * If the other record is already locked than this record may be unlocked.
     * The caller must call the unlock method of the other record when done with it.
     * @param otherRecord the other record.
     */
    void lockOtherRecord(DBRecord otherRecord);
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
     * Get the id for this record instance.
     * Each instance is assigned a unique integer id.
     * @return The id.
     */
    int getRecordID();
    /**
     * Begin a set of record processing.
     */
    void beginProcess();
    /**
     * End of record processing.
     */
    void endProcess();
    /**
     * Create a RecordListener.
     * This must be called by a client that wants to call DBData.addListener for one or more
     * fields of this record instance.
     * @param listener The DBListener interface.
     * @return A RecordListener interface.
     */
    RecordListener createRecordListener(DBListener listener);
    /**
     * Remove a RecordListener interface.
     * This also removes all calls to DBData.addListener; 
     * @param listener The Listen interface returned by the call to createListener.
     */
    void removeRecordListener(RecordListener listener);
    /**
     * Remove all listeners.
     * Any code that modifies the structure of a record must call this before making modifications.
     */
    void removeRecordListeners();
    /**
     * Used for communication between AbstractDBRecord and AbstractDBData.
     * AbstractDBData calls this the first time DBData.addListener is called.
     * @param dbData The AbstractDBData instance.
     */
    void addListenerSource(AbstractDBData dbData);
    /**
     * Get the DBD that contains this record.
     * @return The DBD or null if it was never set.
     */
    DBD getDBD();
    /**
     * Set the DBD that contains this record instance.
     * @param dbd The DBD.
     */
    void setDBD(DBD dbd);
    /**
     * Get the IOCDB to which this record belongs.
     * This can change so user code should never save the return value.
     * @return The current IOC database.
     */
    IOCDB getIOCDB();
    /**
     * Set the IOCDB.
     * @param iocdb The iocdb to which this record belongs.
     * This is called after an IOCDB is merged into the master to change it to the master.
     */
    void setIOCDB(IOCDB iocdb);
}
