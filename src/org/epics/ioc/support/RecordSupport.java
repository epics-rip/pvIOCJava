/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;
import org.epics.pvData.pv.PVField;
/**
 * Every record instance in a support database has a RecordSupport.
 * @author mrk
 *
 */
public interface RecordSupport {
    /**
     * Get the recordProcess for this record.
     * @return The interface.
     */
    RecordProcess getRecordProcess();
    /**
     * Set the recordProcess for this record.
     * @param recordProcess
     */
    void setRecordProcess(RecordProcess recordProcess);
    /**
     * Get the support for the pvField.
     * @param pvField The field.
     * @return The interface or null if the field does not have support.
     */
    Support getSupport(PVField pvField);
    /**
     * Set the support for the field.
     * @param pvField The field.
     * @param support The support.
     */
    void setSupport(PVField pvField,Support support);
}
