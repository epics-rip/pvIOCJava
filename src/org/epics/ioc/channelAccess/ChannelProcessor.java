/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.property.TimeStamp;
/**
 * Interface for requesting the channel be processed.
 * @author mrk
 *
 */
public interface ChannelProcessor {
    /**
     * Detach from being record processor.
     */
    void detach();
    /**
     * Request to process. ChannelProcessorRequester.becomeProcessor is called when the requester can call setActive and/or process.
     */
    void requestProcess();
    /**
     * Set the record active.
     * This is called if the requester wants to put to the record
     * before the record starts processing.
     * If this is successful than the requester must call process after
     * puts are complete.
     * @return (false,true) if the record is active.
     * A value of false means that the request failed.
     * In this case Requester.message is called.
     */
    boolean setActive();
    /**
     * Process the record.
     * A value of false means that the request failed.
     * If the request failed Requester .message is called.
     * @param leaveActive Should the record be left active after
     * process is complete? This is true if the requester wants to read
     * data from the record after processing.
     * @param timeStamp The timeStamp to be assigned to the record.
     * This can be null.
     * @return (false,true) if the record is processing.
     * A value of false means that the request failed.
     * In this case Requester.message is called.
     */
    boolean process(boolean leaveActive,TimeStamp timeStamp);
    /**
     * Called if process requested that the record be left active.
     */
    void setInactive();
}
