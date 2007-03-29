/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for a CDQueue.
 * @author mrk
 *
 */
public class BaseCDQueue implements CDQueue {
    private ReentrantLock lock = new ReentrantLock();
    private int queueSize;
    private ArrayList<CD> freeList;
    private ArrayList<CD> inUseList;
    private CD next = null;
    private int numberMissed = 0;

    /**
     * Constructor.
     * @param queue The array of CD to put in the queue.
     */
    public BaseCDQueue(CD[] queue) {
        queueSize = queue.length;
        freeList = new ArrayList<CD>(queueSize);
        for(CD cD : queue) freeList.add(cD);
        inUseList = new ArrayList<CD>(queueSize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#capacity()
     */
    public int capacity() {
        return queueSize;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#getFree()
     */
    public CD getFree(boolean forceFree) {
        lock.lock();
        try {
            if(freeList.size()>0) {
                CD cD = freeList.remove(0);
                return cD;
            }
            numberMissed++;
            if(!forceFree) return null;
            if(inUseList.size()<=0) return null;
            CD cD = inUseList.remove(0);
            return cD;
        } finally {
            lock.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#setInUse(org.epics.ioc.ca.CD)
     */
    public void setInUse(CD cD) {
        inUseList.add(cD);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#getNext()
     */
    public CD getNext() {
        lock.lock();
        try {
            if(next!=null) {
                throw new IllegalStateException("already have next");
            }
            if(inUseList.size()<=0) return null;
            next = inUseList.remove(0);
            return next;
        } finally {
            lock.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#getNumberMissed()
     */
    public int getNumberMissed() {
        int number = numberMissed;
        numberMissed = 0;
        return number;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#getNumberFree()
     */
    public int getNumberFree() {
        lock.lock();
        try {
            return freeList.size();
        } finally {
            lock.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDQueue#releaseNext(org.epics.ioc.ca.CDField)
     */
    public void releaseNext(CD cD) {
        lock.lock();
        try {
            if(next!=cD) {
                throw new IllegalStateException("channelDataField is not that returned by getNext");
            }
            cD.clearNumPuts();
            freeList.add(next);
            next = null;
        } finally {
            lock.unlock();
        }
    }
}
