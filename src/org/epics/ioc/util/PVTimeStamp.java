/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.pvAccess.*;

/**
 * A convenience class for a timeStamp field.
 * This class does no locking.
 * The code that uses it must be thread safe, which means that the
 * associated record must be locked while the field is accessed.
 * @author mrk
 *
 */
public class PVTimeStamp {
    private PVLong secondsPastEpoch;
    private PVInt nanoSeconds;
    
    /**
     * Given a pvData create a PVTimeStamp if the field is actually
     * a timeStamp structure.
     * @param pvData The field.
     * @return A PVTimeStamp or null if the field is not a timeStamp structure.
     */
    public static PVTimeStamp create(PVData pvData) {
        if(pvData.getField().getType()!=Type.pvStructure) return null;
        PVStructure structure = (PVStructure)pvData;
        PVData[] pvDatas = structure.getFieldPVDatas();
        if(pvDatas.length!=2) return null;
        PVData fieldPvData = pvDatas[0];
        Field field = fieldPvData.getField();
        if(field.getType()!=Type.pvLong) return null;
        if(!field.getName().equals("secondsPastEpoch")) return null;
        PVLong secondsPastEpoch = (PVLong)fieldPvData;
        fieldPvData = pvDatas[1];
        field = fieldPvData.getField();
        if(field.getType()!=Type.pvInt) return null;
        if(!field.getName().equals("nanoSeconds")) return null;
        PVInt nanoSeconds = (PVInt)fieldPvData; 
        return new PVTimeStamp(secondsPastEpoch,nanoSeconds);
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
    }
    
    private PVTimeStamp(PVLong secondsPastEpoch,PVInt nanoSeconds){
        this.secondsPastEpoch = secondsPastEpoch;
        this.nanoSeconds = nanoSeconds;
    }
        
}
