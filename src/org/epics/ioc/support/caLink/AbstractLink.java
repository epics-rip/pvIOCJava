/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelAccess;
import org.epics.ca.client.ChannelAccessFactory;
import org.epics.ca.client.ChannelProvider;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.Channel.ConnectionState;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.AfterStartFactory;
import org.epics.ioc.install.AfterStartNode;
import org.epics.ioc.install.AfterStartRequester;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;

/**
 * Abstract Support for Channel Access Link.
 * This is not public since it is for use by this package.
 * @author mrk
 *
 */
abstract class AbstractLink extends AbstractSupport implements AfterStartRequester,ChannelRequester {
    /**
     * pvDataCreate is for creating PV data.
     */
    protected static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    /**
     * The PVRecordField this link supports.
     */
    protected final PVRecordField pvRecordField;
    /**
     * The pvStructure that this link supports.
     */
    protected final PVStructure pvStructure;
    /**
     * The pvRecord that this link supports.
     */
    protected final PVRecord pvRecord;
    /**
     * The channelRequesterName.
     */
    protected final String channelRequesterName;
    /**
     * The recordProcess for this record.
     */
    protected RecordProcess recordProcess = null;
    /**
     * The alarmSupport.
     */
    protected AlarmSupport alarmSupport = null;
    /**
     * The name of the channel provider.
     */
    protected PVString providerPV = null;
    /**
     * The interface for getting the pvName.
     */
    protected PVString pvnamePV = null;
    
    /**
     * The channel to which this link is connected.
     */
    protected Channel channel = null;
    protected static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    protected ChannelProvider channelProvider = null;
    
    private AfterStartNode afterStartNode = AfterStartFactory.allocNode(this);
    private AfterStart afterStart = null;
    
    
    /**
     * Constructor.
     * @param supportName The name of the support.
     * @param pvField The field which is supported.
     */
    protected AbstractLink(
        String supportName,PVRecordField pvRecordField)
    {
        super(supportName,pvRecordField);
        this.pvRecordField = pvRecordField;
        this.pvStructure = pvRecordField.getPVField().getParent();
        pvRecord = pvRecordField.getPVRecord();
        channelRequesterName = pvRecordField.getFullName();
    }
    /**
     * Must be implemented by derived class and is called by this class.
     * @param isConnected is connected?
     */
    abstract void connectionChange(boolean isConnected);
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
        recordProcess = pvRecord.getRecordProcess();
        PVField pvAlarm = pvStructure.getSubField("alarm");
        if(pvAlarm==null) {
            pvStructure.message("alarm not found", MessageType.error);
            return;
        }
        alarmSupport = AlarmSupportFactory.getAlarmSupport(pvRecord.findPVRecordField(pvAlarm));
        if(alarmSupport==null) {
            pvStructure.message("alarm does not have alarmSupport", MessageType.error);
            return;
        }
        providerPV = pvStructure.getStringField("providerName");
        if(providerPV==null) return;
        pvnamePV = pvStructure.getStringField("pvname");
        if(pvnamePV==null) return;
        super.initialize();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        String providerName = providerPV.get();
        channelProvider = channelAccess.getProvider(providerName);
        if(channelProvider==null) {
            message("providerName " + providerName +  " not found",MessageType.error);
            return;
        }
        super.start(afterStart);
        this.afterStart = afterStart;
        if(providerName.equals("local")) {
            afterStart.requestCallback(afterStartNode, true, ThreadPriority.high);
        } else {
            channelProvider.createChannel(pvnamePV.get(), this,ChannelProvider.PRIORITY_LINKS_DB);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.install.AfterStartRequester#callback(org.epics.ioc.install.AfterStartNode)
     */
    @Override
    public void callback(AfterStartNode node) {
        String providerName = channelProvider.getProviderName();
        String pvname = pvnamePV.get();
        if(providerName.equals("local") || providerName.indexOf('4')>=0) {
            int index = pvname.indexOf('.');
            if(index>0) pvname = pvname.substring(0, index);
        }
        channelProvider.createChannel(pvname, this,ChannelProvider.PRIORITY_LINKS_DB);
        afterStart.done(afterStartNode);
        afterStart = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelRequester#channelCreated(Status,org.epics.ca.client.Channel)
     */
    @Override
    public void channelCreated(Status status, Channel channel) {
    	if (status.isOK()) {
	        this.channel = channel;
    	}
    	else {
            message("pvname " + pvnamePV.get() +  " not created",MessageType.error);
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    @Override
    public void stop() {
        if(channel!=null) channel.destroy();
        channel = null;
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#message(java.lang.String, org.epics.pvData.pv.MessageType)
     */
    @Override
    public void message(String message,MessageType messageType) {
        pvRecord.lock();
        try {
            pvStructure.message(message, messageType);
        } finally {
            pvRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelRequester#channelStateChange(org.epics.ca.client.Channel, org.epics.ca.client.Channel.ConnectionState)
     */
    @Override
    public void channelStateChange(Channel c, ConnectionState connectionState) {
        this.channel = c;
        connectionChange(connectionState == ConnectionState.CONNECTED);
    }
}
