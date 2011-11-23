/**
 * 
 */
package org.epics.rpc;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelAccess;
import org.epics.ca.client.ChannelAccessFactory;
import org.epics.ca.client.ChannelProvider;
import org.epics.ca.client.ChannelRPC;
import org.epics.ca.client.ChannelRPCRequester;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.CreateRequestFactory;
import org.epics.ca.client.Channel.ConnectionState;
import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Status.StatusType;

/**
 * The factory to create a ServiceClient.
 * @author mrk
 *
 */
public class ServiceClientFactory {
	/**
	 * Create a ServiceClient and connect to the service.
	 * @param serviceName The service name. This is the name of a PVRecord with associated support that implements the service.
	 * @param requester The ServiceClientRequester interface implemented by the requester.
	 * @return The ServiceClient interface.
	 */
	public static ServiceClient create(String serviceName,ServiceClientRequester requester) {
		return new ServiceClientImpl(serviceName,requester);
	}
	
	private static final String providerName = "pvAccess";
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private static final ChannelProvider channelProvider = channelAccess.getProvider(providerName);
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status notConnectedStatus = statusCreate.createStatus(StatusType.ERROR, "did not connect", null);
    private static final Status destroyedStatus = statusCreate.createStatus(StatusType.ERROR, "channel destroyed", null);
   
    
	/**
	 * @author mrk
	 *
	 */
	private static class ServiceClientImpl implements ServiceClient,ChannelRequester,ChannelRPCRequester{
		ServiceClientImpl(String serviceName,ServiceClientRequester requester) {
			this.requester = requester;
			channel = channelProvider.createChannel(serviceName, this, ChannelProvider.PRIORITY_DEFAULT);
		}
		
		private enum State{destroyed,connecting,waitConnect,connected,requesting,waitRequest}
		private volatile State state = State.connecting;
		private final ServiceClientRequester requester;
        private Channel channel = null;
        private ChannelRPC channelRPC = null;
        private final PVStructure pvRPCRequest = CreateRequestFactory.createRequest("record[]field(arguments)",this);
		/* (non-Javadoc)
		 * @see org.epics.pvService.client.ServiceClient#destroy()
		 */
		@Override
		public void destroy() {
			Channel channel = null;
			synchronized(this) {
				channel = this.channel;
				state = State.destroyed;
				this.channel = null;
			}
			if(channel!=null) channel.destroy();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvService.client.ServiceClient#connect(double)
		 */
		@Override
		public void waitConnect(double timeout) {
			long timeoutMs = (long)(timeout*1000.0);
			Channel channel = null;
			State state = null;
			synchronized (this) {
				state = this.state;
				if (state==State.connecting) {
					this.state=State.waitConnect;
					try {
						this.wait(timeoutMs);
					} catch (InterruptedException e) {}
					state = this.state;
				}
				if(state==State.connected) return;
				channel = this.channel;
		        this.channel = null;
		        this.state = State.destroyed;
			}
			if(channel!=null) {
				this.channel = null;
				channel.destroy();
			}
			requester.connectResult(destroyedStatus);
		}
		@Override
		public void sendRequest(PVStructure pvArgument) {
			State state = null;
			synchronized(this) {
				state = this.state;
				if(state==State.connected) {
					this.state = State.waitRequest;					
				}
				if(!(state==State.connected)) {
					requester.requestResult(notConnectedStatus, null);
				}
				channelRPC.request(pvArgument,false);
			}
		}
		void requestResult(State state) {
			if(state==State.destroyed) {
				requester.connectResult(destroyedStatus);
				return;
			}
			if(state==State.connecting) {
				requester.requestResult(notConnectedStatus, null);
				return;
			}
			String message = "illegal state " + state.toString();
			Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
			requester.requestResult(status, null);
		}
		/* (non-Javadoc)
		 * @see org.epics.pvService.client.ServiceClient#waitRequest()
		 */
		@Override
		public void waitRequest() {
			boolean notify = false;
			State state = null;
			synchronized (this) {
				state = this.state;
				if (state==State.waitRequest) {
					try {
						this.wait();
					} catch (InterruptedException e) {}
					state = this.state;
				}
				if(state!=State.connected) notify = true;
			}
			if(notify)requestResult(state);
		}
		/* (non-Javadoc)
		 * @see org.epics.ca.client.ChannelRequester#channelCreated(org.epics.pvData.pv.Status, org.epics.ca.client.Channel)
		 */
		@Override
		public void channelCreated(Status status, Channel channel) {
			State oldState = state;
			synchronized(this) {
				this.channel = channel;
				if(status.isSuccess()) return;
				state = State.destroyed;
			}
			requester.connectResult(status);
			synchronized (this) {
				if(oldState==State.waitConnect) this.notifyAll();
			}
			return;
		}
		/* (non-Javadoc)
		 * @see org.epics.ca.client.ChannelRequester#channelStateChange(org.epics.ca.client.Channel, org.epics.ca.client.Channel.ConnectionState)
		 */
		@Override
		public void channelStateChange(Channel c,ConnectionState connectionState) {
			this.channel = c;
			if(connectionState!=ConnectionState.CONNECTED) {
				if(state==State.waitConnect) {
					requester.connectResult(notConnectedStatus);
					synchronized (this) {
						state = State.connecting;
		        		this.notifyAll();
		        	}
				}
				if(state==State.waitRequest) {
					requester.requestResult(notConnectedStatus, null);
					synchronized (this) {
						state = State.connecting;
		        		this.notifyAll();
		        	}
				}
				return;
			}
			channel.createChannelRPC(this, pvRPCRequest);
		}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Requester#getRequesterName()
		 */
		@Override
		public String getRequesterName() {
			return requester.getRequesterName();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Requester#message(java.lang.String, org.epics.pvData.pv.MessageType)
		 */
		@Override
		public void message(String message, MessageType messageType) {
			requester.message(message, messageType);
		}
		@Override
		public void channelRPCConnect(Status status, ChannelRPC channelRPC) {
			if(!status.isOK()) {
				channel.destroy();
				synchronized(this) {
					this.channelRPC = null;
					State oldState = state;
					state = State.destroyed;
					if(oldState==State.waitConnect) this.notifyAll();
					return;
				}
			}
			requester.connectResult(status);
			synchronized (this) {
				this.channelRPC = channelRPC;
				State oldState = state;
				state = State.connected;
				if(oldState==State.waitConnect) this.notifyAll();
			}
		}
		/* (non-Javadoc)
		 * @see org.epics.ca.client.ChannelRPCRequester#requestDone(org.epics.pvData.pv.Status, org.epics.pvData.pv.PVStructure)
		 */
		@Override
		public void requestDone(Status status, PVStructure pvResponse) {
			requester.requestResult(status, pvResponse);
			synchronized(this) {
				if(state==State.waitRequest) {
					state = State.connected;
					this.notifyAll();
				}
			}
		}
	}
}
