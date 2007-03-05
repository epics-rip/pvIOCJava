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
     * A scalar or array modification has occured.
     * @param dbField The data.
     */
    void dataPut(DBField dbField);
    /**
     * The enum index has been modified.
     * @param dbEnum The enum interface.
     */
    void enumIndexPut(DBEnum dbEnum);
    /**
     * The enum choices has been modified.
     * @param dbEnum The enum interface.
     */
    void enumChoicesPut(DBEnum dbEnum);
    /**
     * The supportName has been modified.
     * @param dbField
     */
    void supportNamePut(DBField dbField);
    /**
     * The link configuration structure has been modified.
     * @param dbLink The link interface.
     */
    void configurationStructurePut(DBLink dbLink);
    /**
     * Start of a structure modification.
     * @param dbStructure The structure.
     */
    void beginPut(DBStructure dbStructure);
    /**
     * End of a structure modification.
     * @param dbStructure The structure.
     */
    void endPut(DBStructure dbStructure);
    /**
     * A put to a subfield has occured.
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. This the pvType can be.
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
     * A put to an enum subfield has occured.
     * The enum index has been modified.
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. This the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructure</li>
     *        <li>pvArray</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param dbEnum The enum interface.
     */
    void enumIndexPut(DBField requested,DBEnum dbEnum);
    /**
     * A put to an enum subfield has occured.
     * The enum choices has been modified.
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. This the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructure</li>
     *        <li>pvArray</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param dbEnum The enum interface.
     */
    void enumChoicesPut(DBField requested,DBEnum dbEnum);
    /**
     * The supportName of a subfield has been modified..
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. This the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructure</li>
     *        <li>pvArray</li>
     *        <li>pvLink</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param dbField
     */
    void supportNamePut(DBField requested,DBField dbField);
    /**
     * The configuration structure of a link subfield has been modified..
     * @param requested The requester is listening to this dbField.
     * It can be any field that has subfields. This the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructurte</li>
     *        <li>pvArray</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param dbLink The link interface.
     */
    void configurationStructurePut(DBField requested,DBLink dbLink);
    /**
     * Begin record processing.
     * From begin until end of record processing,
     * each newData returns data modified while record is being processed.
     */
    void beginProcess();
    /**
     * End of record processing.
     */
    void endProcess();
    /**
     * Connection to record is being terminated and the RecordListener is no longer valid.
     */
    void unlisten(RecordListener listener);
}
