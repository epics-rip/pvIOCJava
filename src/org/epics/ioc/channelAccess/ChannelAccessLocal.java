/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.PVData;
import org.epics.ioc.pvAccess.Property;

/**
 * @author mrk
 *
 */
public class ChannelAccessLocal {
    
    public static final Channel createChannel(IOCDB iocdb,DBRecord record) {
        return null;
    }
    
    private static class LocalChannel implements Channel {
        private IOCDB iocdb;
        private DBAccess dbAccess;
        private ChannelDataLocal recordData;
        private ChannelDataLocal currentData;
        private TreeSet<ChannelDataLocal> dataList = 
            new TreeSet<ChannelDataLocal>();
        
        LocalChannel(IOCDB iocdb,DBRecord record) {
            this.iocdb = iocdb;
            dbAccess = iocdb.createAccess(record.getRecordName());
            DBData dbData = dbAccess.getField();
            recordData = new ChannelDataLocal(dbData);
            dataList.add(recordData);
            currentData = recordData;
        }
        
        public void addListener(ChannelStateListener listener) {
            // TODO Auto-generated method stub
            
        }

        public void get(ChannelDataListener callback, ChannelOptions[] options) {
            // TODO Auto-generated method stub
            
        }

        public AccessRights getAccessRights() {
            // OK until access security is implemented
            if(dbAccess.getField().getField().isMutable()) {
                return AccessRights.readWrite;
            } else {
                return AccessRights.read;
            }
        }

        public ChannelDataPut getChannelDataPut() {
            // TODO Auto-generated method stub
            return null;
        }

        public ChannelData getField() {
            return currentData;
        }

        public boolean getPropertyField(Property property) {
            DBData dbData = dbAccess.getPropertyField(property);
            if(dbData==null) return false;
            return true;
        }

        public boolean getPropertyField(String name) {
            DBData dbData = dbAccess.getPropertyField(name);
            if(dbData==null) return false;
            return true;
        }

        
        public boolean isConnected() {
            return true;
        }

        public void removeListener(ChannelStateListener listener) {
            // TODO Auto-generated method stub
            
        }

        public boolean setField(String name) {
            DBData dbData = dbAccess.getField();
            if(dbData==null) return false;
            Iterator<ChannelDataLocal> iter = dataList.iterator();
            while(iter.hasNext()) {
                ChannelDataLocal local = iter.next();
                if(local.dbData==dbData) {
                    currentData = local;
                    return true;
                }
            }
            currentData = new ChannelDataLocal(dbData);
            dataList.add(currentData);
            return true;
        }

        public void subscribe(ChannelDataListener listener, Event why) {
            // TODO Auto-generated method stub
            
        }

        public void subscribe(ChannelNotifyListener listener, Event why) {
            // TODO Auto-generated method stub
            
        }

        public void destroy() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private static class ChannelDataLocal implements ChannelData, Comparable {
        
        DBData dbData;
        private long thisInstance;
        private static long numberInstances = 0;
        
        ChannelDataLocal(DBData dbData) {
            this.dbData = dbData;
            thisInstance = numberInstances++;
        }

        public PVData getPVData() {
            return dbData;
        }

        public int compareTo(Object o) {
            ChannelDataLocal other = (ChannelDataLocal)o;
            long diff = thisInstance - other.thisInstance;
            if(diff<0) return -1;
            if(diff>0) return 1;
            return 0;
        }
        
    }
}
