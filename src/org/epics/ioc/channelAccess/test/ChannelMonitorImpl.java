/*
 * Copyright (c) 2004 by Cosylab
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
import org.epics.ca.core.QoS;
import org.epics.ca.core.Transport;
import org.epics.ca.core.impl.client.ChannelImpl;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;

/**
 * CA monitor request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class ChannelMonitorImpl extends BaseRequestImpl implements ChannelMonitor {

	/**
	 * PVField factory.
	 */
	private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

	/**
	 * Response callback listener.
	 */
	protected final ChannelMonitorRequester callback;

	protected final PVStructure pvRequest;
	protected final String structureName;
	protected final PVStructure pvOption;
	protected final Executor executor;
	
	protected volatile PVStructure data = null;
	protected volatile BitSet bitSet = null;
	protected volatile BitSet overrunBitSet = null;
	
	public ChannelMonitorImpl(ChannelImpl channel,
			ChannelMonitorRequester callback,
	        PVStructure pvRequest,String structureName,
	        PVStructure pvOption,
	        Executor executor)
	{
		super(channel, callback);
		
		this.callback = callback;
		
		this.pvRequest = pvRequest;
		this.structureName = structureName;

		this.pvOption = pvOption;
		this.executor = executor;

		// subscribe
		try {
			resubscribeSubscription(channel.checkAndGetTransport());
		} catch (CAException e) {		
			requester.message(e.toString(), MessageType.error);
		}
	}

	/**
	 * Send get request message.
	 */
	private void sendRequestMessage(Transport transport, PVField triggerData) 
		throws CAException
	{
    	final ByteBuffer buffer = transport.acquireSendBufferWithHeader((byte)13);
		try {
			// SID
			buffer.putInt(channel.getServerChannelID());
			// IOID
			buffer.putInt(ioid);

			// qos
			final int qos = QoS.INIT.getMaskValue();
			buffer.put((byte)qos);

			// TODO optimize
		    transport.getIntrospectionRegistry().serialize(pvRequest.getField(), buffer);
		    pvRequest.serialize(buffer);

		    // pvOption
		    transport.getIntrospectionRegistry().serialize(pvOption != null ? pvOption.getField() : null, buffer);
		    if (pvOption != null)
		    	pvOption.serialize(buffer);

		    // TODO !!!
		    // trigger data
		    if (triggerData == null)
			    transport.getIntrospectionRegistry().serialize(null, buffer);
		    else {
			    transport.getIntrospectionRegistry().serialize(triggerData.getField(), buffer);
		    	triggerData.serialize(buffer);
		    }
			
			// send and release
			transport.releaseSendBuffer(true);
		} catch (Throwable th) {
			// release buffer on unexpected error
			transport.releaseSendBuffer(false);

			throw new CAException("Failed to send message.", th);
		}
	}

	/**
	 * Send get request message.
	 */
	protected void sendRequestMessage(Transport transport, boolean get, boolean startStopAction, boolean lastRequest)  throws CAException
	{
    	final ByteBuffer buffer = transport.acquireSendBufferWithHeader((byte)13);
		try {
			// SID
			buffer.putInt(channel.getServerChannelID());
			// IOID
			buffer.putInt(ioid);

			// qos
			int qos = (lastRequest ? QoS.DESTROY.getMaskValue() : QoS.DEFAULT.getMaskValue());
			if (get)
				qos |= QoS.GET.getMaskValue();
			if (startStopAction)
				qos |= QoS.PROCESS.getMaskValue();
			buffer.put((byte)qos);

			// send and release
			transport.releaseSendBuffer(true);
		} catch (Throwable th) {
			// release buffer on unexpected error
			transport.releaseSendBuffer(false);
			
			throw new CAException("Failed to send message.", th);
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.impl.client.channelAccess.BaseRequestImpl#destroyResponse(org.epics.ca.core.Transport, byte, java.nio.ByteBuffer, org.epics.ca.CAStatus)
	 */
	@Override
	void destroyResponse(Transport transport, byte version, ByteBuffer payloadBuffer, CAStatus status) {
		// data available
		if (payloadBuffer.hasRemaining())
		{
			normalResponse(transport, version, payloadBuffer, (byte)QoS.DEFAULT.getMaskValue(), status);
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.impl.client.channelAccess.BaseRequestImpl#initResponse(org.epics.ca.core.Transport, byte, java.nio.ByteBuffer, org.epics.ca.CAStatus)
	 */
	@Override
	void initResponse(Transport transport, byte version, ByteBuffer payloadBuffer, CAStatus status) {
		// TODO error handling!!!
		
		// deserialize Field...
		final Field field = transport.getIntrospectionRegistry().deserialize(payloadBuffer);
		// TODO structureName !!!

		// and create bitSet and data of it
		data = (PVStructure)pvDataCreate.createPVField(null, field);
		bitSet = new BitSet(data.getNumberFields());
		
		overrunBitSet = new BitSet(data.getNumberFields());

		// notify
		callback.channelMonitorConnect(this);
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.impl.client.channelAccess.BaseRequestImpl#normalResponse(org.epics.ca.core.Transport, byte, java.nio.ByteBuffer, byte, org.epics.ca.CAStatus)
	 */
	@Override
	void normalResponse(Transport transport, byte version, ByteBuffer payloadBuffer, byte qos, CAStatus status) {
		if (QoS.GET.isSet(qos))
		{
			// TODO not supported by IF yet...
		}
		else
		{
			// deserialize bitSet and data, and overrun bit set
			bitSet.deserialize(payloadBuffer);
			data.deserialize(payloadBuffer, bitSet);

			overrunBitSet.deserialize(payloadBuffer);
			
			callback.monitorEvent(data, bitSet, overrunBitSet);
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.channelAccess.ChannelMonitor#start()
	 */
	@Override
	public void start() {
		try {
			sendRequestMessage(channel.checkAndGetTransport(), false, true, false);
		} catch (CAException e) {		
			requester.message(e.toString(), MessageType.error);
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.channelAccess.ChannelMonitor#stop()
	 */
	@Override
	public void stop() {
		try {
			sendRequestMessage(channel.checkAndGetTransport(), false, true, true);
		} catch (CAException e) {		
			requester.message(e.toString(), MessageType.error);
		}
	}

	/* Called on server restart...
	 * @see org.epics.ca.core.SubscriptionRequest#resubscribeSubscription(org.epics.ca.core.Transport)
	 */
	@Override
	public void resubscribeSubscription(Transport transport) throws CAException {
		// TODO triggerData
		sendRequestMessage(transport, null);
	}

	/* (non-Javadoc)
	 * @see org.epics.ca.core.SubscriptionRequest#updateSubscription()
	 */
	@Override
	public void updateSubscription() throws CAException {
		// get latest value
		try {
			sendRequestMessage(channel.checkAndGetTransport(), true, false, false);
		} catch (CAException e) {		
			requester.message(e.toString(), MessageType.error);
		}
	}
	
}
