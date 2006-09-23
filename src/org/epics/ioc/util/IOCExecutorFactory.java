/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 * @author mrk
 *
 */
public class IOCExecutorFactory {
    static public IOCExecutor create() {
        String poolSize = System.getenv("IOCExecutorPoolSize");
        if(poolSize==null) poolSize = "1";
        int threadPoolSize = Integer.parseInt(poolSize);
        assert(threadPoolSize>=1);
        return new ExecutorInstance(threadPoolSize);
    }
    
    static private class ThreadPool {
        private ThreadInstance[] threadPool;
        private int[] size;
        private ThreadPool(int nthreads) {
            threadPool = new ThreadInstance[nthreads];
            size = new int[nthreads];
        }
    }
    
    static private class ExecutorInstance implements IOCExecutor {
        private ThreadPool[] threadPool = null;
        private int threadPoolSize;

        private ExecutorInstance(int threadPoolSize) {
            super();
            this.threadPoolSize = threadPoolSize;
            int numPriorities = ScanPriority.values().length;
            threadPool = new ThreadPool[numPriorities];
            for(int i=0; i<numPriorities; i++) {
                threadPool[i] = new ThreadPool(threadPoolSize);
            }
        }

        public synchronized void execute(List<Runnable> commands, ScanPriority priority) {
            ThreadPool threadPool = this.threadPool[priority.ordinal()];
            int size = threadPool.size[0];
            int index = 0;
            for(int next =1; next<threadPoolSize; next++) {
                int sizeNext = threadPool.size[next];
                if(sizeNext<size) {
                    size = sizeNext;
                    index = next;
                }
            }
            size = threadPool.size[index];
            ThreadInstance threadInstance = threadPool.threadPool[index];
            threadPool.size[index] = threadInstance.add(commands);
            
        }

        public synchronized void execute(Runnable command, ScanPriority priority) {
            ThreadPool threadPool = this.threadPool[priority.ordinal()];
            int size = threadPool.size[0];
            int index = 0;
            for(int next =1; next<threadPoolSize; next++) {
                int sizeNext = threadPool.size[next];
                if(sizeNext<size) {
                    size = sizeNext;
                    index = next;
                }
            }
            size = threadPool.size[index];
            ThreadInstance threadInstance = threadPool.threadPool[index];
            threadPool.size[index] = threadInstance.add(command);
        }
    }
    
    static private class ThreadInstance implements Runnable {
        private List<Runnable> runList = new ArrayList<Runnable>();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();

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
