/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanSupportFactory {
    private static Convert convert = ConvertFactory.getConvert();
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    private static final String supportName = "scan";
    /**
     * Create support for the scan field.
     * @param dbStructure The interface to the scan field.
     * @return The support or null if the scan field is improperly defined.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        ScanField scanField = ScanFieldFactory.create(pvStructure.getPVRecord());
        if(scanField==null) return null;
        String supportName = pvStructure.getSupportName();
        if(!supportName.equals(supportName)) {
            pvStructure.message(
                    "ScanSupportFactory create supportName is " + supportName
                    + " but expected scan",
                    MessageType.fatalError);
                return null;
        }
        try {
            return new ScanFieldSupport(dbStructure);
        } catch (IllegalStateException e) {
            pvStructure.message(
                "ScanSupportFactory create failure " + e.getMessage(),
                MessageType.fatalError);
            return null;
        }  
    }
    
    private static class ScanFieldSupport extends AbstractSupport implements IOCDBMergeListener {
        
        private boolean isMerged = false;
        private DBRecord dbRecord = null;
        private PVRecord pvRecord;
        private String recordName = null;
        private ScanType scanType;
        private ScanPriority priority;
        private String eventName = null;
        private double rate = 0.0;
        
        private ScanFieldSupport(DBStructure dbStructure) {
            super(supportName,dbStructure);
            String fieldName;
            PVField oldField;
            int index;
            String choice;
            Menu menu;
            PVMenu newMenu;
            dbRecord = dbStructure.getDBRecord();
            pvRecord = dbRecord.getPVRecord();
            recordName = pvRecord.getRecordName();
            PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);           
            pvAccess.findField("");
            fieldName = "scan.scan";
            if(pvAccess.findField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = pvAccess.getField();
            if(oldField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menu field ");
            }
            PVMenu scanMenu = (PVMenu)oldField;
            if(!ScanFieldFactory.isScanMenu(scanMenu)) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menuScan ");
            }
            index = scanMenu.getIndex();
            String[] choices = scanMenu.getChoices();
            choice = choices[index];
            scanType = ScanType.valueOf(choice);
            menu = (Menu)oldField.getField();
            newMenu = new DBScan(this,(DBField)dbStructure,menu);
            newMenu.setIndex(index);
            DBField dbField = dbRecord.findDBField(oldField);
            dbField.replacePVField(newMenu);
            
            pvAccess.findField("");
            fieldName = "scan.rate";
            if(pvAccess.findField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = pvAccess.getField();
            if(oldField.getField().getType()!=Type.pvDouble) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a double field ");
            }
            PVDouble oldRate = (PVDouble)oldField;
            rate = oldRate.get();
            PVDouble newRate = new DBRate(this,(DBField)dbStructure,oldField.getField());
            newRate.put(rate);
            dbField = dbRecord.findDBField(oldField);
            dbField.replacePVField(newRate);
            
            pvAccess.findField("");
            fieldName = "scan.eventName";
            if(pvAccess.findField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = pvAccess.getField();
            if(oldField.getField().getType()!=Type.pvString) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a string field ");
            }
            PVString oldEventName = (PVString)oldField;
            eventName = oldEventName.get();
            PVString newEventName = new DBEventName(this,(DBField)dbStructure,oldField.getField());
            newEventName.put(eventName);
            dbField = dbRecord.findDBField(oldField);
            dbField.replacePVField(newEventName);
            
            pvAccess.findField("");
            fieldName = "scan.priority";
            if(pvAccess.findField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not found");
            }
            oldField = pvAccess.getField();
            if(oldField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menu field ");
            }
            PVMenu priorityMenu = (PVMenu)oldField;
            if(!ScanFieldFactory.isPriorityMenu(priorityMenu)) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menuPriority ");
            }
            index = priorityMenu.getIndex();
            choice = priorityMenu.getChoices()[index];
            priority = ScanPriority.valueOf(choice);
            menu = (Menu)oldField.getField();
            newMenu = new DBPriority(this,(DBField)dbStructure,menu);
            newMenu.setIndex(index);
            dbField = dbRecord.findDBField(oldField);
            dbField.replacePVField(newMenu);
            dbRecord.getIOCDB().addIOCDBMergeListener(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#getName()
         */
        public String getRequestorName() {
            return supportName;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize(org.epics.ioc.process.SupportCreation)
         */
        public void initialize() {
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            stop();
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            if(isMerged) startScanner();
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(isMerged) {
                switch (scanType) {
                case passive: break;
                case event:
                    eventScanner.removeRecord(dbRecord);
                    break;
                case periodic:
                    periodicScanner.unschedule(dbRecord);
                    break;
                }
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            dbRecord.getPVRecord().message("process is being called. Why?", MessageType.error);
            supportProcessRequestor.supportProcessDone(RequestResult.failure);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#processContinue()
         */
        public void processContinue() {
            dbRecord.getPVRecord().message("processContinue is being called. Why?", MessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDBMergeListener#merged()
         */
        public void merged() {
            dbRecord.lock();
            try {
                if(super.getSupportState()==SupportState.ready) startScanner();
                isMerged = true;
            } finally {
                dbRecord.unlock();
            }
        }
        
        private void startScanner() {
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.addRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.schedule(dbRecord);
                break;
            }
        }
        private void putPriority(ScanPriority value) {
            if(value==priority) return;
            boolean isStarted = this.isMerged;
            if(isStarted) stop();
            priority = value;
            if(isStarted) start();
        }
        private void putScanType(ScanType type) {
            if(type==scanType) return;
            boolean isStarted = this.isMerged;
            if(isStarted) stop();
            scanType = type;
            if(isStarted) start();
        }
        private void putEventName(String name) {
            if(name==null && eventName==null) return;
            if(name!=null && name.equals(eventName)) return;
            boolean isStarted = this.isMerged;
            if(isStarted && scanType==ScanType.event) stop();
            eventName = name;
            if(isStarted && scanType==ScanType.event) start();
        }
        private void putRate(double rate) {
            if(rate==this.rate) return;
            boolean isStarted = this.isMerged;
            if(isStarted && scanType==ScanType.periodic) stop();
            this.rate = rate;
            if(isStarted && scanType==ScanType.periodic) start();
        }
        
    }
    
    private static class DBPriority extends BasePVMenu {
        private ScanFieldSupport scanFieldSupport;
        
        private DBPriority(ScanFieldSupport scanFieldSupport,DBField parent,Menu menu) {
            super(parent.getPVField(),menu);
            this.scanFieldSupport = scanFieldSupport;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.db.BaseDBEnum#setIndex(int)
         */
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scanFieldSupport.putPriority(ScanPriority.valueOf(ScanPriority.class, super.getChoices()[index]));
        }
    }
    
    private static class DBScan extends BasePVMenu {
        private ScanFieldSupport scanFieldSupport;
        
        private DBScan(ScanFieldSupport scanFieldSupport,DBField parent,Menu menu) {
            super(parent.getPVField(),menu);
            this.scanFieldSupport = scanFieldSupport;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.BaseDBEnum#setIndex(int)
         */
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scanFieldSupport.putScanType(ScanType.valueOf(ScanType.class, super.getChoices()[index]));
        }
    }
    
    private static class DBRate extends AbstractPVField implements PVDouble{
        private double value;
        private ScanFieldSupport scanFieldSupport;
        
        private DBRate(ScanFieldSupport scanFieldSupport,DBField parent,Field field) {
            super(parent.getPVField(),field);
            value = 0;
            this.scanFieldSupport = scanFieldSupport;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#get()
         */
        public double get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#put(double)
         */
        public void put(double value) {
            if(getField().isMutable()) {
                double oldValue = this.value;
                this.value = value;
                if(oldValue==value) return;
                scanFieldSupport.putRate(value);
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
    
    private static class DBEventName extends AbstractPVField implements PVString{
        private String value;
        private ScanFieldSupport scanFieldSupport;
        
        private DBEventName(ScanFieldSupport scanFieldSupport,DBField parent,Field field) {
            super(parent.getPVField(),field);
            value = null;
            this.scanFieldSupport = scanFieldSupport;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(getField().isMutable()) {
                String oldValue = this.value;
                this.value = value;
                if(oldValue==null && value==null) return;
                if(oldValue!=null && oldValue.equals(value)) return;
                scanFieldSupport.putEventName(value);
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
}
