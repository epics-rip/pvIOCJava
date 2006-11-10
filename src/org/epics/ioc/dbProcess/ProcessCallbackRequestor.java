/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.*;

/**
 * The interface that must be implemented by code that makes a recordProcess.requestProcessCallback
 * request.
 * This is called with the record unlocked.
 * This is a request to be called back just before recordSupport.process or recordSupport.processContinue returns.
 * Code that wants to process other records as a result of recordProcess.process
 * can only request processing of the other records via this method.
 * This method MUST only be called by support from either it's process or processContinue method.
 * It is NOT permissible to make direct process requests because of "deadly embrace" race conditions,
 * because RecordProcess locks a record instance before calling recordSupport.process. 
 * @author mrk
 *
 */
public interface ProcessCallbackRequestor extends Requestor{
    /**
     * The callback to call after recordSupport.process returns.
     * The callback is called with the record unlocked but still active.
     * The callback can request that other records be processed.
     */
    void processCallback();
}
