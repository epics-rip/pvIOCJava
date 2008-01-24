/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

import org.epics.ioc.ca.AbstractChannel;
import org.epics.ioc.ca.AbstractChannelField;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelMonitor;
import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutGet;
import org.epics.ioc.ca.ChannelPutGetRequester;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanPriority;

/**
 * Base class that implements V3Channel.
 * @author mrk
 *
 */
public class BaseV3Channel extends AbstractChannel
implements V3Channel,ConnectionListener,Runnable,V3ChannelRecordRequester {

    private static IOCExecutor iocExecutor
        = IOCExecutorFactory.create("caV3Connect", ScanPriority.low);
    private Context context = null;
    private String pvName = null;
    private String recordName = null;
    private String valueFieldName = null;
    private String[] propertyNames = null;

    
    private gov.aps.jca.Channel jcaChannel = null;
    private BaseV3ChannelRecord v3ChannelRecord = null;

    /**
     * The constructer.
     * @param listener The ChannelListener.
     * @param options A string containing options.
     */
    public BaseV3Channel(ChannelListener listener,String options)
    {
        super(listener,options);
    }
    /**
     * init the channel.
     * @param context The JCA Context.
     * @param pvName The pvName.
     * @param recordName The recordName.
     * @param valueFieldName The name of the value field.
     * @param propertyNames An array of desired propertyNames.
     */
    public void init(Context context,String pvName,
            String recordName,String valueFieldName,String[] propertyNames)
    {
        this.context = context;
        this.pvName = pvName;
        this.recordName = recordName;
        this.valueFieldName = valueFieldName;
        this.propertyNames = propertyNames;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getJcaChannel()
     */
    public Channel getJCAChannel() {
        return jcaChannel;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getPVName()
     */
    public String getPVName() {
        return pvName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getV3ChannelRecord()
     */
    public V3ChannelRecord getV3ChannelRecord() {
        return v3ChannelRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getPropertyNames()
     */
    public String[] getPropertyNames() {
        return propertyNames;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getValueFieldName()
     */
    public String getValueFieldName() {
        return valueFieldName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getIOCExecutor()
     */
    public IOCExecutor getIOCExecutor() {
        return iocExecutor;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.AbstractChannel#connect()
     */
    public void connect() {
        try {
            jcaChannel = context.createChannel(pvName,this);
        } catch (CAException e) {
            super.getChannelListener().message(
                "createChannel failed " + e.getMessage(),
                MessageType.error);
            jcaChannel = null;
        };
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.AbstractChannel#disconnect()
     */
    public void disconnect() {
        super.disconnect();
        jcaChannel.dispose();
        jcaChannel = null;
        v3ChannelRecord = null;
    }                     
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createChannelField(java.lang.String)
     */
    public ChannelField createChannelField(String name) {
        if(v3ChannelRecord==null) {
            message("createChannelField but not connected",MessageType.warning);
            return null;
        }
        PVRecord pvRecord = v3ChannelRecord.getPVRecord();
        if(name==null || name.length()<=0) return new ChannelFieldImpl(pvRecord);
        PVField pvField = pvRecord.findProperty(name);
        if(pvField==null) return null;
        return new ChannelFieldImpl(pvField);               
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester)
     */
    public ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester)
    {
        if(v3ChannelRecord==null) {
            channelProcessRequester.message(
                    "createChannelProcess but not connected",MessageType.warning);
            return null;
        }
        BaseV3ChannelProcess channelProcess = new BaseV3ChannelProcess(channelProcessRequester);
        boolean success = channelProcess.init(this);
        if(success) {
            super.add(channelProcess);
            return channelProcess;
        }
        return null;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester, boolean)
     */
    public ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
            ChannelGetRequester channelGetRequester, boolean process)
    {
        if(v3ChannelRecord==null) {
            channelGetRequester.message(
                    "createChannelGet but not connected",MessageType.warning);
            return null;
        }
        BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelFieldGroup,channelGetRequester,process);
        boolean success = channelGet.init(this);
        if(success) {
            super.add(channelGet);
            return channelGet;
        }
        return null;
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean)
     */
    public ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
            ChannelPutRequester channelPutRequester, boolean process)
    {
        if(v3ChannelRecord==null) {
            channelPutRequester.message(
                    "createChannelPut but not connected",MessageType.warning);
            return null;
        }
        BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelFieldGroup,channelPutRequester,process);
        boolean success = channelPut.init(this);
        if(success) {
            super.add(channelPut);
            return channelPut;
        }
        return null;
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean)
     */
    public ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
            ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
            boolean process)
    {
        if(v3ChannelRecord==null) {
            channelPutGetRequester.message(
                    "createChannelPutGet but not connected",MessageType.warning);
            return null;
        }
        BaseV3ChannelPutGet channelPutGet = new BaseV3ChannelPutGet(
                putFieldGroup,getFieldGroup,channelPutGetRequester,process);
        boolean success = channelPutGet.init(this);
        if(success) {
            super.add(channelPutGet);
            return channelPutGet;
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
     */
    public ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester)
    {
        if(v3ChannelRecord==null) {
            message(
                    "createChannelMonitor but not connected",MessageType.warning);    
            return null;
        }
        BaseV3ChannelMonitor channelMonitor = new BaseV3ChannelMonitor(channelMonitorRequester);
        boolean success = channelMonitor.init(this);
        if(success) {
            super.add(channelMonitor);
            return channelMonitor;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        boolean isConnected = arg0.isConnected();
        if(isConnected) {
            iocExecutor.execute(this);
        } else {
            disconnect();
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if(jcaChannel==null) return;
        v3ChannelRecord = new BaseV3ChannelRecord(this);
        if(v3ChannelRecord.createPVRecord(this,recordName)) return;
        disconnect();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelRecordRequester#createPVRecordDone(org.epics.ioc.util.RequestResult)
     */
    public void createPVRecordDone(RequestResult requestResult) {
        super.setPVRecord(v3ChannelRecord.getPVRecord(),valueFieldName);
        super.connect();
    }

    private static class ChannelFieldImpl extends AbstractChannelField {
        private PVField pvField;
        
        private ChannelFieldImpl(PVField pvField) {
            super(pvField);
            this.pvField = pvField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelField#postPut()
         */
        public void postPut() {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelField#findProperty(java.lang.String)
         */
        public ChannelField findProperty(String propertyName) {
            PVField pvf = pvField.findProperty(propertyName);
            if (pvf == null) return null;
            return new ChannelFieldImpl(pvf);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelField#createChannelField(java.lang.String)
         */
        public ChannelField createChannelField(String fieldName) {
            PVField pvf = pvField.getSubField(fieldName);
            if (pvf == null) return null;
            return new ChannelFieldImpl(pvf);
        }
    }
}