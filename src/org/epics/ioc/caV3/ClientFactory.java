
/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.DBR_TIME_String;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import java.util.List;
import java.util.regex.Pattern;

import org.epics.ioc.ca.AbstractChannel;
import org.epics.ioc.ca.BaseChannelField;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelMonitor;
import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelProvider;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutGet;
import org.epics.ioc.ca.ChannelPutGetRequester;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVByteArray;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVDoubleArray;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVFloatArray;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVIntArray;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVShortArray;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.RunnableReady;
import org.epics.ioc.util.ThreadCreate;
import org.epics.ioc.util.ThreadFactory;
import org.epics.ioc.util.ThreadReady;

/**
 * Factory and implementation of Channel Access V3 client. This provides communication
 * between a javaIOC and a V3 EPICS IOC.
 * @author mrk
 *
 */
public class ClientFactory  {
    private static ChannelProviderImpl channelAccess = new ChannelProviderImpl();
    private static JCALibrary jca = null;
    private static Context context = null;
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static DBD dbd = DBDFactory.getMasterDBD();
    private static ThreadCreate threadCreate = ThreadFactory.getThreadCreate();
    
    public static void start() {
        channelAccess.register();
    }
    
    private static class ChannelProviderImpl
    implements ChannelProvider, ContextExceptionListener, ContextMessageListener
    {
        static private final String providerName = "caV3";
        static private final Pattern periodPattern = Pattern.compile("[.]");
        static private final Pattern leftBracePattern = Pattern.compile("[{]");
        static private final Pattern rightBracePattern = Pattern.compile("[}]");
        private boolean isRegistered = false; 
        private CAThread caThread = null;
        
        synchronized void register() {
            if(isRegistered) return;
            isRegistered = true;
            try {
                jca = JCALibrary.getInstance();
                context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
                context.addContextExceptionListener(this);
                context.addContextMessageListener(this);
                caThread = new CAThread("cav3",3);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return;
            }
            
            ChannelFactory.registerChannelProvider(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#createChannel(java.lang.String, org.epics.ioc.ca.ChannelListener)
         */
        public synchronized Channel createChannel(String pvName,ChannelListener listener) {
            String recordName = null;
            String fieldName = null;
            String options = null;
            String[] names = periodPattern.split(pvName,2);
            recordName = names[0];
            if(names.length==2) {
                names = leftBracePattern.split(names[1], 2);
                fieldName = names[0];
                if(fieldName.length()==0) fieldName = null;
                if(names.length==2) {
                    names = rightBracePattern.split(names[1], 2);
                    options = names[0];
                }
            }
            return new ChannelImpl(listener,recordName,fieldName,options);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#getProviderName()
         */
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#isProvider(java.lang.String)
         */
        public boolean isProvider(String channelName) {            
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#destroy()
         */
        public void destroy() {
            caThread.stop();
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextExceptionListener#contextException(gov.aps.jca.event.ContextExceptionEvent)
         */
        public void contextException(ContextExceptionEvent arg0) {
            String message = arg0.getMessage();
            System.out.println(message);
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextExceptionListener#contextVirtualCircuitException(gov.aps.jca.event.ContextVirtualCircuitExceptionEvent)
         */
        public void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent arg0) {
            String message = "status " + arg0.getStatus().toString();
            System.out.println(message);
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextMessageListener#contextMessage(gov.aps.jca.event.ContextMessageEvent)
         */
        public void contextMessage(ContextMessageEvent arg0) {
            String message = arg0.getMessage();
            System.out.println(message);
        }
    }
    
    private static class ChannelImpl extends AbstractChannel implements ConnectionListener {
        private String recordName;
        
        private gov.aps.jca.Channel channel = null;
        private volatile boolean isReady = false;
        private boolean isConnected = false;
        private int elementCount = 0;
        private DBRType valueDBRType = null;
        private PVRecord pvRecord = null;
        private DBRType requestDBRType = null;
        
        private ChannelImpl(ChannelListener listener,
                String recordName,String fieldName, String options)
        {
            super(listener,fieldName,options);
            this.recordName = recordName;
            
            if(fieldName==null) {
                channelName = recordName + ".VAL";
            } else {
                channelName =  recordName + "." + fieldName;
            }
            try {
                 channel = context.createChannel(channelName,this);
                 isReady = true;
            } catch (Exception e) {
                message(e.getMessage(),MessageType.error);
                return;
            }
            
        }
        
        private void createPVRecord() {
            DBDStructure dbdAlarm = dbd.getStructure("alarm");
            DBDStructure dbdTimeStamp = dbd.getStructure("timeStamp");            
            Type type = null;
            if(valueDBRType.isBYTE()) {
                type = Type.pvByte;
                requestDBRType = DBRType.TIME_BYTE;
            } else if(valueDBRType.isSHORT()) {
                type= Type.pvShort;
                requestDBRType = DBRType.TIME_SHORT;
            } else if(valueDBRType.isINT()) {
                type = Type.pvInt;
                requestDBRType = DBRType.TIME_INT;
            } else if(valueDBRType.isFLOAT()) {
                type = Type.pvFloat;
                requestDBRType = DBRType.TIME_FLOAT;
            } else if(valueDBRType.isDOUBLE()) {
                type = Type.pvDouble;
                requestDBRType = DBRType.TIME_DOUBLE;
            } else if(valueDBRType.isSTRING()) {
                type = Type.pvString;
                requestDBRType = DBRType.TIME_STRING;
            } else if(valueDBRType.isENUM()) {
                // marty do something
            }
            Type elementType = null;
            Field valueField = null;
            if(elementCount<2) {
                valueField = fieldCreate.createField("value", type);
            }
            if(elementCount>1) {
                elementType = type;
                type = Type.pvArray;
                valueField = fieldCreate.createArray("value", elementType);
            }
            Field[] fields = new Field[3];
            fields[0] = valueField;
            Field[] alarmFields = dbdAlarm.getFields();
            fields[1] = fieldCreate.createStructure("alarm", "alarm", alarmFields);
            Field[] timeStampFields = dbdTimeStamp.getFields();
            fields[2] = fieldCreate.createStructure("timeStamp", "timeStamp",timeStampFields);
            Structure structure = fieldCreate.createStructure("caV3", "caV3", fields);
            pvRecord = pvDataCreate.createPVRecord(recordName, structure);
            super.SetPVRecord(pvRecord);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#isConnected()
         */
        public synchronized boolean isConnected() {
            if(isDestroyed) {
                return false;
            } else {
                return isConnected;
            }
        }                   
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
         */
        public void connectionChanged(ConnectionEvent arg0) {
            while(!isReady) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    
                }
            }
            isConnected = arg0.isConnected();
            if(isConnected) {
                elementCount = channel.getElementCount();
                valueDBRType = channel.getFieldType();
                createPVRecord();
            } else {
                pvRecord = null;
            }
            channelListener.channelStateChange(this, isConnected);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy()
         */
        public void destroy() {
            ChannelListener channelListener = destroyPvt();
            if(channelListener!=null) channelListener.disconnect(this);
        }
        
        private synchronized ChannelListener destroyPvt() {
            if(isDestroyed) return null;
            isDestroyed = true;
            return channelListener;
        }  
                     
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelField(java.lang.String)
         */
        public synchronized ChannelField createChannelField(String name) {
            if(isDestroyed) return null;
            if(name==null || name.length()<=0) return new BaseChannelField(pvRecord);
            PVField pvField = pvRecord.findProperty(name);
            if(pvField==null) return null;
            return new BaseChannelField(pvField);               
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester)
         */
        public synchronized ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester)
        {
            if(isDestroyed) {
                channelProcessRequester.message(
                        "channel has been destroyed",MessageType.fatalError);
                return null;
            }
            ChannelProcessImpl channelProcess;
            try {
                channelProcess = new ChannelProcessImpl(channelProcessRequester);
                super.add(channelProcess);
            } catch(IllegalStateException e) {
                channelProcessRequester.message(
                        e.getMessage(),MessageType.fatalError);
                return null;
            }
            return channelProcess;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester, boolean)
         */
        public synchronized ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequester channelGetRequester, boolean process)
        {
            if(isDestroyed) return null;
            ChannelGetImpl channelGet = 
                new ChannelGetImpl(channelFieldGroup,channelGetRequester,process);
            super.add(channelGet);
            return channelGet;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean)
         */
        public synchronized ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process)
        {
            if(isDestroyed) return null;
            ChannelPutImpl channelPut = 
                new ChannelPutImpl(channelFieldGroup,channelPutRequester,process);
            super.add(channelPut);
            return channelPut;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean)
         */
        public synchronized ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
            ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
            boolean process)
        {
            if(isDestroyed) return null;
            ChannelPutGetImpl channelPutGet = 
                new ChannelPutGetImpl(putFieldGroup,getFieldGroup,
                        channelPutGetRequester,process);
            super.add(channelPutGet);
            return channelPutGet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
         */
        public synchronized ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester)
        {
            if(isDestroyed) {
                channelListener.message(
                        "channel has been destroyed",MessageType.fatalError);
                return null;
            }
            MonitorImpl impl = new MonitorImpl(this,channelMonitorRequester);
            super.add(impl);
            return impl;
        }

        private class ChannelProcessImpl implements ChannelProcess
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelProcessRequester channelProcessRequester = null;
                 
            private ChannelProcessImpl(ChannelProcessRequester channelProcessRequester)
            {
                this.channelProcessRequester = channelProcessRequester;
                requesterName = "Process:" + channelProcessRequester.getRequesterName();
            }           
            public void destroy() {
                isDestroyed = true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            public void process() {
                // TODO Auto-generated method stub
                
            }
        }
        
        private class ChannelGetImpl implements ChannelGet,GetListener
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelGetRequester channelGetRequester = null;
            private boolean process;
            private ChannelFieldGroup channelFieldGroup = null;
            private List<ChannelField> channelFieldList;
            
            private ChannelGetImpl(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequester channelGetRequester,boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.channelFieldGroup = channelFieldGroup;
                this.channelGetRequester = channelGetRequester;
                this.process = process;
                channelFieldList = channelFieldGroup.getList();
                requesterName = "Get:" + channelGetRequester.getRequesterName();
            }
            
            public void destroy() {
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#get()
             */
            public void get() {
                try {
                    channel.get(requestDBRType, elementCount, this);
                } catch (Exception e) {
                    message(e.getMessage(),MessageType.error);
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#getDelayed(org.epics.ioc.pv.PVField)
             */
            public void getDelayed(PVField pvField) {
                // nothing to do
            }

            /* (non-Javadoc)
             * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
             */
            public void getCompleted(GetEvent arg0) {
                gov.aps.jca.dbr.Status status = null;
                gov.aps.jca.dbr.TimeStamp timeStamp = null;
                gov.aps.jca.dbr.Severity severity = null;
                if(requestDBRType==DBRType.TIME_BYTE) {
                    DBR_TIME_Byte dbr = (DBR_TIME_Byte)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVByte pvValue = pvRecord.getByteField("value");
                        pvValue.put(dbr.getByteValue()[0]);
                    } else {
                        PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField("value",Type.pvByte);
                        pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
                    }
                } else if(requestDBRType==DBRType.TIME_SHORT) {
                    DBR_TIME_Short dbr = (DBR_TIME_Short)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVShort pvValue = pvRecord.getShortField("value");
                        pvValue.put(dbr.getShortValue()[0]);
                    } else {
                        PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField("value",Type.pvShort);
                        pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
                    }
                } else if(requestDBRType==DBRType.TIME_INT) {
                    DBR_TIME_Int dbr = (DBR_TIME_Int)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVInt pvValue = pvRecord.getIntField("value");
                        pvValue.put(dbr.getIntValue()[0]);
                    } else {
                        PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField("value",Type.pvInt);
                        pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
                    }
                } else if(requestDBRType==DBRType.TIME_FLOAT) {
                    DBR_TIME_Float dbr = (DBR_TIME_Float)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVFloat pvValue = pvRecord.getFloatField("value");
                        pvValue.put(dbr.getFloatValue()[0]);
                    } else {
                        PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField("value",Type.pvFloat);
                        pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
                    }
                } else if(requestDBRType==DBRType.TIME_DOUBLE) {
                    DBR_TIME_Double dbr = (DBR_TIME_Double)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVDouble pvValue = pvRecord.getDoubleField("value");
                        pvValue.put(dbr.getDoubleValue()[0]);
                    } else {
                        PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField("value",Type.pvDouble);
                        pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
                    }
                } else if(requestDBRType==DBRType.TIME_STRING) {
                    DBR_TIME_String dbr = (DBR_TIME_String)arg0.getDBR();
                    status = dbr.getStatus();
                    timeStamp = dbr.getTimeStamp();
                    severity = dbr.getSeverity();
                    if(elementCount==1) {
                        PVString pvValue = pvRecord.getStringField("value");
                        pvValue.put(dbr.getStringValue()[0]);
                    } else {
                        PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField("value",Type.pvString);
                        pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
                    }
                }
                PVStructure pvStructure = pvRecord.getStructureField("timeStamp", "timeStamp");
                PVLong pvSeconds = pvStructure.getLongField("secondsPastEpoch");
                long seconds = timeStamp.secPastEpoch();
                seconds -= 7305*86400;
                pvSeconds.put(seconds);
                PVInt pvNano = pvStructure.getIntField("nanoSeconds");
                pvNano.put((int)timeStamp.nsec());
                pvStructure = pvRecord.getStructureField("alarm", "alarm");
                PVString pvMessage = pvStructure.getStringField("message");
                pvMessage.put(status.getName());
                PVEnumerated pvEnumerated = (PVEnumerated)pvStructure.getStructureField(
                      "severity","alarmSeverity").getPVEnumerated();
                PVInt pvIndex = pvEnumerated.getIndexField();
                pvIndex.put(severity.getValue());
                channelGetRequester.getDone(RequestResult.success);
            }
        }
        
        private class ChannelPutImpl implements ChannelPut
        {
            private String requesterName;
            private ChannelPutRequester channelPutRequester = null;
            private boolean process;
                        
            private ChannelField[] channelFields;
            
            private ChannelPutImpl(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.channelPutRequester = channelPutRequester;
                this.process = process;
                channelFields = channelFieldGroup.getArray();
                requesterName = "Put:" + channelPutRequester.getRequesterName();
            } 
            
            public void destroy() {
               
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put()
             */
            public void put() {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#putDelayed(org.epics.ioc.pv.PVField)
             */
            public void putDelayed(PVField pvField) {
                // TODO Auto-generated method stub
                
            }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private boolean process;
            
            private ChannelField[] getChannelFields;
            private ChannelField[] putChannelFields;
            private PVField pvField;
            private int fieldIndex;
                 
            private ChannelPutGetImpl(
                ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
                ChannelPutGetRequester channelPutGetRequester,boolean process)
            {
                this.channelPutGetRequester = channelPutGetRequester;
                this.process = process;
                getChannelFields = getFieldGroup.getArray();
                putChannelFields = putFieldGroup.getArray();
                requesterName = "ChannelGetPut:" + channelPutGetRequester.getRequesterName();
            }
            
            public void destroy() {
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#getDelayed(org.epics.ioc.pv.PVField)
             */
            public void getDelayed(PVField pvField) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putDelayed(org.epics.ioc.pv.PVField)
             */
            public void putDelayed(PVField pvField) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet()
             */
            public void putGet() {
                // TODO Auto-generated method stub
                
            }
        }
        
        private class MonitorImpl implements ChannelMonitor
        {
            private Channel channel;
            private ChannelMonitorRequester channelMonitorRequester;
            
            private MonitorImpl(Channel channel,ChannelMonitorRequester channelMonitorRequester) {
                this.channel = channel;
                this.channelMonitorRequester = channelMonitorRequester;
            }
            
            public void destroy() {
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#getData(org.epics.ioc.ca.CD)
             */
            public void getData(CD cd) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start()
             */
            public void start() {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#stop()
             */
            public void stop() {
                // TODO Auto-generated method stub
                
            }
        }
    }
    
    private static class CAThread implements RunnableReady {
        private Thread thread = null;
        private CAThread(String threadName,int threadPriority)
        {
            thread = threadCreate.create(threadName, threadPriority, this);
        }         
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
           
System.out.println("CAV3CLIENT");
context.printInfo();
System.out.println("END CAV3CLIENT");
            threadReady.ready();
            try {
                while(true) {
                    try {
                        context.poll();
                    } catch (CAException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                    Thread.sleep(1);
                }
            } catch(InterruptedException e) {

            }
        }
        
        private void stop() {
            thread.interrupt();
        }
    }
}
