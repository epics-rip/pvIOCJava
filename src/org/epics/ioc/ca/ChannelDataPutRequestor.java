/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requestor;

/**
 * @author mrk
 *
 */
public interface ChannelDataPutRequestor extends Requestor{
    /**
     * 
     */
    void getDone();
    void putDone(RequestResult requestResult);
}
