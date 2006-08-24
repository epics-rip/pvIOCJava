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
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.channelAccess.*;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local IOC.
 * @author mrk
 *
 */
public class ChannelAccessLocalFactory  {
    private static ChannelAccess channelAccess = null;
    
    /**
     * Create local channel access.
     * Only one instance will be created.
     * @param iocdb The ioc database that the support will access.
     * @return (false,true) if it (already existed, was created).
     */
    static public boolean create(IOCDB iocdb) {
        if(channelAccess!=null) return false;
        channelAccess = new ChannelAccessLocal(iocdb);
        return true;
    }
    
    private static class ChannelAccessLocal implements ChannelAccess{
        private IOCDB iocdb;
        
        private ChannelAccessLocal(IOCDB iocdb) {
            this.iocdb = iocdb;
            ChannelFactory.registerLocalChannelAccess(this);
        }
        
        public Channel createChannel(String name,ChannelStateListener listener) {
            DBRecord dbRecord = iocdb.findRecord(name);
            if(dbRecord==null) return null;
            return new ChannelImpl(iocdb,dbRecord,listener);
        }
    }
        
    private static class ChannelImpl implements ChannelLink {
        private boolean isDestroyed = false;
        private ChannelStateListener stateListener = null;
        private DBAccess dbAccess;
        private DBRecord linkRecord = null;
        private DBData currentData = null;
        private String otherRecord = null;
        private String otherField = null;
        private double timeout = 0.0;
        private LinkedList<ChannelFieldGroup> fieldGroupList = 
            new LinkedList<ChannelFieldGroup>();
        private LinkedList<ChannelProcess> dataProcessList = 
            new LinkedList<ChannelProcess>();
        private LinkedList<ChannelGet> dataGetList = 
            new LinkedList<ChannelGet>();
        private LinkedList<ChannelPut> dataPutList = 
            new LinkedList<ChannelPut>();
        private LinkedList<ChannelPutGet> dataPutGetList = 
            new LinkedList<ChannelPutGet>();
        private LinkedList<ChannelSubscribe> subscribeList = 
            new LinkedList<ChannelSubscribe>();
        
        private ChannelImpl(IOCDB iocdb,DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbAccess = iocdb.createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ChannelLink#setLinkRecord(org.epics.ioc.dbAccess.DBRecord)
         */
        public void setLinkRecord(DBRecord record) {
            linkRecord = record;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ChannelLink#getLinkRecord()
         */
        public DBRecord getLinkRecord() {
            return linkRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            Iterator<ChannelProcess> iter2 = dataProcessList.iterator();
            while(iter2.hasNext()) {
                ChannelProcess temp = iter2.next();
                temp.destroy();
                iter2.remove();
            }
            Iterator<ChannelGet> iter3 = dataGetList.iterator();
            while(iter3.hasNext()) {
                ChannelGet temp = iter3.next();
                temp.destroy();
                iter3.remove();
            }
            Iterator<ChannelPut> iter4 = dataPutList.iterator();
            while(iter4.hasNext()) {
                ChannelPut temp = iter4.next();
                temp.destroy();
                iter4.remove();
            }
            Iterator<ChannelPutGet> iter5 = dataPutGetList.iterator();
            while(iter5.hasNext()) {
                ChannelPutGet temp = iter5.next();
                temp.destroy();
                iter5.remove();
            }
            Iterator<ChannelSubscribe> iter6 = subscribeList.iterator();
            while(iter6.hasNext()) {
                ChannelSubscribe temp = iter6.next();
                temp.destroy();
                iter6.remove();
            }
            Iterator<ChannelFieldGroup> iter1 = fieldGroupList.iterator();
            while(iter1.hasNext()) {
                ChannelFieldGroup temp = iter1.next();
                temp.destroy();
                iter1.remove();
            }
            stateListener.disconnect(this);
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#isConnected()
         */
        public boolean isConnected() {
            if(isDestroyed) return false;
            return true;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#setField(java.lang.String)
         */
        public ChannelSetFieldResult setField(String name) {
            if(isDestroyed) return ChannelSetFieldResult.failure;
            AccessSetResult result = dbAccess.setField(name);
            if(result==AccessSetResult.notFound) return ChannelSetFieldResult.notFound;
            if(result==AccessSetResult.otherRecord) {
                otherRecord = dbAccess.getOtherRecord();
                otherField = dbAccess.getOtherField();
                return ChannelSetFieldResult.otherChannel;
            }
            if(result==AccessSetResult.thisRecord) {
                currentData = dbAccess.getField();
                return ChannelSetFieldResult.thisChannel;
            }
            throw new IllegalStateException(
                "ChannelAccessLocal logic error unknown AccessSetResult value");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getOtherChannel()
         */
        public String getOtherChannel() {
            if(isDestroyed) return null;
            return otherRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getOtherField()
         */
        public String getOtherField() {
            if(isDestroyed) return null;
            return otherField;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getChannelField()
         */
        public ChannelField getChannelField() {
            if(isDestroyed) return null;
            return new ChannelFieldInstance(currentData);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createFieldGroup(org.epics.ioc.channelAccess.ChannelFieldGroupListener)
         */
        public ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener) {
            if(isDestroyed) return null;
            ChannelFieldGroup fieldGroup = new FieldGroup(listener);
            fieldGroupList.add(fieldGroup);
            return fieldGroup;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#setTimeout(double)
         */
        public void setTimeout(double timeout) {
            this.timeout = timeout;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelProcess()
         */
        public ChannelProcess createChannelProcess() {
            if(isDestroyed) return null;
            ChannelProcess dataProcess = new ChannelProcessInstance(this,dbAccess.getDbRecord());
            dataProcessList.add(dataProcess);
            return dataProcess;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelGet()
         */
        public ChannelGet createChannelGet() {
            if(isDestroyed) return null;
            ChannelGet dataGet = new ChannelGetInstance(this,dbAccess.getDbRecord());
            dataGetList.add(dataGet);
            return dataGet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPut()
         */
        public ChannelPut createChannelPut() {
            if(isDestroyed) return null;
            ChannelPut dataPut = new ChannelPutInstance(this,dbAccess.getDbRecord());
            dataPutList.add(dataPut);
            return dataPut;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPutGet()
         */
        public ChannelPutGet createChannelPutGet() {
            if(isDestroyed) return null;
            ChannelPutGet dataPutGet =  new ChannelPutGetInstance(this,dbAccess.getDbRecord());
            dataPutGetList.add(dataPutGet);
            return dataPutGet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createSubscribe()
         */
        public ChannelSubscribe createSubscribe() {
            if(isDestroyed) return null;
            ChannelSubscribe subscribe =  new Subscribe(this,dbAccess.getDbRecord());
            subscribeList.add(subscribe);
            return subscribe;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#isLocal()
         */
        public boolean isLocal() {
            return true;
        }
    }
    
    private static class ChannelFieldInstance implements ChannelField {
        private DBData dbData;
        
        private ChannelFieldInstance(DBData dbData) {
            this.dbData = dbData;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelField#getAccessRights()
         */
        public AccessRights getAccessRights() {
            // OK until access security is implemented
            if(dbData.getField().isMutable()) {
                return AccessRights.readWrite;
            } else {
                return AccessRights.read;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelField#getField()
         */
        public Field getField() {
            return dbData.getField();
        }       
        private DBData getDBData() {
            return dbData;
        }

    }
    
    private static class FieldGroup implements ChannelFieldGroup {
        private boolean isDestroyed = false;
        private ChannelFieldGroupListener fieldGroupListener;
        private LinkedList<ChannelFieldInstance> fieldList = 
            new LinkedList<ChannelFieldInstance>();

        private FieldGroup(ChannelFieldGroupListener listener) {
            fieldGroupListener = listener;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            fieldList.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#addChannelField(org.epics.ioc.channelAccess.ChannelField)
         */
        public void addChannelField(ChannelField channelField) {
            if(isDestroyed) return;
            fieldList.add((ChannelFieldInstance)channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#removeChannelField(org.epics.ioc.channelAccess.ChannelField)
         */
        public void removeChannelField(ChannelField channelField) {
            if(isDestroyed) return;
            fieldList.remove(channelField);
        }
        
        private List<ChannelFieldInstance> getList() {
            return fieldList;
        }
    }
    private static class ChannelProcessInstance implements ChannelProcess,ProcessCallbackListener,ProcessCompleteListener {
        private ChannelLink channel;
        private RecordProcessSupport linkRecordProcessSupport;
        private RecordProcess recordProcess;
        private boolean isDestroyed = false;
        private ChannelProcessListener listener = null;
        private boolean wait = false;
        
        
        private ChannelProcessInstance(ChannelLink channel,DBRecord dbRecord) {
            this.channel = channel;
            linkRecordProcessSupport = channel.getLinkRecord().getRecordProcess().getRecordProcessSupport();
            recordProcess = dbRecord.getRecordProcess();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            isDestroyed = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process(org.epics.ioc.channelAccess.ChannelProcessListener, boolean)
         */
        public void process(ChannelProcessListener listener, boolean wait) {
            if(isDestroyed) return;
            this.listener = listener;
            this.wait = wait;
            linkRecordProcessSupport.requestProcessCallback(this);
            return;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#callback()
         */
        public void callback() {
            if(isDestroyed) return;
            ProcessReturn processReturn = recordProcess.process(this);
            if(wait && listener!=null) {
                switch(processReturn) {
                case zombie:
                    listener.failure(channel,"zombie"); return;
                case noop:
                    listener.failure(channel,"zombie"); return;
                case success:
                    processComplete(null,ProcessResult.success);
                    return;
                case failure:
                    processComplete(null,ProcessResult.failure);
                    return;
                case active: return;
                case alreadyActive: return;
                default:
                    throw new IllegalStateException("Unknown ProcessReturn state in ChannelAccessLocal");
                }
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#cancelProcess()
         */
        public void cancelProcess() {
            listener = null;
            recordProcess.removeCompletionListener(this);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            if(isDestroyed) return;
            if(listener==null) return;
            RecordProcessSupport recordProcessSupport = recordProcess.getRecordProcessSupport();
            listener.processDone(channel,result,
                recordProcessSupport.getAlarmSeverity(),
                recordProcessSupport.getStatus());
            listener = null;
        }
    }
    private static class ChannelGetInstance implements ChannelGet,ProcessCallbackListener,ProcessCompleteListener {
        private ChannelLink channel;
        private RecordProcessSupport linkRecordProcessSupport;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private DBRecord linkRecord;
        private boolean isDestroyed = false;
        private FieldGroup fieldGroup = null;
        private ChannelGetListener listener = null;
        private boolean wait = false;
        
        private ChannelGetInstance(ChannelLink channel,DBRecord dbRecord) {
            this.channel = channel;
            linkRecordProcessSupport = channel.getLinkRecord().getRecordProcess().getRecordProcessSupport();
            this.dbRecord = dbRecord;
            recordProcess = dbRecord.getRecordProcess();
            linkRecord = channel.getLinkRecord();
         }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            return;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#get(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelGetListener, boolean, boolean)
         */
        public void get(ChannelFieldGroup fieldGroup,
        ChannelGetListener listener, boolean process, boolean wait) {
            if(isDestroyed) return;
            this.fieldGroup = (FieldGroup)fieldGroup;
            this.listener = listener;
            this.wait = wait;
            if(!process) {
                processComplete(ProcessResult.success);
                return;
            }
            linkRecordProcessSupport.requestProcessCallback(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#callback()
         */
        public void callback() {
            ProcessReturn processReturn = recordProcess.process((wait ? this : null));
            switch(processReturn) {
            case zombie:
                listener.failure(channel,"zombie"); return;
            case noop:
                // no break
            case success:
                processComplete(ProcessResult.success);
                return;
            case failure:
                processComplete(ProcessResult.failure);
                return;
            case active:
            case alreadyActive:
                if(!wait) processComplete(ProcessResult.success);
                return;
            default:
                throw new IllegalStateException("Unknown ProcessReturn state in ChannelAccessLocal");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#cancelGet()
         */
        public void cancelGet() {
            listener = null;
            recordProcess.removeCompletionListener(this);
        }

        private void processComplete(ProcessResult result) {
            if(isDestroyed) return;
            if(listener==null) return;
            linkRecord.lock();
            linkRecord.lockOtherRecord(dbRecord);
            try {
                listener.beginSynchronous(channel);
                List<ChannelFieldInstance> list = fieldGroup.getList();
                Iterator<ChannelFieldInstance> iter = list.iterator();
                while(iter.hasNext()) {
                    ChannelFieldInstance field = iter.next();
                    listener.newData(channel,field,field.getDBData());
                }
                listener.endSynchronous(channel);
            } finally {
                dbRecord.unlock();
                linkRecord.unlock();
            
            }
            listener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            processComplete(result);
        }
        
    }
    
    private static class ChannelPutInstance implements ChannelPut,ProcessCompleteListener,ProcessCallbackListener {
        private ChannelLink channel;
        private RecordProcessSupport linkRecordProcessSupport;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private DBRecord linkRecord;
        private boolean isDestroyed = false;
        private boolean wait;
        
        private ChannelPutListener listener = null;

        private ChannelPutInstance(ChannelLink channel,DBRecord dbRecord) {
            this.channel = channel;
            linkRecordProcessSupport = channel.getLinkRecord().getRecordProcess().getRecordProcessSupport();
            this.dbRecord = dbRecord;
            recordProcess = dbRecord.getRecordProcess();
            linkRecord = channel.getLinkRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#destroy()
         */
        public void destroy() {
            isDestroyed = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#put(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelPutListener, boolean, boolean)
         */
        public void put(ChannelFieldGroup fieldGroup, ChannelPutListener callback, boolean process, boolean wait) {
            linkRecord.lockOtherRecord(dbRecord);
            try {
                List<ChannelFieldInstance> list = ((FieldGroup)fieldGroup).getList();
                Iterator<ChannelFieldInstance> iter = list.iterator();
                while(iter.hasNext()) {
                    ChannelFieldInstance field = iter.next();
                    listener.nextData(channel,field,field.dbData);
                }
            } finally {
                dbRecord.unlock();
            }
            if(!process) return;
            this.listener = callback;
            this.wait = wait;
            linkRecordProcessSupport.requestProcessCallback(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#callback()
         */
        public void callback() {
            if(isDestroyed) return;
            ProcessReturn processReturn = recordProcess.process(this);
            if(wait && listener!=null) {
                switch(processReturn) {
                case zombie:
                    listener.failure(channel,"zombie"); return;
                case noop:
                    // no break
                case success:
                    processComplete(null,ProcessResult.success);
                    return;
                case failure:
                    processComplete(null,ProcessResult.failure);
                    return;
                case active: return;
                case alreadyActive: return;
                default:
                    throw new IllegalStateException("Unknown ProcessReturn state in ChannelAccessLocal");
                }
            }
            
        }
        
        private void processComplete(ProcessResult result) {
            if(isDestroyed) return;
            if(listener==null) return;
            RecordProcessSupport recordProcessSupport = recordProcess.getRecordProcessSupport();
            listener.processDone(channel,result,
                recordProcessSupport.getAlarmSeverity(),
                recordProcessSupport.getStatus());
            listener = null;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            processComplete(result);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#cancelPut()
         */
        public void cancelPut() {
            listener = null;
            recordProcess.removeCompletionListener(this);
        }
        
    }
    
    private static class ChannelPutGetInstance implements ChannelPutGet,ChannelPutListener {
        private ChannelPutInstance dataPut;
        private ChannelGetInstance dataGet;
        
        private boolean isDestroyed = false;
        private ChannelPutListener putCallback = null;
        private ChannelFieldGroup getFieldGroup = null;
        private ChannelGetListener getCallback = null;
       
        private ChannelPutGetInstance(ChannelLink channel,DBRecord dbRecord) {
            dataPut = new ChannelPutInstance(channel,dbRecord);
            dataGet = new ChannelGetInstance(channel,dbRecord);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#destroy()
         */
        public void destroy() { 
            isDestroyed = true;
            dataPut.destroy();
            dataGet.destroy();
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putGet(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelPutListener, org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelGetListener, boolean, boolean)
         */
        public void putGet(  
        ChannelFieldGroup putFieldGroup, ChannelPutListener putCallback,
        ChannelFieldGroup getFieldGroup, ChannelGetListener getCallback,
        boolean process, boolean wait)
        {
            dataPut.put(putFieldGroup,this,process,wait);
            if(!process) dataGet.get(getFieldGroup,getCallback,false,false);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#failure(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void failure(Channel channel, String reason) {
            putCallback.failure(channel,reason);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#nextData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextData(Channel channel, ChannelField field, PVData data) {
            if(isDestroyed) return;
            putCallback.nextData(channel,field,data);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#processDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.dbProcess.ProcessResult, org.epics.ioc.util.AlarmSeverity, java.lang.String)
         */
        public void processDone(Channel channel, ProcessResult result, AlarmSeverity alarmSeverity, String status) {
            if(isDestroyed) return;
            dataGet.get(getFieldGroup,getCallback,false,false);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#cancelPutGet()
         */
        public void cancelPutGet() {
            if(isDestroyed) return;
            dataPut.cancelPut();
            dataGet.cancelGet();
        }
        
    }
    
    private static class Subscribe implements ChannelSubscribe,DBListener {
        private Channel channel;
        private boolean isDestroyed = false;
        private RecordListener recordListener = null;
        private FieldGroup fieldGroup = null;
        private ChannelNotifyListener notifyListener = null;
        private ChannelNotifyGetListener dataListener = null;
        private Event event = null;
        
        private Subscribe(Channel channel,DBRecord dbRecord)
        {
            this.channel = channel;
            recordListener = dbRecord.createListener(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            if(notifyListener!=null) notifyListener.failure(channel,"deleted");
            if(dataListener!=null) dataListener.failure(channel,"deleted");
            if(fieldGroup!=null) stop();
            recordListener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyGetListener, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup fieldGroup, ChannelNotifyGetListener listener, Event why) {
            if(isDestroyed) return;
            notifyListener = null;
            dataListener = listener;
            startCommon(fieldGroup,why);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyListener, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup fieldGroup, ChannelNotifyListener listener, Event why) {
            if(isDestroyed) return;
            notifyListener = listener;
            dataListener = null;
            startCommon(fieldGroup,why);
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#stop()
         */
        public void stop() {
            if(isDestroyed) return;
            if(fieldGroup==null) return;
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                DBData dbData = field.getDBData();
                dbData.removeListener(this.recordListener);
            }
            event = null;
            fieldGroup = null;
            dataListener = null;
            notifyListener = null;
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginSynchronous()
         */
        public void beginSynchronous() {
            if(dataListener!=null) {
                dataListener.beginSynchronous(channel);
            }
            if(notifyListener!=null) {
                notifyListener.beginSynchronous(channel);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
            if(dataListener!=null) {
                dataListener.endSynchronous(channel);
            }
            if(notifyListener!=null) {
                notifyListener.endSynchronous(channel);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
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
                throw new IllegalStateException("ChannelAccessLocalFactory logic error");
            }
            if(dataListener!=null) {
                dataListener.newData(channel,field,dbData);
            } else {
                notifyListener.newData(channel,field);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            if(fieldGroup!=null) stop();
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
                dbData.addListener(this.recordListener);
            }
        }
    }
}
