/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFieldSupportFactory {
    private static Convert convert = ConvertFactory.getConvert();
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        if(supportName.equals("scan")) {
            try {
                return new ScanFieldSupport(dbStructure);
            } catch (IllegalStateException e) {
                System.err.printf("ScanFieldSupportFactory create failure %s%n",e.getMessage());
                return null;
            }  
        }
        System.err.printf("ScanFieldSupportFactory create supportName %s but expected scan%n",supportName);
        return null;
    }
    
    private static class ScanFieldSupport extends AbstractSupport implements IOCDBMergeListener {
        private static String supportName = "scan";
        private boolean isStarted = false;
        private DBRecord dbRecord = null;
        private String recordName = null;
        private ScanType scanType;
        private ScanPriority priority;
        private String eventName = null;
        private double rate = 0.0;
        
        
        /**
         * The public constructor.
         * @param dbStructure The structure for accessing the scan field.
         */
        public ScanFieldSupport(DBStructure dbStructure) {
            super(supportName,dbStructure);
            String fieldName;
            DBData oldField;
            int index;
            String choice;
            DBDMenuField dbdMenuField;
            DBMenu newMenu;
            
            dbRecord = dbStructure.getRecord();
            recordName = dbRecord.getRecordName();
            if(!dbStructure.getField().getName().equals("scan")
            || dbStructure.getParent()!=dbRecord) {
                throw new IllegalStateException(recordName + " scan support: illegal scan field definition.");
            }
            IOCDB iocdb = dbRecord.getIOCDB();
            DBAccess dbAccess = iocdb.createAccess(recordName);
            
            fieldName = "scan";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not found");
            }
            if(dbAccess.getField()!=dbStructure) {
                throw new IllegalStateException(recordName + " scan field is not at top level");
            }
            
            dbAccess.setField("");
            fieldName = "priority";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not found");
            }
            oldField = dbAccess.getField();
            if(oldField.getDBDField().getDBType()!=DBType.dbMenu) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menu field ");
            }
            DBMenu priorityMenu = (DBMenu)oldField;
            if(!ScanFieldFactory.isPriorityMenu(priorityMenu)) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menuPriority ");
            }
            index = priorityMenu.getIndex();
            choice = priorityMenu.getChoices()[index];
            priority = ScanPriority.valueOf(choice);
            dbdMenuField = (DBDMenuField)oldField.getDBDField();
            newMenu = new PriorityField(this,dbStructure,dbdMenuField);
            newMenu.setIndex(index);
            dbAccess.replaceField(oldField,newMenu);

            dbAccess.setField("");
            fieldName = "scan.scan";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getDBDField().getDBType()!=DBType.dbMenu) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menu field ");
            }
            DBMenu scanMenu = (DBMenu)oldField;
            if(!ScanFieldFactory.isScanMenu(scanMenu)) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a menuScan ");
            }
            index = scanMenu.getIndex();
            choice = scanMenu.getChoices()[index];
            scanType = ScanType.valueOf(choice);
            dbdMenuField = (DBDMenuField)oldField.getDBDField();
            newMenu = new ScanField(this,dbStructure,dbdMenuField);
            newMenu.setIndex(index);
            dbAccess.replaceField(oldField,newMenu);
            
            dbAccess.setField("");
            fieldName = "scan.rate";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getField().getType()!=Type.pvDouble) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a double field ");
            }
            DBDouble oldRate = (DBDouble)oldField;
            rate = oldRate.get();
            DBDouble newRate = new RateField(this,dbStructure,oldField.getDBDField());
            newRate.put(rate);
            dbAccess.replaceField(oldField,newRate);
            
            dbAccess.setField("");
            fieldName = "scan.eventName";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + "." + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getField().getType()!=Type.pvString) {
                throw new IllegalStateException(recordName + "." + fieldName + " is not a string field ");
            }
            DBString oldEventName = (DBString)oldField;
            eventName = oldEventName.get();
            DBString newEventName = new EventNameField(this,dbStructure,oldField.getDBDField());
            newEventName.put(eventName);
            dbAccess.replaceField(oldField,newEventName);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#getName()
         */
        public String getName() {
            return supportName;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#initialize(org.epics.ioc.dbProcess.SupportCreation)
         */
        public void initialize() {
            // Nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            stop();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#start()
         */
        public void start() {
            dbRecord.getIOCDB().addIOCDBMergeListener(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#stop()
         */
        public void stop() {
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.removeRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.unschedule(dbRecord);
                break;
            }
            isStarted = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            System.err.println(recordName + " ScanField process is being called. Why?");
            return ProcessReturn.failure;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#processContinue()
         */
        public void processContinue() {
            System.err.println(recordName + " ScanField processContinue is being called. Why?");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDBMergeListener#merged()
         */
        public void merged() {
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.addRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.schedule(dbRecord);
                break;
            }
            isStarted = true;
        }
        
        private void putPriority(ScanPriority value) {
            if(value==priority) return;
            boolean isStarted = this.isStarted;
            if(isStarted) stop();
            priority = value;
            if(isStarted) start();
        }
        private void putScanType(ScanType type) {
            if(type==scanType) return;
            boolean isStarted = this.isStarted;
            if(isStarted) stop();
            scanType = type;
            if(isStarted) start();
        }
        private void putEventName(String name) {
            if(name==null && eventName==null) return;
            if(name!=null && name.equals(eventName)) return;
            boolean isStarted = this.isStarted;
            if(isStarted && scanType==ScanType.event) stop();
            eventName = name;
            if(isStarted && scanType==ScanType.event) start();
        }
        private void putRate(double rate) {
            if(rate==this.rate) return;
            boolean isStarted = this.isStarted;
            if(isStarted && scanType==ScanType.periodic) stop();
            this.rate = rate;
            if(isStarted && scanType==ScanType.periodic) start();
        }
        
    }
    
    private static class PriorityField extends AbstractDBMenu {
        private ScanFieldSupport scanFieldSupport;
        
        private PriorityField(ScanFieldSupport scanFieldSupport,DBStructure parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
            this.scanFieldSupport = scanFieldSupport;
        }
        
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scanFieldSupport.putPriority(ScanPriority.valueOf(ScanPriority.class, super.getChoices()[index]));
        }
    }
    
    private static class ScanField extends AbstractDBMenu {
        private ScanFieldSupport scanFieldSupport;
        
        private ScanField(ScanFieldSupport scanFieldSupport,DBStructure parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
            this.scanFieldSupport = scanFieldSupport;
        }
        
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scanFieldSupport.putScanType(ScanType.valueOf(ScanType.class, super.getChoices()[index]));
        }
    }
    
    private static class RateField extends AbstractDBData implements DBDouble{
        private double value;
        private ScanFieldSupport scanFieldSupport;
        
        private RateField(ScanFieldSupport scanFieldSupport,DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            this.scanFieldSupport = scanFieldSupport;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        public double get() {
            return value;
        }
        public void put(double value) {
            if(getField().isMutable()) {
                double oldValue = this.value;
                this.value = value;
                postPut();
                if(oldValue==value) return;
                scanFieldSupport.putRate(value);
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }       
        public String toString() {
            return toString(0);
        }       
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
    
    private static class EventNameField extends AbstractDBData implements DBString{
        private String value;
        private ScanFieldSupport scanFieldSupport;
        
        private EventNameField(ScanFieldSupport scanFieldSupport,DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
            this.scanFieldSupport = scanFieldSupport;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        public String get() {
            return value;
        }
        public void put(String value) {
            if(getField().isMutable()) {
                String oldValue = this.value;
                this.value = value;
                postPut();
                if(oldValue==null && value==null) return;
                if(oldValue!=null && oldValue.equals(value)) return;
                scanFieldSupport.putEventName(value);
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }       
        public String toString() {
            return toString(0);
        }       
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
}
