/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;
import java.util.List;

import org.epics.ioc.process.Support;
import org.epics.ioc.pv.*;


/**
 * The base interface for accessing a field of an IOC record.
 * @author mrk
 *
 */
public interface DBData {
    /**
     * Get the DBRecord that contains this dbData.
     * @return The DBRecord interface.
     */
    DBRecord getDBRecord();
    /**
     * Get the parent of this dbData.
     * @return The DBData interface.
     * The parent can have one of the following types:
     * <ol>
     *   <li>pvStructure</li>
     *   <li>pvArray with a elementType of
     *   <ol>
     *      <li>pvStructure</li>
     *      <li>pvArray</li>
     *      <li>pvLink></li>
     *   </ol></li>
     * </ol>
     * If this is the DBStructure for the record itself <i>null</i> is returned.
     */
    DBData getParent();
    /**
     * Get the pvData for this dbData.
     * @return The pvData interface.
     */
    PVData getPVData();
    /**
     * Replace the pvData with a new implementation.
     * This calls pvData.replacePVData();
     * @param newPVData The new pvData implementation.
     */
    void replacePVData(PVData newPVData);
    /**
     * Get the support name if it exists.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Set the name of the support or null to specify no support.
     * @param name The name.
     * @return null is the supportName was changed or the reason why it was not set.
     */
    String setSupportName(String name);
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
     * This is must be called by the code that implements a put method. 
     */
    void postPut();
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
