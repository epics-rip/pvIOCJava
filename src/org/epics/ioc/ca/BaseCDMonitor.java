/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.util.ReadyRunnable;
import org.epics.ioc.util.ThreadCreate;
import org.epics.ioc.util.ThreadFactory;

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
    
    protected synchronized void start(String threadName, int threadPriority)  {
        monitorThread = new MonitorThread(threadName,threadPriority);
    }

    protected synchronized CD getFreeCD() {
        return cdQueue.getFree(true);
    }

    protected synchronized void notifyRequestor(CD cd) {
        cdQueue.setInUse(cd);
        monitorThread.signal();
    }

    protected synchronized void stop() {
        monitorThread.stop();
    }
    
    protected static ThreadCreate threadCreate = ThreadFactory.getThreadCreate();
    
    protected CDMonitorRequester cdMonitorRequester;
    private CDQueue cdQueue = null;    
    private MonitorThread monitorThread = null;;    
    
    
    private class MonitorThread implements ReadyRunnable {
        private boolean isReady = false;
        private Thread thread = null;
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();

        private MonitorThread(String threadName,int threadPriority)
        {
            thread = threadCreate.create(threadName, threadPriority, this);
        }         
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ReadyRunnable#isReady()
         */
        public boolean isReady() {
            return isReady;
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                while(true) {
                    CD cd = null;
                    lock.lock();
                    try {
                        isReady = true;
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
