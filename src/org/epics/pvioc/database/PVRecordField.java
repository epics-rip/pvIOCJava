/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.database;

import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.support.Support;

/**
 * PVRecordField is for PVField that are part of a PVRecord.
 * Each PVType has an interface that extends PVField.
 * @author mrk
 *
 */
public interface PVRecordField extends Requester{
	/**
	 * Get the support for this field.
	 * @return The interface or null if the field has no support.
	 */
	Support getSupport();
	/**
	 * Set the support for this field.
	 * A field can have at most one support.
	 * An exception is thrown if this is called for a field that already has support.
	 * @param support
	 */
	void setSupport(Support support);
	/**
	 * Get the parent of this field.
	 * @return The parent interface or null if top level structure.
	 */
	PVRecordStructure getParent();
	/**
	 * Get the PVField.
	 * @return The PVField interface.
	 */
	PVField getPVField();
//	/**
//	 * Replace the pvField.
//	 * @param pvField The new pvField;
//	 */
//	void replacePVField(PVField pvField);
    /**
     * Get the fullFieldName, i.e. the complete hierarchy.
     * @return The name.
     */
    String getFullFieldName();
    /**
     * Get the full name, which is the recordName plus the fullFieldName
     * @return The name.
     */
    String getFullName();
    /**
     * Get the record.
     * @return The record interface.
     */
    PVRecord getPVRecord();
//    /**
//     * Rename the fieldName of the Field interface.
//     * @param newName The new name for the field.
//     */
//    void renameField(String newName);
    /**
     * Add a listener to this field.
     * @param pvListener The pvListener to add to list for postPut notification.
     * @return (false,true) if the pvListener (was not,was) added.
     * If the listener was already in the list false is returned.
     */
    boolean addListener(PVListener pvListener);
    /**
     * remove a pvListener.
     * @param pvListener The listener to remove.
     */
    void removeListener(PVListener pvListener);
    /**
     * post that data has been modified.
     * This must be called by the code that implements put.
     */
    void postPut();
}
