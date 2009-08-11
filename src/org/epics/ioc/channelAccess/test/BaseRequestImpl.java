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

package org.epics.ioc.channelAccess.test;

import java.nio.ByteBuffer;

import org.epics.ca.CAException;
import org.epics.ca.CAStatus;
import org.epics.ca.core.DataResponse;
import org.epics.ca.core.QoS;
import org.epics.ca.core.SubscriptionRequest;
import org.epics.ca.core.Transport;
import org.epics.ca.core.impl.client.ChannelImpl;
import org.epics.ca.core.impl.client.ClientContextImpl;
import org.epics.ca.core.impl.client.requests.CancelRequest;
import org.epics.pvData.pv.Requester;

/**
 * Base channel request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
abstract class BaseRequestImpl implements DataResponse, SubscriptionRequest {

	/**
	 * Channel.
	 */
	protected final ChannelImpl channel;

	/**
	 * Context.
	 */
	protected final ClientContextImpl context;

	/**
	 * I/O ID given by the context when registered.
	 */
	protected int ioid = -1;

	/**
	 * Response callback listener.
	 */
	protected final Requester requester;

	/**
	 * Destroyed flag.
	 */
	protected volatile boolean destroyed = false;
	
	/**
	 * Remote instance destroyed.
	 */
	protected volatile boolean remotelyDestroyed = false;

	public BaseRequestImpl(ChannelImpl channel, Requester requester)
	{
		if (requester == null)
			throw new IllegalArgumentException("requester == null");

		this.channel = channel;
		this.context = (ClientContextImpl)channel.getContext();
		
		this.requester = requester;

		// register response request
		ioid = context.registerResponseRequest(this);
		channel.registerResponseRequest(this);
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.ResponseRequest#getIOID()
	 */
	public int getIOID() {
		return ioid;
	}

	abstract void initResponse(Transport transport, byte version, ByteBuffer payloadBuffer, CAStatus status);
	abstract void destroyResponse(Transport transport, byte version, ByteBuffer payloadBuffer, CAStatus status);
	abstract void normalResponse(Transport transport, byte version, ByteBuffer payloadBuffer, byte qos, CAStatus status);
	
	/* (non-Javadoc)
	 * @see org.epics.ca.core.DataResponse#response(org.epics.ca.core.Transport, byte, java.nio.ByteBuffer)
	 */
	public void response(Transport transport, byte version, ByteBuffer payloadBuffer) {
		boolean cancel = false;
		try
		{	
			final byte qos = payloadBuffer.get();
			final CAStatus status = CAStatus.get(payloadBuffer.getShort());

			if (QoS.INIT.isSet(qos))
			{
				initResponse(transport, version, payloadBuffer, status);
			}
			else if (QoS.DESTROY.isSet(qos))
			{
				remotelyDestroyed = true;
				cancel = true;

				destroyResponse(transport, version, payloadBuffer, status);
				// TODO notify if initiated from server??!
			}
			else
			{
				normalResponse(transport, version, payloadBuffer, qos, status);
			}
		}
		finally
		{
			// always cancel request
			if (cancel)
				cancel();
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.ResponseRequest#cancel()
	 */
	public void cancel() {
		destroy();
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.channelAccess.ChannelRequest#destroy()
	 */
	public void destroy() {
		
		synchronized (this) {
			if (destroyed)
				return;
			destroyed = true;
		}

		// unregister response request
		context.unregisterResponseRequest(this);
		channel.unregisterResponseRequest(this);

		// destroy remote instance
		if (!remotelyDestroyed)
		{
			// TODO what if disconnected/unresponsive?
			try {
				CancelRequest.send(channel.checkAndGetTransport(), channel.getServerChannelID(), ioid);
			} catch (CAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.epics.ca.core.ResponseRequest#timeout()
	 */
	public void timeout() {
		cancel();
		// TODO notify?
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.ResponseRequest#reportStatus(org.epics.ca.CAStatus)
	 */
	public void reportStatus(CAStatus status) {
		// destroy, since channel (parent) was destroyed
		if (status == CAStatus.CHANDESTROY)
			destroy();
		// TODO notify?
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.SubscriptionRequest#updateSubscription()
	 */
	@Override
	public void updateSubscription() throws CAException {
		// default is noop
	}
	
}
