/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;


/**
 * 
 * A factory to create a ScanField interface.
 * @author mrk
 *
 */
public class ScanFieldFactory {
    /**
     * Create a ScanField.
     * This is called by RecordProcessFactory.
     * If the record instance does not have a field named scan then null is returned.
     * If it does the field must be a scan structure.
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
        if(index<0) return null;
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvStructure){
            pvRecord.message("field scan is not a structure", MessageType.fatalError);
            return null;
        }
        DBStructure dbScan = (DBStructure)dbRecord.getDBStructure().getDBFields()[index];
        DBField[] dbFields = dbScan.getDBFields();
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
        PVDouble pvRate = (PVDouble)pvField;
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
        PVString pvEventName = (PVString)pvField;
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
        PVBoolean pvProcessSelf = (PVBoolean)pvField;
        index = structure.getFieldIndex("processAfterStart");
        if(index<0) {
            pvField.message("does not have a field processAfterStart", MessageType.fatalError);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvBoolean) {
            ((PVField)pvField).message("is not a boolean", MessageType.fatalError);
            return null;
        }
        PVBoolean pvProcessAfterStart = (PVBoolean)pvField;
        return new ScanFieldInstance(dbScan,pvPriority,pvType,pvRate,pvEventName,pvProcessSelf,pvProcessAfterStart);
    }
    
    
    private static class ScanFieldInstance implements ScanField{
        private PVInt pvPriority;
        private PVInt pvType;
        private PVDouble pvRate;
        private PVString pvEventName;
        private PVBoolean pvProcessSelf;
        private PVBoolean pvProcessAfterStart;
        
        private ScanFieldInstance(DBField scanField,PVInt pvPriority, PVInt pvType,
            PVDouble pvRate, PVString pvEventName, PVBoolean pvProcessSelfField, PVBoolean pvProcessAfterStart)
        {
            super();
            this.pvPriority = pvPriority;
            this.pvType = pvType;
            this.pvRate = pvRate;
            this.pvEventName = pvEventName;
            this.pvProcessSelf = pvProcessSelfField;
            this.pvProcessAfterStart = pvProcessAfterStart;
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
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessAfterStart()
         */
        public boolean getProcessAfterStart() {
            return pvProcessAfterStart.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessAfterStartPV()
         */
        public PVBoolean getProcessAfterStartPV() {
            return pvProcessAfterStart;
        }        
    }
}
