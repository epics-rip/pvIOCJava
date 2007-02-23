/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mrk
 *
 */
public class BaseChannelDataQueue implements ChannelDataQueue {
    private ReentrantLock lock = new ReentrantLock();
    private int queueSize;
    private ArrayList<ChannelData> freeList;
    private ArrayList<ChannelData> inUseList;
    private ChannelData next = null;
    private int numberMissed = 0;

    public BaseChannelDataQueue(ChannelData[] queue) {
        queueSize = queue.length;
        freeList = new ArrayList<ChannelData>(queueSize);
        for(ChannelData data : queue) freeList.add(data);
        inUseList = new ArrayList<ChannelData>(queueSize);
    }
   /* (non-Javadoc)
    * @see org.epics.ioc.ca.ChannelDataQueue#capacity()
    */
   public int capacity() {
       return queueSize;
   }
   /* (non-Javadoc)
    * @see org.epics.ioc.ca.ChannelDataQueue#getFree()
    */
   public ChannelData getFree(boolean forceFree) {
       lock.lock();
       try {
           if(freeList.size()>0) {
               ChannelData data = freeList.remove(0);
               inUseList.add(data);
               return data;
           }
           numberMissed++;
           if(!forceFree) return null;
           if(inUseList.size()<=0) return null;
           ChannelData data = inUseList.remove(0);
           inUseList.add(data);
           return data;
       } finally {
           lock.unlock();
       }
   }
   /* (non-Javadoc)
    * @see org.epics.ioc.ca.ChannelDataQueue#getNext()
    */
   public ChannelData getNext() {
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
    * @see org.epics.ioc.ca.ChannelDataQueue#getNumberMissed()
    */
   public int getNumberMissed() {
       int number = numberMissed;
       numberMissed = 0;
       return number;
   }
   /* (non-Javadoc)
    * @see org.epics.ioc.ca.ChannelDataQueue#getNumberFree()
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
    * @see org.epics.ioc.ca.ChannelDataQueue#releaseNext(org.epics.ioc.ca.CDBData)
    */
   public void releaseNext(ChannelData channelData) {
       lock.lock();
       try {
           if(next!=channelData) {
               throw new IllegalStateException("channelDataField is not that returned by getNext");
           }
           channelData.clearNumPuts();
           freeList.add(next);
           next = null;
       } finally {
           lock.unlock();
       }
   }
}
