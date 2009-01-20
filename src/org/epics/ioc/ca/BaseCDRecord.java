/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;

/**
 * Base class for a CDRecord (Channel Data Record).
 * @author mrk
 *
 */
public class BaseCDRecord implements CDRecord {
    private PVDataCreate pvDataCreate;
    private PVRecord pvRecord;
    private CDStructure cdStructure;
    
    /**
     * Constructor.
     * @param pvDataCreate Factory to create PVField objects.
     * @param recordName The record, i.e. channel name.
     * @param channelFieldGroup The channelFieldGroup for the CDRecord.
     */
    public BaseCDRecord(PVDataCreate pvDataCreate,
        String recordName,ChannelFieldGroup channelFieldGroup)
    {
        this.pvDataCreate = pvDataCreate;
        ChannelField[] channelFields = channelFieldGroup.getArray();
        int length = channelFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) {
            newFields[i] = channelFields[i].getPVField().getField();
        }
        pvRecord = pvDataCreate.createPVRecord(recordName, newFields);
        cdStructure = new BaseCDStructure(null,this,pvRecord,channelFields);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#findCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findCDField(PVField pvField) {

        return cdStructure.findCDField(pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#findSourceCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findSourceCDField(PVField sourcePVField) {
        return cdStructure.findSourceCDField(sourcePVField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getCDBStructure()
     */
    public CDStructure getCDStructure() {
        return cdStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getPVDataCreate()
     */
    public PVDataCreate getPVDataCreate() {
        return pvDataCreate;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getPVRecord()
     */
    public PVRecord getPVRecord() {
        return pvRecord;
    }
}
