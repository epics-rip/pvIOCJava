/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;

/**
 * The methods implemented by the requester for a CDPut.
 * @author mrk
 *
 */
public interface CDPutRequester extends Requester{
    /**
     * The get request has completed.
     * The data resides in the CD.
     * @param requestResult The result of the get request.
     */
    void getDone(RequestResult requestResult);
    /**
     * The put request is done.
     * @param requestResult The result of the put request.
     */
    void putDone(RequestResult requestResult);
}
