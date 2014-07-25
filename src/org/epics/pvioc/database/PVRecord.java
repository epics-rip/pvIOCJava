/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.database;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.support.RecordProcess;



/**
 * PVRecord interrace.
 * @author mrk
 *
 */
public interface PVRecord {
	/**
	 * Get the recordProcess for this record.
	 * @return The interface.
	 * An exception is thrown if the record has not been assigned a recordProvcess.
	 */
	RecordProcess getRecordProcess();
	/**
	 * Set the recordProcess for this field.
	 * A field can have at most one recordProcess.
	 * An exception is thrown if this is called for a record that already has a recordProcess.
	 * @param recordProcess The interface.
	 */
	void setRecordProcess(RecordProcess recordProcess);
	/**
	 * Find the PVRecordField for the pvField.
	 * @param pvField The pvField interface.
	 * @return The PVRecordField interface or null is not in record.
	 */
	PVRecordField findPVRecordField(PVField pvField);
	/**
     * Get the top level PVRecordStructure.
     * @return The PVRecordStructure interface.
     */
	PVRecordStructure getPVRecordStructure();
    /**
     * Get the record instance name.
     * @return The name.
     */
    String getRecordName();
    /**
     * Report a message.
     * The record name will be appended to the message.
     * @param message The message.
     * @param messageType The message type.
     */
    void message(String message, MessageType messageType);
    /**
     * Add a requester to receive messages.
     * @param requester The requester to add.
     */
    void addRequester(Requester requester);
    /**
     * Remove a message requester.
     * @param requester The requester to remove.
     */
    void removeRequester(Requester requester);
    /**
     * Lock the record instance.
     * This must be called before accessing the record.
     */
    void lock();
    /**
     * Unlock the record.
     */
    void unlock();
    /**
     * Try to lock the record instance.
     * This can be called before accessing the record instead of lock.
     * @return If true then it is just like lock. If false the record must not be accessed.
     */
    boolean tryLock();
    /**
     * While holding lock on this record lock another record.
     * If the other record is already locked than this record may be unlocked.
     * The caller must call the unlock method of the other record when done with it.
     * @param otherRecord the other record.
     */
    void lockOtherRecord(PVRecord otherRecord);
    /**
     * Begin a group of related puts.
     */
    void beginGroupPut();
    /**
     * End of a group of related puts.
     */
    void endGroupPut();
    /**
     * Register a PVListener. This must be called before pvField.addListener.
     * @param pvListener The listener.
     */
    void registerListener(PVListener pvListener);
    /**
     * Unregister a PVListener.
     * @param pvListener The listener.
     */
    void unregisterListener(PVListener pvListener);
    /**
     * Is this pvListener registered? This is called by AbstractPVField. addListener.
     * @param pvListener The listener.
     * @return (false,true) if the listener (is not, is) registered.
     */
    boolean isRegisteredListener(PVListener pvListener);
    /**
     * Remove every PVListener.
     */
    void removeEveryListener();
    /**
     * Register a client of the record.
     * This must be called by any code that connects to the record.
     * @param pvRecordClient The record client.
     */
    void registerClient(PVRecordClient pvRecordClient);
    /**
     * Unregister a client of the record.
     * This must be called by any code that disconnects from the record.
     * @param pvRecordClient The record client.
     */
    void unregisterClient(PVRecordClient pvRecordClient);
    /**
     * Detach all registered clients.
     */
    void detachClients();
    /**
     * Get the number of registerd clients.
     * @return The number.
     */
    int getNumberClients();
    /**
     * Implement standard toString().
     * @return The record as a String.
     */
    String toString();
    /**
     * Implement standard toString()
     * @param indentLevel indentation level.
     * @return The record as a string.
     */
    String toString(int indentLevel);
    /**
     * Check that PVRecordStruvture is compatible with PVStructure. 
     */
    boolean checkValid();
}
