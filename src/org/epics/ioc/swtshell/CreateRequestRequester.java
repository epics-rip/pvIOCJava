/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.epics.ca.client.ChannelRequester;
import org.epics.pvData.pv.PVStructure;

/**
 * Requester for a CreateRequest.
 * @author mrk
 *
 */
public interface CreateRequestRequester extends ChannelRequester
{
    /**
     * The request has been created.
     * @param pvRequest The request structure.
     * @param isShared Should the request be a shared request?
     */
    void request(PVStructure pvRequest,boolean isShared);
}
