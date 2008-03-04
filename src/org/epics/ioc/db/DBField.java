/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;
import java.util.List;

import org.epics.ioc.create.Create;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.support.Support;


/**
 * The base interface for accessing a field of an IOC record.
 * @author mrk
 *
 */
public interface DBField {
    /**
     * Get the DBRecord that contains this dbField.
     * @return The DBRecord interface.
     */
    DBRecord getDBRecord();
    /**
     * Get the parent of this dbField.
     * @return The DBField interface.
     * The parent can have one of the following types:
     * <ol>
     *   <li>pvStructure</li>
     *   <li>pvArray with a elementType of
     *   <ol>
     *      <li>pvStructure</li>
     *      <li>pvArray</li>
     *      <li>pvStructure></li>
     *   </ol></li>
     * </ol>
     * If this is the DBStructure for the record itself <i>null</i> is returned.
     */
    DBField getParent();
    /**
     * Get the pvField for this dbField.
     * @return The pvField interface.
     */
    PVField getPVField();
    /**
     * Replace the pvField with a new implementation.
     * This calls pvField.replacePVField();
     * @param newPVField The new pvField implementation.
     */
    void replacePVField(PVField newPVField);
    /**
     * Get the support name if it exists.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Set the name of the support or null to specify no support.
     * @param name The name.
     * @return <i>null</i> if the supportName was changed or the reason why the request failed.
     */
    String setSupportName(String name);
    /**
     * Get the Create interface for this field.
     * @return The interface or null if no create exists.
     */
    Create getCreate();
    /**
     * Set the Create interface.
     * @param create The interface.
     */
    void setCreate(Create create);
    /**
     * Look to see if create should be replaced.
     */
    void replaceCreate();
    /**
     * Get the Support for the field.
     * @return The support or null if no support exists.
     */
    Support getSupport();
    /**
     * Set the support.
     * @param support The support.
     */
    void setSupport(Support support);
    /**
     * The data was modified.
     * This is must be called by the code that calls a put method. 
     */
    void postPut();
    /**
     * The data was modified via dbField.
     * @param dbField The parent field that was modified.
     */
    void postPut(DBField dbField);
    /**
     * Add a listener to this field.
     * @param recordListener The recordListener created by calling dbRecord.createRecordListener.
     */
    void addListener(RecordListener recordListener);
    /**
     * remove a recordListener.
     * This is called by dbRecord.removeRecordListener.
     * Thus normally a client does not need to call this.
     * @param recordListener The recordListener to remove.
     */
    void removeListener(RecordListener recordListener);
    /**
     * Get the list of RecordListeners.
     * @return The list.
     */
    List<RecordListener> getRecordListenerList();
    /**
     * Convert the data to a string.
     * @return The string.
     */
    String toString();
    /**
     * Convert the data to a string.
     * Each line is indented.
     * @param indentLevel The indentation level.
     * @return The string.
     */
    String toString(int indentLevel);
}
