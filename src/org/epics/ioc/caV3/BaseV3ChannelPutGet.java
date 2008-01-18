/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutGet;
import org.epics.ioc.ca.ChannelPutGetRequester;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Base class that implements ChannelPutGet for communicating with a V3 IOC.
 * @author mrk
 *
 */

public class BaseV3ChannelPutGet implements ChannelPutGet,ChannelPutRequester,ChannelGetRequester
{
    private ChannelFieldGroup putFieldGroup = null;
    private ChannelFieldGroup getFieldGroup = null;
    private ChannelPutGetRequester channelPutGetRequester = null;
    private boolean process;
    
    private Channel channel = null;
    private ChannelPut channelPut = null;
    private ChannelGet channelGet = null;
    
    private boolean isDestroyed = false;

    /**
     * Constructer
     * @param putFieldGroup The putFieldGroup.
     * @param getFieldGroup The getFieldGroup.
     * @param channelPutGetRequester The channelPutGetRequester.
     * @param process Should the record be processed between tyhe put and the get.
     */
    public BaseV3ChannelPutGet(
            ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
            ChannelPutGetRequester channelPutGetRequester,boolean process)
    {
        this.putFieldGroup = putFieldGroup;
        this.getFieldGroup = getFieldGroup;
        this.channelPutGetRequester = channelPutGetRequester;
        this.process = process;
    }
    /**
     * Initialize the channelPutGet.
     * @param channel The V3Channel
     * @return (false,true) if the channelPutGet (did not, did) properly initialize.
     */
    public boolean init(Channel channel) {
        this.channel = channel;
        channelPut = channel.createChannelPut(putFieldGroup,this, false);
        if(channelPut==null) return false;
        channelGet = channel.createChannelGet(getFieldGroup, this, process);
        if(channelGet==null) {
            channelPut.destroy();
            return false;
        }
        return true;
    }

    public void destroy() {
       isDestroyed = true;
       channelPut.destroy();
       channelGet.destroy();
       channel.remove(this);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutGet#getDelayed(org.epics.ioc.pv.PVField)
     */
    public void getDelayed(PVField pvField) {
        channelGet.getDelayed(pvField);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutGet#putDelayed(org.epics.ioc.pv.PVField)
     */
    public void putDelayed(PVField pvField) {
        channelPut.putDelayed(pvField);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutGet#putGet()
     */
    public void putGet() {
        if(isDestroyed) {
            channelPutGetRequester.putDone(RequestResult.zombie);
            channelPutGetRequester.getDone(RequestResult.zombie);
            return;
        }
        channelPut.put();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedPutField(PVField field) {
        return channelPutGetRequester.nextDelayedPutField(field);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextPutField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextPutField(ChannelField channelField, PVField pvField) {
        return channelPutGetRequester.nextPutField(channelField, pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#putDone(org.epics.ioc.util.RequestResult)
     */
    public void putDone(RequestResult requestResult) {
        channelPutGetRequester.putDone(requestResult);
        channelGet.get();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        channelPutGetRequester.getDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedGetField(PVField pvField) {
        return channelPutGetRequester.nextDelayedGetField(pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextGetField(ChannelField channelField, PVField pvField) {
        return channelPutGetRequester.nextGetField(channelField, pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return channelPutGetRequester.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        channelPutGetRequester.message(message, messageType);   
    }
}