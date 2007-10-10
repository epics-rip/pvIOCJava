/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;

/**
 * A convenience class for a timeStamp field.
 * This class does no locking.
 * The code that uses it must be thread safe, which means that the
 * associated record must be locked while the field is accessed.
 * @author mrk
 *
 */
public class PVTimeStamp {
    private DBField dbField;
    private PVLong secondsPastEpoch;
    private PVInt nanoSeconds;
    
    /**
     * Constructor.
     * @param dbField The dbField. It must be a PVStructure that is a timeStamp.
     * @return The PVTimeStamp interface.
     */
    public static PVTimeStamp create(DBField dbField) {
        PVTimeStamp pvTimeStamp = create(dbField.getPVField());
        if(pvTimeStamp!=null) pvTimeStamp.setDBField(dbField);
        return pvTimeStamp;
    }
    /**
     * Given a pvField create a PVTimeStamp if the field is actually
     * a timeStamp structure.
     * @param pvField The field.
     * @return A PVTimeStamp or null if the field is not a timeStamp structure.
     */
    public static PVTimeStamp create(PVField pvField) {
        if(pvField.getField().getType()!=Type.pvStructure) return null;
        PVStructure timeStamp = (PVStructure)pvField;
        PVField[] pvFields = timeStamp.getPVFields();
        if(pvFields.length!=2) return null;
        PVField fieldPvField = pvFields[0];
        Field field = fieldPvField.getField();
        if(field.getType()!=Type.pvLong) return null;
        if(!field.getFieldName().equals("secondsPastEpoch")) return null;
        PVLong secondsPastEpoch = (PVLong)fieldPvField;
        fieldPvField = pvFields[1];
        field = fieldPvField.getField();
        if(field.getType()!=Type.pvInt) return null;
        if(!field.getFieldName().equals("nanoSeconds")) return null;
        PVInt nanoSeconds = (PVInt)fieldPvField; 
        return new PVTimeStamp(pvField,secondsPastEpoch,nanoSeconds);
    }
    
    /**
     * Get the current field value.
     * @param timeStamp The TimeStamp to receive the current value.
     */
    public void get(TimeStamp timeStamp) {
        timeStamp.secondsPastEpoch = secondsPastEpoch.get();
        timeStamp.nanoSeconds = nanoSeconds.get();
    }
    
    /**
     * Put the current value from TimeStamp.
     * @param timeStamp The new value.
     */
    public void put(TimeStamp timeStamp) {
        secondsPastEpoch.put(timeStamp.secondsPastEpoch);
        nanoSeconds.put(timeStamp.nanoSeconds);
        if(dbField!=null) dbField.postPut();
    }
    
    private PVTimeStamp(PVField pvField,PVLong secondsPastEpoch,PVInt nanoSeconds){
        this.secondsPastEpoch = secondsPastEpoch;
        this.nanoSeconds = nanoSeconds;
    }
    
    private void setDBField(DBField dbField) {
        this.dbField = dbField;
    }
        
}
