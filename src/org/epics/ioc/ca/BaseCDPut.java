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
public class BaseCDPut implements CDPut, ChannelPutRequester, ChannelGetRequester {

    public BaseCDPut(Channel channel,ChannelFieldGroup channelFieldGroup,
        CDPutRequester cdPutRequester,boolean process)
    {
        this.channel = channel;
        this.cdPutRequester = cdPutRequester;
        channelGet = channel.createChannelGet(channelFieldGroup, this,false);
        channelPut = channel.createChannelPut(channelFieldGroup, this, process);
    }

    private Channel channel;
    private CDPutRequester cdPutRequester;               
    private ChannelGet channelGet;
    private ChannelPut channelPut;  
    private CDField[] cdFields = null;
    private ReentrantLock lock = new ReentrantLock();
    private Condition moreWork = lock.newCondition();
    private boolean isDone = false;
    private RequestResult requestResult = null;

    public void destroy() {
        channel.destroy(channelGet);
        channel.destroy(channelPut);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return cdPutRequester.getRequesterName();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        cdPutRequester.message(message, messageType);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDPut#get()
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
                cdPutRequester.getDone(requestResult);
            }finally {
                lock.unlock();
            }
        } catch(InterruptedException e) {}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDPut#put()
     */
    public void put(CD cd) {
        cdFields = cd.getCDRecord().getCDStructure().getCDFields();
        isDone = false;
        requestResult = RequestResult.success;
        channelPut.put();
        try {
            lock.lock();
            try {
                while(!isDone) {
                    moreWork.await();
                }
                cdPutRequester.putDone(requestResult);
            }finally {
                lock.unlock();
            }
        } catch(InterruptedException e) {}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedPutField(PVField field) {
        throw new IllegalStateException("Logic error");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#nextPutField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextPutField(ChannelField channelField, PVField pvField) {
        int length = cdFields.length;
        for(int i=0; i<length; i++) {
            if(cdFields[i].getChannelField()==channelField) {
                CDField cdField = cdFields[i];
                cdField.get(pvField,true);
                return false;
            }
        }
        throw new IllegalStateException("Logic error");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPutRequester#putDone(org.epics.ioc.util.RequestResult)
     */
    public void putDone(RequestResult requestResult) {
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