/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.Iterator;
import java.util.LinkedList;

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
        PVField[] pvFields = pvRecord.getFieldPVFields();
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
        pvFields = pvScan.getFieldPVFields(); 
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
    
    
    private static class ScanFieldInstance implements ScanField ,DBListener{
        private PVInt pvPriority;
        private PVInt pvType;
        private PVDouble pvRate;
        private PVString pvEventName;
        private PVBoolean pvProcessSelfField;
        
        private LinkedList<ScanFieldModifyListener> scanFieldModifyListenerList
        = new LinkedList<ScanFieldModifyListener>();
        
        
        private ScanFieldInstance(DBField scanField,PVInt pvPriority, PVInt pvType,
            PVDouble pvRate, PVString pvEventName, PVBoolean pvProcessSelfField)
        {
            super();
            this.pvPriority = pvPriority;
            this.pvType = pvType;
            this.pvRate = pvRate;
            this.pvEventName = pvEventName;
            this.pvProcessSelfField = pvProcessSelfField;
            RecordListener recordListener = scanField.getDBRecord().createRecordListener(this);
            scanField.addListener(recordListener);
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
            return pvProcessSelfField.get();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#addModifyListener(org.epics.ioc.util.ScanFieldModifyListener)
         */
        public void addModifyListener(ScanFieldModifyListener modifyListener) {
            scanFieldModifyListenerList.add(modifyListener);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#removeModifyListener(org.epics.ioc.util.ScanFieldModifyListener)
         */
        public void removeModifyListener(ScanFieldModifyListener modifyListener) {
            scanFieldModifyListenerList.remove(modifyListener);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginProcess()
         */
        public void beginProcess() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
         */
        public void beginPut(DBStructure dbStructure) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
         */
        public void dataPut(DBField requested, DBField dbField) {
            callListeners();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
         */
        public void dataPut(DBField dbField) {
            callListeners();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endProcess()
         */
        public void endProcess() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
         */
        public void endPut(DBStructure dbStructure) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
         */
        public void supportNamePut(DBField requested, DBField dbField) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField)
         */
        public void supportNamePut(DBField dbField) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
         */
        public void unlisten(RecordListener listener) {}
        
        private void callListeners() {
            Iterator<ScanFieldModifyListener> iter;
            iter = scanFieldModifyListenerList.iterator();
            while(iter.hasNext()) {
                ScanFieldModifyListener listener = iter.next();
                listener.modified();
            }
        }
        
    }
}
