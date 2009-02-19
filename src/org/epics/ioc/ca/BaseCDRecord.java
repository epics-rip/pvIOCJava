/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;

/**
 * Base class for a CDRecord (Channel Data Record).
 * @author mrk
 *
 */
public class BaseCDRecord implements CDRecord {
    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private PVRecord pvRecord;
    private CDStructure cdStructure;
    
    /**
     * Constructor.
     * @param recordName The record, i.e. channel name.
     * @param channelFieldGroup The channelFieldGroup for the CDRecord.
     */
    public BaseCDRecord(String recordName,ChannelFieldGroup channelFieldGroup)
    {
        ChannelField[] channelFields = channelFieldGroup.getArray();
        int length = channelFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) {
            newFields[i] = channelFields[i].getPVField().getField();
        }
        pvRecord = pvDataCreate.createPVRecord(recordName, newFields);
        PVField[] pvFields = pvRecord.getPVFields();
        for(int i=0; i<length; i++) {
            PVField pvFrom = channelFields[i].getPVField();
            PVField pvTo = pvFields[i];
            switch(pvFrom.getField().getType()) {
            case scalar:
                convert.copyScalar((PVScalar)pvFrom, (PVScalar)pvTo);
                break;
            case scalarArray: {
                PVArray pvArray = (PVArray)pvFrom;
                convert.copyArray(pvArray,0,(PVArray)pvTo,0, pvArray.getLength());;
                break;
            }
            case structure:
                convert.copyStructure((PVStructure)pvFrom, (PVStructure)pvTo);
                break;
            }
        }
        cdStructure = new BaseCDStructure(null,this,pvRecord,null,channelFields);
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
     * @see org.epics.ioc.ca.CDRecord#getPVRecord()
     */
    public PVRecord getPVRecord() {
        return pvRecord;
    }
}
