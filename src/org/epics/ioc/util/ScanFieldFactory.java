/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.Type;


/**
 * 
 * A factory to create a ScanField interface.
 * @author mrk
 *
 */
public class ScanFieldFactory {
    /**
     * Create a ScanField.
     * The record instance must have a top level field named "scan"
     * that must be a "scan" structure as defined in the
     * menuStructureSupportDBD.xml file that appears in package
     * org.epics.ioc.support.
     * ScanFieldFactory does no locking so code that uses it must be thread safe.
     * In general this means that the record instance must be locked when any method is called. 
     * @param dbRecord The record instance.
     * @return The ScanField interface or null of the record instance does not have
     * a valid scan field.
     */
    public static ScanField create(DBRecord dbRecord) {
        DBData[] datas = dbRecord.getFieldDBDatas();
        int index;
        DBData data;  
        index = dbRecord.getFieldDBDataIndex("scan");
        if(index<0) {
            dbRecord.message("field scan does not exist", IOCMessageType.fatalError);
            return null;
        }
        data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbStructure){
            dbRecord.message("field scan is not a structure", IOCMessageType.fatalError);
            return null;
        }
        DBStructure scan = (DBStructure)data;
        DBDStructure dbdStructure = scan.getDBDStructure();
        if(!dbdStructure.getStructureName().equals("scan")) {
            scan.message("is not a scan structure", IOCMessageType.fatalError);
            return null;
        }
        datas = scan.getFieldDBDatas(); 
        index = scan.getFieldDBDataIndex("priority");
        if(index<0) {
            scan.message("does not have field priority", IOCMessageType.fatalError);
            return null;
        }
        data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbMenu) {
            scan.message("is not a menu", IOCMessageType.fatalError);
            return null;
        }
        DBMenu priorityField = (DBMenu)data;
        if(!isPriorityMenu(priorityField)) {
            scan.message("is not a priority menu", IOCMessageType.fatalError);
            return null;     
        }
        index = scan.getFieldDBDataIndex("scan");
        if(index<0) {
            scan.message("does not have a field scan", IOCMessageType.fatalError);
            return null;
        }
        data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbMenu) {
            data.message("is not a menu", IOCMessageType.fatalError);
            return null;
        }
        DBMenu scanField = (DBMenu)data;
        if(!isScanMenu(scanField)) {
            scanField.message("is not a scan menu", IOCMessageType.fatalError);
            return null;        
        }
        index = scan.getFieldDBDataIndex("rate");
        if(index<0) {
            scan.message("does not have a field rate", IOCMessageType.fatalError);
            return null;
        }
        data = datas[index];
        if(data.getField().getType()!=Type.pvDouble) {
            data.message("is not a double", IOCMessageType.fatalError);
            return null;
        }
        DBDouble rateField = (DBDouble)data;
        index = scan.getFieldDBDataIndex("eventName");
        if(index<0) {
            scan.message("does not have a field eventName", IOCMessageType.fatalError);
            return null;
        }
        data = datas[index];
        if(data.getField().getType()!=Type.pvString) {
            data.message("is not a string", IOCMessageType.fatalError);
            return null;
        }
        DBString eventNameField = (DBString)data;
        return new ScanFieldInstance(priorityField,scanField,rateField,eventNameField);
    }
    
    /**
     * Does the menu define the scan types?
     * @param dbMenu The menu.
     * @return (false,true) is the menu defined the scan types.
     */
    public static boolean isScanMenu(DBMenu dbMenu) {
        if(!dbMenu.getMenuName().equals("scan")) return false;
        String[] choices = dbMenu.getChoices();
        if(choices.length!=3) return false;
        for(int i=0; i<choices.length; i++) {
            try {
                if(ScanType.valueOf(choices[i]).ordinal()!=i) return false;
            } catch(IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
    /**
     * Does the menu define the thread priorities.
     * @param dbMenu The menu.
     * @return (false,true) is the menu defined the thread priorities.
     */
    public static boolean isPriorityMenu(DBMenu dbMenu) {
        if(!dbMenu.getMenuName().equals("priority")) return false;
        String[] choices = dbMenu.getChoices();
        if(choices.length!=7) return false;
        for(int i=0; i<choices.length; i++) {
            try {
                if(ScanPriority.valueOf(choices[i]).ordinal()!=i) return false;
            } catch(IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
    private static class ScanFieldInstance implements ScanField {
        private DBMenu priority;
        private DBMenu scan;
        private DBDouble rate;
        private DBString eventName;
        
        private ScanFieldInstance(DBMenu priority, DBMenu scan, DBDouble rate, DBString eventName) {
            super();
            this.priority = priority;
            this.scan = scan;
            this.rate = rate;
            this.eventName = eventName;
        }
        public String getEventName() {
            return eventName.get();
        }
        public ScanPriority getPriority() {
            return ScanPriority.valueOf(priority.getChoices()[priority.getIndex()]);
        }
        public double getRate() {
            return rate.get();
        }
        public ScanType getScanType() {
            return ScanType.valueOf(scan.getChoices()[scan.getIndex()]);
        }
    }
}
