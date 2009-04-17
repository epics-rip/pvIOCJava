/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelAccess;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Abstract Support for Channel Access Link.
 * This is not public since it is for use by this package.
 * @author mrk
 *
 */
abstract class AbstractLink extends AbstractSupport implements ChannelListener,ChannelFieldGroupListener {
    /**
     * The channelRequesterName.
     */
    protected String channelRequesterName;
    /**
     * The pvStructure that this link supports.
     */
    protected PVStructure pvStructure;
    /**
     * The pvRecord for pvStructure.
     */
    protected PVRecord pvRecord;
    /**
     * The recordProcess for this record.
     */
    protected RecordProcess recordProcess = null;
    /**
     * The alarmSupport, which can be null.
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
     * The array of propertyNames. AbstractIOLink handles this.
     */
    protected String[] propertyNames = null;
    // propertyPVFields does NOT include alarm
    /**
     * The channel to which this link is connected.
     */
    protected Channel channel = null;
    protected static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    /**
     * Constructor.
     * @param supportName The name of the support.
     * @param pvField The field which is supported.
     */
    protected AbstractLink(
        String supportName,PVField pvField)
    {
        super(supportName,pvField);
        this.pvStructure = pvField.getParent();
        pvRecord = pvStructure.getPVRecord();
        channelRequesterName = pvField.getFullName();
    }
    
    /**
     * Called after derived class is started
     */
    protected void connect() {
        if(super.getSupportState()!=SupportState.ready) return;
        channel.connect();
    }
    
    /**
     * Must be implemented by derived class and is called by this class.
     * @param isConnected is connected?
     */
    abstract void connectionChange(boolean isConnected);
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
       
        recordProcess = recordSupport.getRecordProcess();
        PVField pvAlarm = pvStructure.getSubField("alarm");
        if(pvAlarm==null) {
            pvStructure.message("alarm not found", MessageType.error);
            return;
        }
        alarmSupport = AlarmSupportFactory.getAlarmSupport(pvAlarm,recordSupport);
        if(alarmSupport==null) {
            pvStructure.message("alarm does not have alarmSupport", MessageType.error);
            return;
        }
        providerPV = pvStructure.getStringField("providerName");
        if(providerPV==null) return;
        pvnamePV = pvStructure.getStringField("pvname");
        if(pvnamePV==null) return;
        super.initialize(recordSupport);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        String providerName = providerPV.get();
        String pvname = pvnamePV.get();
        channel = channelAccess.createChannel(pvname,propertyNames, providerName, this);
        if(channel==null) {
            message("providerName " + providerName + " pvname " + pvname + " not found",MessageType.error);
            return;
        }
        super.start(afterStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    public void stop() {
        channel.destroy();
        channel = null;
        propertyNames = null;
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message,MessageType messageType) {
        pvRecord.lock();
        try {
            pvStructure.message(message, messageType);
        } finally {
            pvRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
     */
    public void destroy(Channel c) {
        pvRecord.lock();
        try {
            if(super.getSupportState()!=SupportState.ready) return;
        } finally {
            pvRecord.unlock();
        }
        recordProcess.stop();
        recordProcess.start(null);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
     */
    public void channelStateChange(Channel c, boolean isConnected) {
        connectionChange(isConnected);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
     */
    public void accessRightsChange(Channel channel, ChannelField channelField) {
        // nothing to do         
    }
}
