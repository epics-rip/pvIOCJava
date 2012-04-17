/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.install;

import org.epics.pvdata.misc.ThreadPriority;

/**
 * AfterStart is a facility that allows support code to be called after all records in a database have started.
 * Code that wants to be called calls method requestCallback.
 * The AfterStartRequest should not block but arrange for some other thread to make any blocking calls.
 * When processing is finished method done ust be called.
 * @author mrk
 *
 */
public interface AfterStart {
    /**
     * Request to be called back after all records have started.
     * @param node The AfterStartNode which must be created by calling AfterStartFactory.createNode().
     * @param afterMerge Should requester be called before or after the beingInstalled database is
     * merged into the master database.
     * @param priority The priority. This is NOT used for specifying a thread but for specifying a queue.
     * The callback queues are executed in order of priority.
     * All callbacks in a queue must call done before the next lower priority queue is executed.
     */
    void requestCallback(AfterStartNode node,boolean afterMerge,ThreadPriority priority);
    /**
     * Call the priority requesters. This is called by InstallFactory when all records in the database have been started.
     */
    void callRequesters(boolean afterMerge);
    /**
     * Called by the AfterStartRequester when it is done.
     * @param node The AfterStartNode.
     */
    void done(AfterStartNode node);
    /**
     * Called by the requester when it is done with a request and request a new callback. 
     * @param node The AfterStartNode which must be created by calling AfterStartFactory.createNode().
     * @param afterMerge Should requester be called before or after the beingInstalled database is
     * merged into the master database.
     * @param priority The priority.
     */
    void doneAndRequest(AfterStartNode node,boolean afterMerge,ThreadPriority priority);
}
