/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.util.Requester;

/**
 * <p>The interface that must be implemented by code that makes a recordProcess.requestProcessCallback
 * request, which results in method processCallback being called.
 * processCallback is called with the record unlocked.
 * It is called just before recordSupport.process or recordSupport.processContinue returns.
 * </p>
 * <p>Code that wants to process other records can only request processing of the other records via this method.
 * It is NOT permissible to make direct process requests because of "deadly embrace" race conditions,
 * because RecordProcess locks a record instance before calling recordSupport.process.</p>
 * <p>Code that implements asynchronous support must implement this method.
 * The method itself must not block so it must use another thread to implement asynchronous operations.</p>
 * @author mrk
 *
 */
public interface ProcessCallbackRequester extends Requester{
    /**
     * Called by RecordProcess as a result of a call to RecordProcess.requestProcessCallback().
     * processCallback is called with the record unlocked but still active.
     */
    void processCallback();
}
