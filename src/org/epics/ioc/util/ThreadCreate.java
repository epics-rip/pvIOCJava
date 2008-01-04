/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * ThreadCreate : Create a thread.
 * This provides two features:
 * <ol>
 *    <li>The create does not return until the thread has started.
 *    <li>Auromatially keeps a list of all active threads,
 *    i.e. threads for which the run method has not retured.
 * </ol>
 * @author mrk
 *
 */
public interface ThreadCreate {
    /**
     * Create a new thread.
     * @param name The thread name.
     * @param priority The thread priority.
     * @param readyRunnable An implementation of ReadyRunnable.
     * @return The newly created thread. Create does not return until the thread has started.
     */
    Thread create(String name, int priority, ReadyRunnable readyRunnable);
    /**
     * Get an array of all the active threads.
     * @return The array.
     */
    Thread[] getThreads();
}
