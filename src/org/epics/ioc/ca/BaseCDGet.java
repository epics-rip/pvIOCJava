/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;


/**
 * Base class for CD.
 * @author mrk
 *
 */
public class BaseCDGet implements CDGet, ChannelGetRequester {

    public BaseCDGet(Channel channel,ChannelFieldGroup channelFieldGroup,
        CDGetRequester cdGetRequester,boolean process)
    {
        this.channel = channel;
        this.cdGetRequester = cdGetRequester;
        channelGet = channel.createChannelGet(channelFieldGroup, this,process);
    }

    private Channel channel;
    private CDGetRequester cdGetRequester;           
    private ChannelGet channelGet = null;
    private CDField[] cdFields = null;
    private ReentrantLock lock = new ReentrantLock();
    private Condition moreWork = lock.newCondition();
    private boolean isDone = false;
    private RequestResult requestResult = null;

    public void destroy() {
        channelGet.destroy();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return cdGetRequester.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        cdGetRequester.message(message, messageType);
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDGet#get()
     */
    public void get(CD cd) {
        cdFields = cd.getCDRecord().getCDStructure().getCDFields();
        isDone = false;
        requestResult = RequestResult.success;
        channelGet.get();
        try {
            lock.lock();
            try {
                while(!isDone) {
                    moreWork.await();
                }
                cdGetRequester.getDone(requestResult);
            }finally {
                lock.unlock();
            }
        } catch(InterruptedException e) {}
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        lock.lock();
        try {
            isDone = true;
            this.requestResult = requestResult;
            moreWork.signal();
        } finally {
            lock.unlock();
        }
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedGetField(PVField pvField) {
        throw new IllegalStateException("Logic error");
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextGetField(ChannelField channelField, PVField pvField) {
        int length = cdFields.length;
        for(int i=0; i<length; i++) {
            if(cdFields[i].getChannelField()==channelField) {
                CDField cdField = cdFields[i];
                cdField.put(pvField);
                return false;
            }
        }
        throw new IllegalStateException("Logic error");
    }
}