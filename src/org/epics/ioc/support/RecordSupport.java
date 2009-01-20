/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;
import org.epics.pvData.pv.PVField;
/**
 * @author mrk
 *
 */
public interface RecordSupport {
    RecordProcess getRecordProcess();
    void setRecordProcess(RecordProcess recordProcess);
    Support getSupport(PVField pvField);
    void setSupport(PVField pvField,Support support);
}
