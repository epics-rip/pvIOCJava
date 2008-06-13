/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;


/**
 * DB listener interface.
 * @author mrk
 *
 */
public interface DBListener {
    /**
     * The data in the dbField has been modified.
     * @param dbField The data.
     */
    void dataPut(DBField dbField);
    /**
     * A put to a subfield has occurred.
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. Thus the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructure</li>
     *        <li>pvArray</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param dbField The data that has been modified.
     */
    void dataPut(DBField requested,DBField dbField);
    /**
     * Begin record processing.
     * Between begin and end of record processing,
     * dataPut may be called 0 or more times.
     */
    void beginProcess();
    /**
     * End of record processing.
     */
    void endProcess();
    /**
     * Connection to record is being terminated and the RecordListener is no longer valid.
     * @param listener The recordListener interface.
     */
    void unlisten(RecordListener listener);
}
