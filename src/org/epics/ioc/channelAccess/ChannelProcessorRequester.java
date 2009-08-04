/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;
import org.epics.pvData.pv.Requester;

/**
 * An interface implemented by a channel process requester.
 * @author mrk
 *
 */
public interface ChannelProcessorRequester extends Requester{
    /**
     * Called as a result of calling ChannelProcessor.requestProcess.
     */
    void becomeProcessor();
    /**
     * The result of the process request.
     * This is called with the record still active and locked.
     * The requester can read data from the record.
     * @param success The result of the process request.
     * If false than Requester.message was called to report the error.
     */
    void recordProcessResult(boolean success);
    /**
     * Called to signify process completion.
     * This is called with the record unlocked.
     */
    void recordProcessComplete();
}
