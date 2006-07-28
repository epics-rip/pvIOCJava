/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.Field;
import org.epics.ioc.pvAccess.PVData;
import org.epics.ioc.channelAccess.*;
import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class ChannelAccessLocal implements ChannelAccess {
    
    private IOCDB iocdb;
    
    public ChannelAccessLocal(IOCDB iocdb) {
        this.iocdb = iocdb;
        if(!ChannelFactory.registerLocalChannelAccess(this)) {
            throw new IllegalStateException(
                "ChannelAccessLocal is already registered");
        }
    }
    
    public ChannelIOC createChannel(String name,ChannelStateListener listener) {
        DBRecord dbRecord = iocdb.findRecord(name);
        if(dbRecord==null) return null;
        return new LocalChannel(iocdb,dbRecord,listener);
    }
    
    private static class LocalChannel implements ChannelIOC {
        private DBAccess dbAccess;
        private DBData currentData = null;
        private ChannelStateListener stateListener = null;
        private String otherRecord = null;
        private String otherField = null;
        private double timeOut = 0.0;
        
        LocalChannel(IOCDB iocdb,DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbAccess = iocdb.createAccess(record.getRecordName());
        }
        
        public void destroy() {
            stateListener.disconnect(this);
        }
        
        public boolean isConnected() {
            return true;
        }
        
        public ChannelSetResult setField(String name) {
            AccessSetResult result = dbAccess.setField(name);
            if(result==AccessSetResult.notFound) return ChannelSetResult.notFound;
            if(result==AccessSetResult.otherRecord) {
                otherRecord = dbAccess.getOtherRecord();
                otherField = dbAccess.getOtherField();
                return ChannelSetResult.otherChannel;
            }
            DBData dbData = dbAccess.getField();
            currentData = dbData;
            return ChannelSetResult.thisChannel;
        }

        public String getOtherChannel() {
            return otherRecord;
        }

        public String getOtherField() {
            return otherField;
        }
        
        public ChannelField getChannelField() {
            return new ChannelFieldInstance(currentData);
        }
        
        public ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener) {
            return new FieldGroup(listener);
        }

        public void setTimeout(double timeout) {
            this.timeOut = timeOut;
        } 
        
        public ChannelDataProcess createChannelDataProcess() {
            return new DataProcess(dbAccess.getDbRecord());
        }
        public ChannelDataGet createChannelDataGet() {
            return new DataGet(dbAccess.getDbRecord());
        }

        public ChannelDataPut createChannelDataPut() {
            return new DataPut(dbAccess.getDbRecord());
        }
        
        public ChannelDataPutGet createChannelDataPutGet() {
            return new DataPutGet(dbAccess.getDbRecord());
        }

        public ChannelSubscribe createSubscribe() {
            return new Subscribe(dbAccess.getDbRecord());
        }

        public boolean isLocal() {
            return true;
        }
        
        public DBRecord getLocalRecord() {
            return dbAccess.getDbRecord();
        }
    }
    
    private static class ChannelFieldInstance implements ChannelField {

        private DBData dbData;
        
        ChannelFieldInstance(DBData dbData) {
            this.dbData = dbData;
        }
        
        public AccessRights getAccessRights() {
            // OK until access security is implemented
            if(dbData.getField().isMutable()) {
                return AccessRights.readWrite;
            } else {
                return AccessRights.read;
            }
        }

        public Field getField() {
            return dbData.getField();
        }
        
        DBData getDBData() {
            return dbData;
        }
        
    }
    
    private static class FieldGroup implements ChannelFieldGroup {
        private ChannelFieldGroupListener accessRightsListener;
        private LinkedList<ChannelFieldInstance> fieldList = 
            new LinkedList<ChannelFieldInstance>();

        FieldGroup(ChannelFieldGroupListener listener) {
            accessRightsListener = listener;
        }
        public void destroy() {
            accessRightsListener = null;
            fieldList.clear();
        }

        List<ChannelFieldInstance> getList() {
            return fieldList;
        }

        public void addChannelField(ChannelField channelField) {
            fieldList.add((ChannelFieldInstance)channelField);
        }

        public void removeChannelField(ChannelField channelField) {
            fieldList.remove(channelField);
        }
    }
    private static class DataProcess implements ChannelDataProcess, ProcessListener {
        private RecordProcess recordProcess;
        private FieldGroup fieldGroup = null;
        private ChannelDataProcessListener listener = null;
        
        
        DataProcess(DBRecord dbRecord) {
            recordProcess = dbRecord.getRecordProcess();
        }
        public void destroy() {
            // nothing to do
        }
        public void process(ChannelFieldGroup fieldGroup, ChannelDataProcessListener callback, boolean wait) {
            
            this.fieldGroup = (FieldGroup)fieldGroup;
            this.listener = callback;
            
            RequestProcessReturn requestReturn;
            requestReturn = recordProcess.requestProcess((wait ? this : null));
            switch(requestReturn) {
            case success: processComplete(ProcessReturn.done); break;
            case listenerAdded: break;
            case failure: processComplete(ProcessReturn.abort); break;
            case alreadyActive:
                if(!wait) {
                    processComplete(ProcessReturn.done); break;
                }
                throw new IllegalStateException("ChannelAccessLocal logic error");
            }
        }
        
        public void cancelProcess() {
            recordProcess.removeCompletionListener(this);
        }

        public void processComplete(ProcessReturn result) {
            listener.processDone(result,
                recordProcess.getAlarmSeverity(),
                recordProcess.getStatus());
        }
        
    }
    private static class DataGet implements ChannelDataGet, ProcessListener {
        private RecordProcess recordProcess;
        private FieldGroup fieldGroup = null;
        private ChannelDataGetListener listener = null;
        
        DataGet(DBRecord dbRecord) {
            recordProcess = dbRecord.getRecordProcess();
        }
        public void destroy() {
            // nothing to do
        }

        public void get(ChannelFieldGroup fieldGroup,
        ChannelDataGetListener callback, boolean process, boolean wait) {
            this.fieldGroup = (FieldGroup)fieldGroup;
            this.listener = callback;
            
            RequestProcessReturn requestReturn = RequestProcessReturn.success;
            if(process) {
                requestReturn = recordProcess.requestProcess((wait ? this : null));
            }
            switch(requestReturn) {
            case success: processComplete(ProcessReturn.done); break;
            case listenerAdded: break;
            case failure: processComplete(ProcessReturn.abort); break;
            case alreadyActive:
                if(!wait) {
                    processComplete(ProcessReturn.done); break;
                }
                throw new IllegalStateException("ChannelAccessLocal logic error");
            }
        }
        
        public void cancelGet() {
            recordProcess.removeCompletionListener(this);
        }

        public void processComplete(ProcessReturn result) {
            if(result!=ProcessReturn.done && result!=ProcessReturn.noop) {
                listener.failure("process returned " +  result.toString());
            }
            listener.beginSynchronous();
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                listener.newData(field,field.dbData);
            }
            listener.endSynchronous();
        }
        
    }
    
    private static class DataPut implements ChannelDataPut, ProcessListener {
        RecordProcess recordProcess = null;
        private ChannelDataPutListener listener = null;

        DataPut(DBRecord dbRecord) {
            recordProcess = dbRecord.getRecordProcess();
        }
        public void destroy() {
            // nothing to do
        }
        public void beginSynchronous() {
            // nothing to do
        }

        public PVData getPutPVData(ChannelField field) {
            ChannelFieldInstance instance = (ChannelFieldInstance)field;
            return instance.getDBData();
        }
        
        public void endSynchronous(boolean process,boolean wait,ChannelDataPutListener listener) {
            this.listener = listener;
            RequestProcessReturn requestReturn = RequestProcessReturn.success;
            if(process) {
                requestReturn = recordProcess.requestProcess((wait ? this : null));
            }
            switch(requestReturn) {
            case success: processComplete(ProcessReturn.done); break;
            case listenerAdded: break;
            case failure: processComplete(ProcessReturn.abort); break;
            case alreadyActive:
                if(!wait) {
                    processComplete(ProcessReturn.done); break;
                }
                throw new IllegalStateException("ChannelAccessLocal logic error");
            }
        }

        public void processComplete(ProcessReturn result) {
            if(listener!=null) listener.processComplete();
        }
        
        public void cancelPut() {
            recordProcess.removeCompletionListener(this);
        }
        
    }
    
    private static class DataPutGet implements ChannelDataPutGet, ProcessListener {
        private RecordProcess recordProcess;
        private FieldGroup fieldGroup = null;
        private ChannelDataGetListener listener = null;
       
        DataPutGet(DBRecord dbRecord) {
            recordProcess = dbRecord.getRecordProcess();
        }
        
        public void destroy() {
            // nothing to do   
        }
        public void beginSynchronous(ChannelFieldGroup inputFieldGroup, ChannelDataGetListener callback) {
            fieldGroup = (FieldGroup)inputFieldGroup;
            listener = callback;
        }

        public void endSynchronous(boolean process, boolean wait) {
            RequestProcessReturn requestReturn = RequestProcessReturn.success;
            if(process) {
                requestReturn = recordProcess.requestProcess((wait ? this : null));
            }
            switch(requestReturn) {
            case success: processComplete(ProcessReturn.done); break;
            case listenerAdded: break;
            case failure: processComplete(ProcessReturn.abort); break;
            case alreadyActive:
                if(!wait) {
                    processComplete(ProcessReturn.done); break;
                }
                throw new IllegalStateException("ChannelAccessLocal logic error");
            }
        }

        public PVData getPutPVData(ChannelField field) {
            ChannelFieldInstance instance = (ChannelFieldInstance)field;
            return instance.getDBData();
        }

        public void processComplete(ProcessReturn result) {
            if(result!=ProcessReturn.done && result!=ProcessReturn.noop) {
                listener.failure("process returned " +  result.toString());
            }
            listener.beginSynchronous();
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                listener.newData(field,field.dbData);
            }
            listener.endSynchronous();
        }
        public void cancelGetPut() {
            recordProcess.removeCompletionListener(this);
        }
        
    }
    
    private static class Subscribe implements ChannelSubscribe, DBListener {
        private RecordListener listener = null;
        private FieldGroup fieldGroup = null;
        private ChannelNotifyListener notifyListener = null;
        private ChannelDataListener dataListener = null;
        private Event event = null;
        
        Subscribe(DBRecord dbRecord)
        {
            listener = dbRecord.createListener(this);
        }
        
        public void destroy() {
            if(fieldGroup!=null) stop();
            listener = null;
        }
        public void start(ChannelFieldGroup fieldGroup, ChannelDataListener listener, Event why) {
            dataListener = listener;
            startCommon(fieldGroup,why);
        }
        public void start(ChannelFieldGroup fieldGroup, ChannelNotifyListener listener, Event why) {
            notifyListener = listener;
            startCommon(fieldGroup,why);
        }
        private void startCommon(ChannelFieldGroup channelFieldGroup, Event why) {
            if(fieldGroup!=null) throw new IllegalStateException("Channel already started");
            fieldGroup = (FieldGroup)channelFieldGroup;
            event = why;
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                DBData dbData = field.getDBData();
                dbData.addListener(this.listener);
            }
        }
        public void stop() {
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                DBData dbData = field.getDBData();
                dbData.removeListener(this.listener);
            }
            event = null;
            fieldGroup = null;
            dataListener = null;
            notifyListener = null;
        }
        
        
        public void beginSynchronous() {
            if(dataListener!=null) {
                dataListener.beginSynchronous();
            }
            if(notifyListener!=null) {
                notifyListener.beginSynchronous();
            }
        }
        public void endSynchronous() {
            if(dataListener!=null) {
                dataListener.endSynchronous();
            }
            if(notifyListener!=null) {
                notifyListener.endSynchronous();
            }
        }
        public void newData(DBData dbData) {
            // must be expanded to support Event
            ChannelFieldInstance field = null;
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance fieldNow = iter.next();
                if(dbData==fieldNow.getDBData()) {
                    field = fieldNow;
                    break;
                }
            }
            if(field==null) {
                throw new IllegalStateException("ChannelAccessLocal logic error");
            }
            if(dataListener!=null) {
                dataListener.newData(field,dbData);
            } else {
                notifyListener.newData(field);
            }
        }
        public void unlisten(RecordListener listener) {
            if(fieldGroup!=null) stop();
        }

    }
}
