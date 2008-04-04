/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Create an IOCExecutor.
 * The executor implements a set of threads, one for each ScanPriority.
 * @author mrk
 *
 */
public class IOCExecutorFactory {
    /**
     * Create a new set of threads.
     * @param threadName The name for the set of threads.
     * @param priority The ScanPriority for the thread.
     * @return The IOCExecutor interface.
     */
    static public IOCExecutor create(String threadName, ScanPriority priority) {
        return new ExecutorInstance(threadName,priority);
    }
    
    static private class ExecutorInstance implements IOCExecutor {
        private ThreadInstance thread;

        private ExecutorInstance(String threadName, ScanPriority priority) {
            thread = new ThreadInstance(threadName,priority.getJavaPriority());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCExecutor#execute(java.lang.Runnable)
         */
        public boolean execute(Runnable command) {
            return thread.add(command);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCExecutor#execute(java.util.List)
         */
        public boolean execute(List<Runnable> commands) {
            return thread.add(commands);
        }
    }
    
    static private ThreadCreate threadCreate = ThreadFactory.getThreadCreate();
    
    static private class ThreadInstance implements RunnableReady {
        private List<Runnable> runList = new ArrayList<Runnable>();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();

        private ThreadInstance(String name,int priority) {
            threadCreate.create(name, priority, this);
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            boolean firstTime = true;
            try {
                while(true) {
                    Runnable runnable = null;
                    lock.lock();
                    try {
                        if(firstTime) {
                            firstTime = false;
                            threadReady.ready();
                        }
                        while(runList.isEmpty()) {
                            moreWork.await();
                        }
                        runnable = runList.remove(0);
                    }finally {
                        lock.unlock();
                    }
                    if(runnable!=null) runnable.run();
                }
            } catch(InterruptedException e) {
                
            }
        }
        
        private boolean add(Runnable runnable) {
            boolean wasAdded = false;
            lock.lock();
            try {
                boolean isEmpty = runList.isEmpty();
                wasAdded = runList.add(runnable);
                if(isEmpty) moreWork.signal();
                return wasAdded;
            } finally {
                lock.unlock();
            }
        }
        
        private boolean add(List<Runnable> runnableList) {
            boolean allAdded = false;
            lock.lock();
            try {
                boolean isEmpty = runList.isEmpty();
                int initialSize = runList.size();
                runList.addAll(runnableList);
                int finalSize = runList.size();
                int added = finalSize - initialSize;
                int size = runnableList.size();
                if(added==size) allAdded = true;
                if(isEmpty) moreWork.signal();
                return allAdded;
            } finally {
                lock.unlock();
            }
        }
        
    }
}
