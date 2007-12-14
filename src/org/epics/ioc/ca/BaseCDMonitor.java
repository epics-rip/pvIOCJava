/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mrk
 *
 */
public class BaseCDMonitor {
    
    protected BaseCDMonitor(CDMonitorRequester cdMonitorRequester)
    {
        
        this.cdMonitorRequester = cdMonitorRequester;
    }
    
    protected void createQueue(Channel channel,int queueSize,ChannelFieldGroup channelFieldGroup) {
        cdQueue = CDFactory.createCDQueue(queueSize, channel, channelFieldGroup);
    }
    
    protected void start(String threadName, int threadPriority)  {
        monitorThread = new MonitorThread(threadName,threadPriority);
    }
    
    protected CD getFreeCD() {
        lock.lock();
        try {
            return cdQueue.getFree(true);
        } finally {
            lock.unlock();
        }
    }
    
    protected void notifyRequestor(CD cd) {
        lock.lock();
        try {
            cdQueue.setInUse(cd);
            monitorThread.signal();
        } finally {
            lock.unlock();
        }
    }
    
    protected void stop() {
        monitorThread.stop();
    }
    
    protected CDMonitorRequester cdMonitorRequester;
    private CDQueue cdQueue = null;    
    private MonitorThread monitorThread = null;;    
    private ReentrantLock lock = new ReentrantLock();
    
    private class MonitorThread implements Runnable {
        
        private Thread thread = null;
        private Condition moreWork = lock.newCondition();
        private boolean isRunning = false;

        private MonitorThread(String threadName,int threadPriority)
        {
            thread = new Thread(this,threadName);
            thread.setPriority(threadPriority);
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        } 
        
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            isRunning = true;
            try {
                while(true) {
                    CD cd = null;
                    lock.lock();
                    try {
                        while(true) {
                            cd = cdQueue.getNext();
                            if(cd!=null) break;
                            moreWork.await();
                        }
                    }finally {
                        lock.unlock();
                    }
                    if(cd!=null) {
                        int missed = cdQueue.getNumberMissed();
                        if(missed>0) cdMonitorRequester.dataOverrun(missed);
                        cdMonitorRequester.monitorCD(cd);
                        cdQueue.releaseNext(cd);
                    }
                }
            } catch(InterruptedException e) {

            }
        }
        private void signal() {
            lock.lock();
            try {
                moreWork.signal();
            } finally {
                lock.unlock();
            }
        }
        private void stop() {
            thread.interrupt();
        }
    }
}
