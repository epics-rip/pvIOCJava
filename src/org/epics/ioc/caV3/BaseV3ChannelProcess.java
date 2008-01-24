/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Base class that implements ChannelProcess for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3ChannelProcess implements
ChannelProcess,ChannelListener,Runnable,ChannelPutRequester,ChannelFieldGroupListener
{
    private ChannelProcessRequester channelProcessRequester = null;
    
    private V3Channel v3Channel = null;
    private IOCExecutor iocExecutor = null;
    private Channel putChannel = null;
    
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelField channelField = null;
    private ChannelPut channelPut = null;
    
    private boolean isReady = false;
    private boolean isDestroyed = false;
    
         
    /**
     * Constructer.
     * @param channelProcessRequester The channelProcessRequester.
      */
    public BaseV3ChannelProcess(ChannelProcessRequester channelProcessRequester)
    {
        this.channelProcessRequester = channelProcessRequester;
    }
    /**
     * Initialize the channelProcess.
     * @param v3Channel The V3Channel
     * @return (false,true) if the channelProcess (did not, did) properly initialize.
     */
    public boolean init(V3Channel v3Channel) {
        this.v3Channel = v3Channel;
        iocExecutor = v3Channel.getIOCExecutor();
        String pvName = v3Channel.getV3ChannelRecord().getPVRecord().getRecordName() + "." + "PROC";
        putChannel = ChannelAccessFactory.getChannelAccess().createChannel(
            pvName, null, "caV3", this);
        putChannel.connect();
        return true;
    }
   
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return channelProcessRequester.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        channelProcessRequester.message(message, messageType);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
     */
    public void channelStateChange(Channel c, boolean isConnected) {
        if(isConnected) {
            iocExecutor.execute(this);
        } else {
            destroy(c);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#destroy(org.epics.ioc.ca.Channel)
     */
    public void destroy(Channel c) {
        if(!isDestroyed) destroy();
        c.destroy();
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        channelField = putChannel.createChannelField("PROC");
        channelFieldGroup = putChannel.createFieldGroup(this);
        channelFieldGroup.addChannelField(channelField);
        channelPut = putChannel.createChannelPut(channelFieldGroup, this, false);
        isReady = true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedPutField(PVField field) {
        // nothing to do
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextPutField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextPutField(ChannelField channelField, PVField pvField) {
        PVByte pvByte = (PVByte)pvField;
        pvByte.put((byte)1);
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#putDone(org.epics.ioc.util.RequestResult)
     */
    public void putDone(RequestResult requestResult) {
        channelProcessRequester.processDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcess#destroy()
     */
    public void destroy() {
        isReady = false;
        isDestroyed = true;
        destroy(putChannel);
        v3Channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcess#process()
     */
    public void process() {
        if(isDestroyed) {
            channelProcessRequester.processDone(RequestResult.zombie);
            return;
        }
        if(!isReady) {
            channelProcessRequester.message("not connected", MessageType.error);
            channelProcessRequester.processDone(RequestResult.failure);
            return;
        }
        channelPut.put();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
     */
    public void accessRightsChange(Channel channel, ChannelField channelField) {
        // nothing to do for now
    }
}