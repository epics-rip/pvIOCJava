/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelRPCRequester;
import org.epics.ioc.database.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;

/**
 * RPC Server.
 * The implementation must implement a method
 *      public static RPCServer create();
 * @author mrk
 *
 */
public interface RPCServer {
	/**
	 * All done.
	 */
	void destroy();
    /**
     * Initialize the service.
     * @param channel The channel that is requesting the service.
     * @param pvRecord The record that is being serviced.
     * @param channelRPCRequester The client that is requesting the service.
     * @param pvRequest The client's request structure.
     * @return The status.
     * The server does NOT call channelRPCRequester.channelRPCConnect.
     */
    Status initialize(Channel channel,PVRecord pvRecord,ChannelRPCRequester channelRPCRequester,PVStructure pvRequest);
    /**
     * A new request. The pvRecord contains the clients request data. The server MUST call channelRPCRequester.requestDone.
     * @param pvArgument the argument passed byn the client.
     */
    void request(PVStructure pvArgument);
}
