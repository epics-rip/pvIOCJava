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
 * @author mrk
 *
 */
public class ScanFieldSupportFactory {
    private static Convert convert = ConvertFactory.getConvert();
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    
    public static Support create(DBStructure dbStructure) {
        Support support = null;
        String supportName = dbStructure.getSupportName();
        if(supportName.equals("scan")) {
            support = new Scan(dbStructure);
        }
        return support;
    }
    
    private static class Scan extends AbstractSupport implements IOCDBMergeListener {
        private static String supportName = "scan";
        private boolean isStarted = false;
        private DBRecord dbRecord = null;
        private String recordName = null;
        private ScanType scanType;
        private ScanPriority priority;
        private String eventName = null;
        private double rate = 0.0;
        
        
        public Scan(DBStructure dbStructure) {
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
                throw new IllegalStateException(recordName + " field " + fieldName + " not found");
            }
            if(dbAccess.getField()!=dbStructure) {
                throw new IllegalStateException(recordName + " scan field is not at top level");
            }
            
            dbAccess.setField("");
            fieldName = "scan.priority";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + " field " + fieldName + " not found");
            }
            oldField = dbAccess.getField();
            if(oldField.getDBDField().getDBType()!=DBType.dbMenu) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a menu field ");
            }
            DBMenu priorityMenu = (DBMenu)oldField;
            if(!ScanFieldFactory.isPriorityMenu(priorityMenu)) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a menuPriority ");
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
                throw new IllegalStateException(recordName + " field " + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getDBDField().getDBType()!=DBType.dbMenu) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a menu field ");
            }
            DBMenu scanMenu = (DBMenu)oldField;
            if(!ScanFieldFactory.isScanMenu(scanMenu)) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a menuScan ");
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
                throw new IllegalStateException(recordName + " field " + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getField().getType()!=Type.pvDouble) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a double field ");
            }
            DBDouble oldRate = (DBDouble)oldField;
            rate = oldRate.get();
            DBDouble newRate = new RateField(this,dbStructure,oldField.getDBDField());
            newRate.put(rate);
            dbAccess.replaceField(oldField,newRate);
            
            dbAccess.setField("");
            fieldName = "scan.eventName";
            if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
                throw new IllegalStateException(recordName + " field " + fieldName + " not in record ");
            }
            oldField = dbAccess.getField();
            if(oldField.getField().getType()!=Type.pvString) {
                throw new IllegalStateException(recordName + " field " + fieldName + " is not a string field ");
            }
            DBString oldEventName = (DBString)oldField;
            eventName = oldEventName.get();
            DBString newEventName = new EventNameField(this,dbStructure,oldField.getDBDField());
            newEventName.put(eventName);
            dbAccess.replaceField(oldField,newEventName);
        }
        
        public String getName() {
            return supportName;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#initialize(org.epics.ioc.dbProcess.SupportCreation)
         */
        public void initialize() {
            // Nothing to do
        }

        public void uninitialize() {
            stop();
        }
        public void start() {
            dbRecord.getIOCDB().addIOCDBMergeListener(this);
        }
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
 
        public ProcessReturn process(ProcessCompleteListener listener) {
            System.err.println(recordName + " ScanField process is being called. Why?");
            return ProcessReturn.failure;
        }       
        public void processContinue() {
            System.err.println(recordName + " ScanField processContinue is being called. Why?");
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
            if(name.equals(eventName)) return;
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
        private Scan scan;
        
        private PriorityField(Scan scan,DBStructure parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
            this.scan = scan;
        }
        
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scan.putPriority(ScanPriority.valueOf(ScanPriority.class, super.getChoices()[index]));
        }
    }
    
    private static class ScanField extends AbstractDBMenu {
        private Scan scan;
        
        private ScanField(Scan scan,DBStructure parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
            this.scan = scan;
        }
        
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            super.setIndex(index);
            int newIndex = super.getIndex();
            if(oldIndex==newIndex) return;
            scan.putScanType(ScanType.valueOf(ScanType.class, super.getChoices()[index]));
        }
    }
    
    private static class RateField extends AbstractDBData implements DBDouble{
        private double value;
        private Scan scan;
        
        private RateField(Scan scan,DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            this.scan = scan;
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
                scan.putRate(value);
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
        private Scan scan;
        
        private EventNameField(Scan scan,DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
            this.scan = scan;
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
                if(oldValue.equals(value)) return;
                scan.putEventName(value);
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
