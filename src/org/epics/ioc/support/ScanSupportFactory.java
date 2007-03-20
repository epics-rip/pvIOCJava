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
    
    private static class ScanFieldSupport extends AbstractSupport
    implements ScanSupport, IOCDBMergeListener
    {
        
        private boolean isMerged = false;
        private DBRecord dbRecord = null;
        private ScanType scanType;
        private ScanPriority priority;
        private String eventName = null;
        private double rate = 0.0;
        private PVScanSelf pvScanSelf;
        
        private ScanFieldSupport(DBStructure dbScan) {
            super(supportName,dbScan);
            dbRecord = dbScan.getDBRecord();
            PVRecord pvRecord = dbRecord.getPVRecord();
            String recordName = pvRecord.getRecordName();
            PVStructure pvScan = dbScan.getPVStructure();
            Structure scanStructure = (Structure)pvScan.getField();
            DBField[] scanDBFields = dbScan.getFieldDBFields();
            PVField[] scanPVFields = pvScan.getFieldPVFields();
            
            String fullFieldName = null;
            PVField pvField = null;
            DBField dbField = null;
            int index; 
            int choiceIndex;
            String choice;
            String[] choices;
            Menu menu;
            PVMenu pvMenu;
            PVMenu newMenu;
            
            fullFieldName = pvScan.getFullFieldName() + ".scan";
            index = scanStructure.getFieldIndex("scan");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type menu");
            }
            pvMenu = (PVMenu)pvField;
            if(!ScanFieldFactory.isScanMenu(pvMenu)) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not a menuScan ");
            }
            dbField = scanDBFields[index];
            choices = pvMenu.getChoices();
            choiceIndex = pvMenu.getIndex();
            choice = choices[choiceIndex];
            scanType = ScanType.valueOf(choice);
            menu = (Menu)pvField.getField();
            newMenu = new DBScan(this,dbField,menu);
            newMenu.setIndex(choiceIndex);
            dbField.replacePVField(newMenu);
            
            fullFieldName = pvScan.getFullFieldName() + ".rate";
            index = scanStructure.getFieldIndex("rate");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvDouble) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type double");
            }
            dbField = scanDBFields[index];
            PVDouble oldRate = (PVDouble)pvField;
            rate = oldRate.get();
            PVDouble newRate = new DBRate(this,dbField,pvField.getField());
            newRate.put(rate);
            dbField.replacePVField(newRate);
            
            fullFieldName = pvScan.getFullFieldName() + ".eventName";
            index = scanStructure.getFieldIndex("eventName");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvString) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type string");
            }
            dbField = scanDBFields[index];
            PVString oldEventName = (PVString)pvField;
            eventName = oldEventName.get();
            PVString newEventName = new DBEventName(this,dbField,pvField.getField());
            newEventName.put(eventName);
            dbField.replacePVField(newEventName);
            
            fullFieldName = pvScan.getFullFieldName() + ".priority";
            index = scanStructure.getFieldIndex("priority");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type menu");
            }
            pvMenu = (PVMenu)pvField;
            if(!ScanFieldFactory.isPriorityMenu(pvMenu)) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not a menuPriority ");
            }
            dbField = scanDBFields[index];
            choices = pvMenu.getChoices();
            choiceIndex = pvMenu.getIndex();
            choice = choices[choiceIndex];
            priority = ScanPriority.valueOf(choice);
            menu = (Menu)pvField.getField();
            newMenu = new DBPriority(this,dbField,menu);
            newMenu.setIndex(choiceIndex);
            dbField.replacePVField(newMenu);
            
            fullFieldName = pvScan.getFullFieldName() + ".scanSelf";
            index = scanStructure.getFieldIndex("scanSelf");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvBoolean) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type boolean");
            }
            dbField = scanDBFields[index];
            PVBoolean oldScanSelf = (PVBoolean)pvField;
            boolean scanSelf = oldScanSelf.get();
            pvScanSelf = new PVScanSelf(this,dbField,pvField.getField());
            pvScanSelf.put(scanSelf);
            dbField.replacePVField(pvScanSelf);
            
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
            pvScanSelf.initialize();
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
         * @see org.epics.ioc.process.ScanSupport#isScanSelf()
         */
        public boolean canScanSelf() {
            return pvScanSelf.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ScanSupport#scanSelf()
         */
        public boolean scanSelf() {
            return pvScanSelf.scanSelf();
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
        
        private DBPriority(ScanFieldSupport scanFieldSupport,DBField dbField,Menu menu) {
            super(dbField.getParent().getPVField(),menu);
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
        
        private DBScan(ScanFieldSupport scanFieldSupport,DBField dbField,Menu menu) {
            super(dbField.getParent().getPVField(),menu);
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
        
        private DBRate(ScanFieldSupport scanFieldSupport,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
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
                //dbField.postPut();
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
        
        private DBEventName(ScanFieldSupport scanFieldSupport,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
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
                //dbField.postPut();
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
    
    private static class PVScanSelf extends AbstractPVField
    implements PVBoolean,RecordProcessRequestor
    {
        private DBField dbField;
        private boolean isScanSelf;
        private RecordProcess recordProcess = null;
        private boolean isActive = false;
        
        private PVScanSelf(ScanFieldSupport scanFieldSupport,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
            this.dbField = dbField;
            isScanSelf = false;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                isScanSelf = Boolean.valueOf(defaultValue);
            }
            dbField.replacePVField(this);
        }
        private void initialize() {
            if(recordProcess!=null) return;
            recordProcess = dbField.getDBRecord().getRecordProcess();
            if(isScanSelf) {
                if(!recordProcess.setRecordProcessRequestor(this)) {
                    dbField.getPVField().message(
                       "scanSelf not possible because setRecordProcessor failed",
                       MessageType.warning);
                    isScanSelf = false;
                }
            }
        }
        private boolean scanSelf() {
            if(!isScanSelf) return false;
            if(isActive) return false;
            isActive = true;
            recordProcess.process(this, false, null);
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return isScanSelf;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            if(getField().isMutable()) {
                if(isScanSelf) {
                    recordProcess.releaseRecordProcessRequestor(this);
                    isScanSelf = false;
                }
                if(value) {
                    isScanSelf = recordProcess.setRecordProcessRequestor(this);
                }
                //dbField.postPut();
                return;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            isActive = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
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
