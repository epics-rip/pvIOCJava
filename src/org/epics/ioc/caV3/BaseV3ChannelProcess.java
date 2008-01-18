/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Base class that implements ChannelProcess for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3ChannelProcess implements
ConnectionListener,ChannelProcess,PutListener
{
    private V3Channel channel = null;
    private gov.aps.jca.Channel jcaChannel = null;
    
    private ReentrantLock lock = new ReentrantLock();
    private Condition waitForConnect = lock.newCondition();
    private volatile boolean isReady = false;
    private volatile boolean connected = false;
    
    private boolean isDestroyed = false;
    private ChannelProcessRequester channelProcessRequester = null;
         
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
     * @param channel The V3Channel
     * @return (false,true) if the channelProcess (did not, did) properly initialize.
     */
    public boolean init(V3Channel channel) {
        this.channel = channel;
        Context context = channel.getContext();
        String recordName = channel.getDBRecord().getPVRecord().getRecordName();
        isReady = false;
        connected = false;
        try {
            jcaChannel = context.createChannel(recordName + "." + "PROC",this);
            isReady = true;
        } catch (Exception e) {
            channelProcessRequester.message(e.getMessage(),MessageType.error);
            return false;
        }
        lock.lock();
        try {
            if(!connected) waitForConnect.await(3, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            channelProcessRequester.message(
                    " did not connect " + e.getMessage(),MessageType.error);
            return false;
        } finally {
            lock.unlock();
            waitForConnect = null;
            lock = null;
        }
        if(!connected) {
            channelProcessRequester.message(
                    " did not connect ",MessageType.error);
            return false;
        }
        return true;
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
        if(arg0.isConnected()) {
            lock.lock();
            try {
                connected = true;
                waitForConnect.signal();
                return;
            } finally {
                lock.unlock();
            }
        }
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcess#destroy()
     */
    public void destroy() {
        isDestroyed = true;
        try {
            jcaChannel.destroy();
        } catch (CAException e) {
            channelProcessRequester.message(e.getMessage(),MessageType.error);
        }
        channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcess#process()
     */
    public void process() {
        if(isDestroyed) {
            channelProcessRequester.processDone(RequestResult.zombie);
            return;
        }
        byte value = 1;
        try {
            jcaChannel.put(value,this);
        } catch (CAException e) {
            channelProcessRequester.message(e.getMessage(),MessageType.error);
            channelProcessRequester.processDone(RequestResult.failure);
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event.PutEvent)
     */
    public void putCompleted(PutEvent arg0) {
        channelProcessRequester.processDone(RequestResult.success);
    }
   
}