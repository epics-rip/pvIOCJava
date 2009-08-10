/*
 * Copyright (c) 2009 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.ioc.caV4;

import org.epics.ca.CAConstants;
import org.epics.ca.CAException;
import org.epics.ca.client.Channel;
import org.epics.ca.client.ConnectionEvent;
import org.epics.ca.client.EventListener;
import org.epics.ca.core.impl.client.ClientContextImpl;
import org.epics.ioc.channelAccess.ChannelAccessFactory;
import org.epics.pvData.channelAccess.ChannelFind;
import org.epics.pvData.channelAccess.ChannelFindRequester;
import org.epics.pvData.channelAccess.ChannelProvider;
import org.epics.pvData.channelAccess.ChannelRequester;

public class ClientFactory {
	
	private static class ChannelProviderImpl implements ChannelProvider
	{
		final ClientContextImpl context;
		
		public ChannelProviderImpl() throws CAException
		{
			context = new ClientContextImpl();
			context.initialize();
		}

		@Override
		public ChannelFind channelFind(String channelName,
				ChannelFindRequester channelFindRequester) {
			// TODO there is no actual support for find right now in CA... can be done via create
			throw new RuntimeException("not implemented");
		}

		@Override
		public void createChannel(String channelName,
				final ChannelRequester channelRequester) {

			EventListener<ConnectionEvent> cl = new EventListener<ConnectionEvent>()
		    {
		 		public void onEvent(ConnectionEvent connectionEvent) {
		 			channelRequester.channelStateChange(connectionEvent.getChannel(), connectionEvent.isConnected());
				}
		    };

		    Channel channel;
			try {
				channel = context.createChannel(channelName, cl, CAConstants.CA_DEFAULT_PRIORITY);
			} catch (Throwable th) {
				// TODO error handling missing in IF
				th.printStackTrace();
				channelRequester.channelNotCreated();
				return;
			}
		    channelRequester.channelCreated(channel);
		}

		@Override
		public void destroy() {
			context.dispose();
		}

		@Override
		public String getProviderName() {
			// TODO constant
			return "CAv4";
		}
		
	}
	
    /**
     * This initializes the Channel Access client.
     */
    public static void init() {
    	try {
			ChannelAccessFactory.registerChannelProvider(new ChannelProviderImpl());
		} catch (CAException e) {
			throw new RuntimeException("Failed to initializa client channel access.", e);
		}
    }
}