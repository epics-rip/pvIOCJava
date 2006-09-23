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
    public static ScanField create(DBRecord dbRecord) {
        DBData[] datas = dbRecord.getFieldDBDatas();
        int index = dbRecord.getFieldDBDataIndex("scan");
        if(index<0) return null;
        DBData data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbStructure) return null;
        DBStructure scan = (DBStructure)data;
        DBDStructure dbdStructure = scan.getDBDStructure();
        if(!dbdStructure.getStructureName().equals("scan")) return null;
        datas = scan.getFieldDBDatas();
        index = scan.getFieldDBDataIndex("priority");
        if(index<0) return null;
        data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbMenu) return null;
        DBMenu priorityField = (DBMenu)data;
        if(!isPriorityMenu(priorityField)) return null;        
        index = scan.getFieldDBDataIndex("scan");
        if(index<0) return null;
        data = datas[index];
        if(data.getDBDField().getDBType()!=DBType.dbMenu) return null;
        DBMenu scanField = (DBMenu)data;
        if(!isScanMenu(scanField)) return null;
        
        index = scan.getFieldDBDataIndex("rate");
        if(index<0) return null;
        data = datas[index];
        if(data.getField().getType()!=Type.pvDouble) return null;
        DBDouble rateField = (DBDouble)data;
        index = scan.getFieldDBDataIndex("eventName");
        if(index<0) return null;
        data = datas[index];
        if(data.getField().getType()!=Type.pvString) return null;
        DBString eventNameField = (DBString)data;
        return new ScanFieldInstance(priorityField,scanField,rateField,eventNameField);
    }
    
    public static boolean isScanMenu(DBMenu dbMenu) {
        if(!dbMenu.getMenuName().equals("scan")) return false;
        String[] choices = dbMenu.getChoices();
        if(choices.length!=3) return false;
        if(!choices[0].equals("passive")) return false;
        if(!choices[1].equals("event")) return false;
        if(!choices[2].equals("periodic")) return false;
        return true;
    }
    public static boolean isPriorityMenu(DBMenu dbMenu) {
        if(!dbMenu.getMenuName().equals("priority")) return false;
        String[] choices = dbMenu.getChoices();
        if(choices.length!=5) return false;
        if(!choices[0].equals("lowest")) return false;
        if(!choices[1].equals("low")) return false;
        if(!choices[2].equals("medium")) return false;
        if(!choices[3].equals("high")) return false;
        if(!choices[4].equals("highest")) return false;
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
