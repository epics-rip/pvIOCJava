/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelMonitorNotify;
import org.epics.ioc.ca.ChannelMonitorNotifyFactory;
import org.epics.ioc.ca.ChannelMonitorNotifyRequester;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Implementation for a channel access monitorNotify link.
 * @author mrk
 *
 */
public class MonitorNotifySupportBase extends AbstractLinkSupport
implements RecordProcessRequester,ChannelMonitorNotifyRequester
{
    private ChannelMonitorNotify channelMonitorNotify = null;
    private boolean isActive;
    
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param dbStructure The dbStructure for the field being supported.
     */
    public MonitorNotifySupportBase(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.CASupportFactory.CASupport#initialize()
     */
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        if(!recordProcess.setRecordProcessRequester(this)) {
            message("notifySupport but record already has recordProcessor",MessageType.error);
            uninitialize();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        if(!super.checkSupportState(SupportState.ready,null)) return;
        super.connect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        isActive = false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        // nothing to do
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            String fieldName = channel.getFieldName();
            if(fieldName==null) fieldName = "value";
            ChannelField channelField = channel.createChannelField(fieldName);
            ChannelFieldGroup channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(channelField);
            channelMonitorNotify = ChannelMonitorNotifyFactory.create(channel, this);
            channelMonitorNotify.setFieldGroup(channelFieldGroup);
            channelMonitorNotify.start();
            
        } else {
            channelMonitorNotify.destroy();
            channelMonitorNotify = null;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitorNotifyRequester#monitorEvent()
     */
    public void monitorEvent() {
        if(isActive) {
            dbRecord.lock();
            try {
                alarmSupport.setAlarm(
                    "channelMonitorNotify event but record already active", AlarmSeverity.minor);
            } finally {
                dbRecord.unlock();
            }
            return;
        }
        recordProcess.process(this, false, null);
    } 
}
