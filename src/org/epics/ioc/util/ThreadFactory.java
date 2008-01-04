package org.epics.ioc.util;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class ThreadFactory {
    public static ThreadCreate getThreadCreate() {
        return threadCreate;
    }

    private static ThreadCreateImpl threadCreate = new ThreadCreateImpl();

    private static class ThreadCreateImpl implements ThreadCreate {

        /* (non-Javadoc)
         * @see org.epics.ioc.util.ThreadCreate#create(java.lang.String, int, java.lang.Runnable)
         */
        public Thread create(String name, int priority, ReadyRunnable readyRunnable) {
            RunnableImpl runnableImpl = new RunnableImpl(name,priority,readyRunnable);
            return runnableImpl.getThread();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.ThreadCreate#getThreads()
         */
        public synchronized Thread[] getThreads() {
            int length = threadList.size();
            Thread[] threads = new Thread[length];
            ListIterator<Thread> iter = threadList.listIterator();
            for(int i=0; i<length; i++) {
                threads[i] = iter.next();
            }
            return threads;
        }
        
        private synchronized void addThread(Thread thread) {
            threadList.add(thread);
        }
        
        private synchronized void removeThread(Thread thread) {
            threadList.remove(thread);
        }
        
        private List<Thread> threadList = new LinkedList<Thread>();

        private static class RunnableImpl implements Runnable {

            private RunnableImpl(String name, int priority, ReadyRunnable runnable) {
                this.runnable = runnable;
                thread = new Thread(this,name);
                thread.setPriority(priority);
                thread.start();
                while(!runnable.isReady()) {
                    try {
                        Thread.sleep(1);
                    } catch(InterruptedException e) {}
                }

            }

            private Thread getThread() {
                return thread;
            }

            private Runnable runnable;
            private Thread thread;
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                threadCreate.addThread(thread);
                runnable.run();
                threadCreate.removeThread(thread);
            }
        }
    }
}
