/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.create.*;


/**
 * 
 * A factory to create a ScanField interface.
 * @author mrk
 *
 */
public class ScanFieldFactory {
    /**
     * Create a ScanField.
     * The record instance must have a top level field named "pvType"
     * that must be a "pvType" structure as defined in the
     * menuStructureSupportDBD.xml file that appears in javaIOC/dbd.
     * ScanFieldFactory does no locking so code that uses it must be thread safe.
     * In general this means that the record instance must be locked when any method is called. 
     * @param dbRecord The record instance.
     * @return The ScanField interface or null of the record instance does not have
     * a valid pvType field.
     */
    public static ScanField create(DBRecord dbRecord) {
        PVRecord pvRecord = dbRecord.getPVRecord();
        Structure recordStructure = (Structure)pvRecord.getField();
        PVField[] pvFields = pvRecord.getPVFields();
        int index;
        PVField pvField;  
        index = recordStructure.getFieldIndex("scan");
        if(index<0) {
            pvRecord.message("field scan does not exist", MessageType.fatalError);
            return null;
        }
        
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvStructure){
            pvRecord.message("field scan is not a structure", MessageType.fatalError);
            return null;
        }
        DBStructure dbScan = (DBStructure)dbRecord.getDBStructure().getFieldDBFields()[index];
        DBField[] dbFields = dbScan.getFieldDBFields();
        PVStructure pvScan = (PVStructure)pvField;
        Structure structure = pvScan.getStructure();
        if(!structure.getStructureName().equals("scan")) {
            pvScan.message("is not a scan structure", MessageType.fatalError);
            return null;
        }
        pvFields = pvScan.getPVFields(); 
        index = structure.getFieldIndex("priority");
        if(index<0) {
            pvScan.message("does not have field priority", MessageType.fatalError);
            return null;
        }
        Enumerated enumerated = ScanPriority.getScanPriority(dbFields[index]);
        if(enumerated==null) {
            pvField.message("priority is not a scanPriority structure", MessageType.fatalError);
            return null;
        }
        PVInt pvPriority = enumerated.getIndexField();
        index = structure.getFieldIndex("type");
        if(index<0) {
            pvScan.message("does not have field type", MessageType.fatalError);
            return null;
        }
        enumerated = ScanType.getScanType(dbFields[index]);
        if(enumerated==null) {
            pvField.message("type is not a scanType structure", MessageType.fatalError);
            return null;
        }
        PVInt pvType = enumerated.getIndexField();
        
        index = structure.getFieldIndex("rate");
        if(index<0) {
            pvField.message("does not have a field pvRate", MessageType.fatalError);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvDouble) {
            pvField.message("is not a double", MessageType.fatalError);
            return null;
        }
        PVDouble rateField = (PVDouble)pvField;
        index = structure.getFieldIndex("eventName");
        if(index<0) {
            pvField.message("does not have a field eventName", MessageType.fatalError);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvString) {
            ((PVField)pvField).message("is not a string", MessageType.fatalError);
            return null;
        }
        PVString eventNameField = (PVString)pvField;
        index = structure.getFieldIndex("processSelf");
        if(index<0) {
            pvField.message("does not have a field processSelf", MessageType.fatalError);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvBoolean) {
            ((PVField)pvField).message("is not a boolean", MessageType.fatalError);
            return null;
        }
        PVBoolean processSelfField = (PVBoolean)pvField;
        return new ScanFieldInstance(dbScan,pvPriority,pvType,rateField,eventNameField,processSelfField);
    }
    
    
    private static class ScanFieldInstance implements ScanField{
        private PVInt pvPriority;
        private PVInt pvType;
        private PVDouble pvRate;
        private PVString pvEventName;
        private PVBoolean pvProcessSelf;
        
        private ScanFieldInstance(DBField scanField,PVInt pvPriority, PVInt pvType,
            PVDouble pvRate, PVString pvEventName, PVBoolean pvProcessSelfField)
        {
            super();
            this.pvPriority = pvPriority;
            this.pvType = pvType;
            this.pvRate = pvRate;
            this.pvEventName = pvEventName;
            this.pvProcessSelf = pvProcessSelfField;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getEventName()
         */
        public String getEventName() {
            return pvEventName.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getPriority()
         */
        public ScanPriority getPriority() {
            return ScanPriority.values()[pvPriority.get()];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getRate()
         */
        public double getRate() {
            return pvRate.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getScanType()
         */
        public ScanType getScanType() {
            return ScanType.values()[pvType.get()];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessSelf()
         */
        public boolean getProcessSelf() {
            return pvProcessSelf.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getEventNamePV()
         */
        public PVString getEventNamePV() {
            return pvEventName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getPriorityPV()
         */
        public PVInt getPriorityIndexPV() {
            return pvPriority;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessSelfPV()
         */
        public PVBoolean getProcessSelfPV() {
            return pvProcessSelf;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getRatePV()
         */
        public PVDouble getRatePV() {
            return pvRate;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getScanTypePV()
         */
        public PVInt getScanTypeIndexPV() {
            return pvType;
        }        
    }
}
