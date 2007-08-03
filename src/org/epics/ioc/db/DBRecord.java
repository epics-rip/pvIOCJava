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
public interface DBRecord {
    /**
     * Given a PVField find the corresponding DBField.
     * @param pvField The pvField.
     * @return The corresponding DBField.
     */
    DBField findDBField(PVField pvField);
    /**
     * Get the PVRecord that has the data for this IOC record instance.
     * @return The PVRecord interface.
     */
    PVRecord getPVRecord();
    /**
     * Get the interface to the subfields of this record.
     * @return The DBStructure interface.
     */
    DBStructure getDBStructure();
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
     * This must be called by a client that wants to call DBField.addListener for one or more
     * fields of this record instance.
     * @param listener The DBListener interface.
     * @return A RecordListener interface.
     */
    RecordListener createRecordListener(DBListener listener);
    /**
     * Remove a RecordListener interface.
     * This also removes all calls to DBField.addListener; 
     * @param listener The Listen interface returned by the call to createListener.
     */
    void removeRecordListener(RecordListener listener);
    /**
     * Remove all listeners.
     * Any code that modifies the structure of a record must call this before making modifications.
     */
    void removeRecordListeners();
    /**
     * Used for communication between BasePVRecord and BaseDBField.
     * DBField calls this the first time DBField.addListener is called.
     * @param dbField The DBField instance.
     */
    void addListenerSource(DBField dbField);
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
     * This can put so user code should never save the return value.
     * @return The current IOC database.
     */
    IOCDB getIOCDB();
    /**
     * Set the IOCDB.
     * @param iocdb The iocdb to which this record belongs.
     * This is called after an IOCDB is merged into the master to put it to the master.
     */
    void setIOCDB(IOCDB iocdb);
    /**
     * Convert the DBRecord to a string.
     * @return The string.
     */
    String toString();
    /**
     * Convert the DBRecord to a string.
     * Each line is indented.
     * @param indentLevel The indentation level.
     * @return The string.
     */
    String toString(int indentLevel);
}
