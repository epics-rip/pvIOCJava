/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Create an IOCExecutor.
 * The executor implements a set of threads, one for each ScanPriority.
 * @author mrk
 *
 */
public class IOCExecutorFactory {
    /**
     * Create a new set of threads.
     * @param name The name for the set of threads.
     * @return The IOCExecutor interface.
     */
    static public IOCExecutor create(String name) {
        return new ExecutorInstance(name);
    }
    
    static private class ExecutorInstance implements IOCExecutor {
        private ThreadInstance[] threads;

        private ExecutorInstance(String name) {
            super();
            int[] javaPriority = ScanPriority.javaPriority;
            int numPriorities = javaPriority.length;
            threads = new ThreadInstance[numPriorities];
            for(int i=0; i<numPriorities; i++) {
                int priority = javaPriority[i];
                String threadName = name + "(" + String.valueOf(priority) + ")";
                threads[i] = new ThreadInstance(threadName,priority);
            }
        }

        public synchronized void execute(List<Runnable> commands, ScanPriority priority) {
            ThreadInstance thread = threads[priority.ordinal()];
            thread.add(commands);
        }

        public synchronized void execute(Runnable command, ScanPriority priority) {
            ThreadInstance thread = threads[priority.ordinal()];
            thread.add(command);
        }
    }
    
    static private class ThreadInstance implements Runnable {
        private Thread thread = null;
        private List<Runnable> runList = new ArrayList<Runnable>();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();

        private ThreadInstance(String name,int priority) {
            thread = new Thread(this,name);
            thread.setPriority(priority);
            thread.start();
        } 
        
        public void run() {
            try {
                while(true) {
                    Runnable runnable = null;
                    lock.lock();
                    try {
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
        private int add(Runnable runnable) {
            int size = 0;
            lock.lock();
            try {
                boolean isEmpty = runList.isEmpty();
                runList.add(runnable);
                size = runList.size();
                if(isEmpty) moreWork.signal();
            } finally {
                lock.unlock();
            }
            return size;
        }
        private int add(List<Runnable> runnableList) {
            int size = 0;
            lock.lock();
            try {
                boolean isEmpty = runList.isEmpty();
                runList.addAll(runnableList);
                size = runList.size();
                if(isEmpty) moreWork.signal();
            } finally {
                lock.unlock();
            }
            return size;
        }
        
    }
}
