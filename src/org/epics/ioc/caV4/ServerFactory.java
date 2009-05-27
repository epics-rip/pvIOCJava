/*
 * Copyright (c) 2007 by Cosylab
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

import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.epics.ca.CAException;
import org.epics.ca.CAStatus;
import org.epics.ca.CAStatusException;
import org.epics.ca.PropertyListType;
import org.epics.ca.client.Channel.MonitorTrigger;
import org.epics.ca.core.impl.server.ServerContextImpl;
import org.epics.ca.core.impl.server.plugins.DefaultBeaconServerDataProvider;
import org.epics.ca.server.ProcessVariable;
import org.epics.ca.server.ProcessVariableAttachCallback;
import org.epics.ca.server.ProcessVariableExistanceCallback;
import org.epics.ca.server.ProcessVariableExistanceCompletion;
import org.epics.ca.server.ProcessVariableReadCallback;
import org.epics.ca.server.ProcessVariableValueCallback;
import org.epics.ca.server.ProcessVariableWriteCallback;
import org.epics.ca.server.Server;
import org.epics.ca.server.plugins.IntrospectionSearchProvider;
import org.epics.ca.util.WildcharMatcher;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelAccess;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelListener;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadReady;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

public class ServerFactory {
    /**
     * This starts the Channel Access Server.
     */
    public static void start() {
        new ThreadInstance();
    }
    
    private static String CHANNEL_PROVIDER_NAME = "local";

    private static final Convert convert = ConvertFactory.getConvert();
    private static final ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    
    private static class JavaIOCIntrospectionSearchProvider implements IntrospectionSearchProvider
    {
    	/**
    	 * Server context to query.
    	 */
    	protected ServerContextImpl context;
    	
    	/**
    	 * Constructor.
    	 * @param context server context to be monitored.
    	 */
    	public JavaIOCIntrospectionSearchProvider(ServerContextImpl context) {
    		this.context = context;
    	}

		/* (non-Javadoc)
		 * @see org.epics.ca.server.plugins.IntrospectionSearchProvider#introspectionSearch(org.epics.ca.server.ProcessVariableReadCallback, org.epics.pvData.pv.PVField)
		 */
		public void introspectionSearch(ProcessVariableReadCallback callback, PVField searchData) throws CAException
		{
			// data check
			if (!(searchData instanceof PVString))
				throw new CAStatusException(CAStatus.BADTYPE);
			
			final String name = ((PVString)searchData).get();
			
			// TODO values (tags) are nor supported
			ArrayList<String> result = new ArrayList<String>();
			String[] recordNames = PVDatabaseFactory.getMaster().getRecordNames();
			for (String recordName : recordNames)
				if (WildcharMatcher.match(name, recordName))
					result.add(recordName);
			
			Array field = FieldFactory.getFieldCreate().createArray("value", ScalarType.pvString);
			PVStringArray pvStringArray = (PVStringArray)PVDataFactory.getPVDataCreate().createPVArray(null, field);
			pvStringArray.put(0, result.size(), result.toArray(new String[result.size()]), 0);
			
			callback.processVariableReadCompleted(pvStringArray, CAStatus.NORMAL);
		}
    	
    }
    
    private static class ThreadInstance implements RunnableReady {

        private ThreadInstance() {
            threadCreate.create("caV4Server", 3, this);
        }
        
    	/**
         * JCA server context.
         */
        private ServerContextImpl context = null;
        
        /**
         * Initialize JCA context.
         * @throws CAException	throws on any failure.
         */
        private void initialize() throws CAException {
            
    		// Create server implmentation
            CAServerImpl server = new CAServerImpl();
    		
    		// Create a context with default configuration values.
    		context = new ServerContextImpl();
    		context.setBeaconServerStatusProvider(new DefaultBeaconServerDataProvider(context));
    		context.setIntrospectionSearachProvider(new JavaIOCIntrospectionSearchProvider(context));
    		
    		context.initialize(server);

    		// Display basic information about the context.
            System.out.println(context.getVersion().getVersionString());
            context.printInfo(); System.out.println();
        }

        /**
         * Destroy JCA server  context.
         */
        private void destroy() {
            
            try {

                // Destroy the context, check if never initialized.
                if (context != null)
                    context.destroy();
                
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            try {
                // initialize context
                initialize();
                threadReady.ready();
                System.out.println("Running server...");
                // run server 
                context.run(0);
                System.out.println("Done.");
            } catch (Throwable th) {
                th.printStackTrace();
            }
            finally {
                // always finalize
                destroy();
            }
        }
    }
    
    private static class CAServerImpl implements Server {

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.Server#processVariableAttach(java.lang.String, gov.aps.jca.cas.ProcessVariableAttachCallback)
         */
        public ProcessVariable processVariableAttach(String aliasName,
                ProcessVariableAttachCallback asyncCompletionCallback)
                throws CAStatusException, IllegalArgumentException,
                IllegalStateException {
            return new ChannelProcessVariable(aliasName);
        }

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.Server#processVariableExistanceTest(java.lang.String, java.net.InetSocketAddress, gov.aps.jca.cas.ProcessVariableExistanceCallback)
         */
        public ProcessVariableExistanceCompletion processVariableExistanceTest(
                String aliasName, InetSocketAddress clientAddress,
                ProcessVariableExistanceCallback asyncCompletionCallback)
        throws CAException, IllegalArgumentException, IllegalStateException {
            boolean exists = channelAccess.isChannelProvider(aliasName, CHANNEL_PROVIDER_NAME);
            return exists ? ProcessVariableExistanceCompletion.EXISTS_HERE : ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
        }
    }
    
    /**
     * Channel process variable implementation. 
     */
    static class ChannelProcessVariable extends ProcessVariable implements ChannelListener
    {
        private static final String[] DESIRED_PROPERTIES = new String[] {
            "timeStamp","alarm","display","control"
        };

        private final Channel channel;
        private ChannelField channelField;
        
        /**
         * Channel PV constructor.
         * @param pvName channelName.
         * @param eventCallback event callback, can be <code>null</code>.
         */
        public ChannelProcessVariable(String pvName)
            throws CAStatusException, IllegalArgumentException, IllegalStateException
        {
            super(pvName);

            channel = channelAccess.createChannel(pvName, DESIRED_PROPERTIES, CHANNEL_PROVIDER_NAME, this);
            if (channel == null)
                throw new CAStatusException(CAStatus.DEFUNCT);
            channel.connect();
            
            initialize();
        }
        
        /**
         * Internal initialize.
         * @throws CAException
         */
        private void initialize() throws CAStatusException
        {
        	// get field name (passed on channel creation)
        	// can be null, which means entire record
        	String fieldName = channel.getFieldName();
        	channelField = channel.createChannelField(fieldName); 
        	if (channelField == null)
	        	 throw new CAStatusException(CAStatus.DEFUNCT, "Failed to find field " + fieldName);
        }
        
        /* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#getField()
		 */
		@Override
		public Field getField() {
			// we report only field and not entire record, if specified
			return channelField.getField();
		}

		/* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#destroy()
         */
        @Override
        public void destroy() {
            super.destroy();
            channel.destroy();
        }

		private static PVDataCreate pvDataFactory = PVDataFactory.getPVDataCreate();
		static FieldCreate fieldFactory = FieldFactory.getFieldCreate();
	    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();
	    
	    /* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#read(org.epics.ca.server.ProcessVariableReadCallback, org.epics.ca.PropertyListType, java.lang.String[])
		 */
		@Override
		public void read(ProcessVariableReadCallback asyncReadCallback,
						 PropertyListType propertyListType, String[] propertyList) throws CAException {
			
			final PVField thisPVField = channelField.getPVField();
			final PVRecord thisRecord = channel.getPVRecord();
			
			final PVField data;
			// "this" field
			if (propertyListType == PropertyListType.ALL ||
				(propertyListType == PropertyListType.VALUE && channel.getPrimaryFieldName().equals(channel.getFieldName())))
			{
				// get all
				data = thisPVField;
			}
			// structure
			else if (channel.getFieldName() == null)
			{
				DynamicSubsetOfPVStructure dynamicStructure = new DynamicSubsetOfPVStructure(thisRecord);
				// subfields
				for (String propertyName : propertyList) {
					PVField pvField = thisRecord.getSubField(propertyName);
					if (pvField != null)
						dynamicStructure.appendPVField(pvField);
				}
				data = dynamicStructure;
			}
			// record.value properties
			else
			{
				DynamicSubsetOfPVStructure dynamicStructure = new DynamicSubsetOfPVStructure(thisRecord);
				// properties
				for (String propertyName : propertyList) {
					PVField pvField = pvProperty.findProperty(thisPVField, propertyName);
					if (pvField != null)
						dynamicStructure.appendPVField(pvField);
				}
				data = dynamicStructure;
			}
			
            // TODO temp (no processing is done)
            final PVRecord record = channel.getPVRecord();
            record.lock();
            try
            {
            	// this method never blocks...
            	asyncReadCallback.processVariableReadCompleted(data, CAStatus.NORMAL);
            } finally {
            	record.unlock();
            }
		}

		/* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#write(org.epics.pvData.pv.PVField, int, org.epics.ca.server.ProcessVariableWriteCallback)
		 */
		@Override
		public CAStatus write(PVField value, int offset, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
			
			PVField targetField = channelField.getPVField();
			final String valueFieldName = value.getField().getFieldName(); 
			if (valueFieldName != null &&
				value.getField().getType() != Type.structure &&
				targetField.getField().getType() == Type.structure)
			{
				// special case: only field is being sent and target is structure, find target field
				targetField = ((PVStructure)targetField).getSubField(valueFieldName);
				if (targetField == null)
					// TODO find better status
					return CAStatus.BADTYPE;
			}
			
			// TODO is this always necessary?!!!
			final boolean groupPut = (value.getField().getType() == Type.structure);
			
            // TODO temp (no processing is done)
            final PVRecord record = channel.getPVRecord();
            record.lock();
            try
            {
                if (groupPut) record.beginGroupPut();
                try
                {
	                // no convert...
	    			PVDataUtils.copyValue(value, targetField, 0, offset, -1);
                } finally {
        			if (groupPut) record.endGroupPut();
                }

            } finally {
            	record.unlock();
            }
            
            return CAStatus.NORMAL;
		}
		
		
		/* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#process(org.epics.ca.server.ProcessVariableWriteCallback)
		 */
		@Override
		public CAStatus process(ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
			// TODO 
			return CAStatus.NOSUPPORT;
		}


		interface MonitorCondition {
			boolean conditionCheck(PVField field);
		}
		
		static final class OnPutCondition implements MonitorCondition {
			public boolean conditionCheck(PVField field) {
				return true;
			}
		}
		
		static final class OnChangeCondition implements MonitorCondition {
			private final PVField monitoredField;
			
			private PVField lastValue;
			
			public OnChangeCondition(PVField monitoredField) {
				this.monitoredField = monitoredField;
				lastValue = pvDataFactory.createPVField(monitoredField.getParent(), monitoredField.getField());
				try {
					PVDataUtils.copyValue(monitoredField, lastValue, 0, 0, -1);
				} catch (CAException e) {
					throw new RuntimeException("unexpected exception occured", e);
				}
			}

			public boolean conditionCheck(PVField field) {
				if (field != monitoredField || field.equals(lastValue))
					return false;

				try {
					// TODO structure change?!!
					PVDataUtils.copyValue(field, lastValue, 0, 0, -1);
				} catch (CAException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
		}

		static final class OnDiffChangeCondition implements MonitorCondition {
			private final PVField monitoredField;
			private final double deadband;
			private final boolean absolute;
			
			private double lastValue;
			
			public OnDiffChangeCondition(PVField monitoredField, PVField data, boolean relative) {
				this.monitoredField = monitoredField;
				if (!(monitoredField instanceof PVScalar) || !((Scalar)monitoredField.getField()).getScalarType().isNumeric())
					throw new IllegalArgumentException("monitored field is not a numeric primitive");
				if (!(data instanceof PVScalar))
					throw new IllegalArgumentException("monitored field is not a numeric primitive");
				
				this.deadband = convert.toDouble((PVScalar)data);
				this.absolute = !relative;
				lastValue = convert.toDouble((PVScalar)monitoredField);
			}

			public boolean conditionCheck(PVField field) {
				if (field != monitoredField)
					return false;
				
				final double value = convert.toDouble((PVScalar)field);
				final double diff = Math.abs(lastValue - value); 
				if (absolute)
				{
					if (diff < deadband)
						return false;

					lastValue = value;
					return true;
				}
				
				// relative
                if(lastValue != 0.0) {
                    if ((diff/Math.abs(lastValue)) < deadband)
                    	return false;
                }
				
				lastValue = value;
				return true;
			}
		}

    	static class PVMonitorImpl implements PVListener
    	{
    		private boolean group = false;
    		private DynamicSubsetOfPVStructure newData;
    		private final MonitorCondition condition;
    		private boolean conditionMet = false;
    		private final PVStructure supersetStructure;
    		
    		private final ProcessVariableValueCallback callback;
    		private final boolean copyData;
    		private final int offset;
    		private final int count;
    		
    		public PVMonitorImpl(PVStructure supersetStructure, ProcessVariableValueCallback callback, 
    				boolean copyData, int offset, int count,
    				MonitorCondition condition)
    		{
    			this.supersetStructure = supersetStructure;
    			this.callback = callback;
    			this.copyData = copyData;
    			this.offset = offset;
    			this.count = count;
    			this.condition = condition;
    			
    			newData = new DynamicSubsetOfPVStructure(supersetStructure);
    		}
    		
			/**
			 * Post data via callback.
			 */
			private final void postData() {
				// no condition met, skip this change
				if (!conditionMet)
					return;
				
				final boolean consumed = callback.postData(newData);
				if (consumed) {
					if (copyData)
						newData = new DynamicSubsetOfPVStructure(supersetStructure);
					else
						newData.clear();
					conditionMet = false;
				}
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
			 */
			public void beginGroupPut(PVRecord pvRecord) {
				group = true;
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
			 */
			public void endGroupPut(PVRecord pvRecord) {
				postData();
				group = false;
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVField)
			 */
			public void dataPut(PVField pvField) {
				if (copyData) {
					// we create copy here... where it is all nicely locked :)
					final PVField pvFieldCopy = pvDataFactory.createPVField(pvField.getParent(), pvField.getField());
					try {
						PVDataUtils.copyValue(pvField, pvFieldCopy, offset, 0, count);
						newData.appendPVField(pvFieldCopy);
					} catch (CAException e) {
						// TODO
						e.printStackTrace();
					}
				}
				else
					newData.appendPVField(pvField);

				// check condition
				conditionMet |= condition.conditionCheck(pvField);	// put case
				
				if (!group)
					postData();
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
			 */
			public void dataPut(PVStructure requested, PVField pvField) {
				dataPut(pvField);
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
			 */
			public void unlisten(PVRecord pvRecord) {
System.err.println("unlisten:" + pvRecord.getFullName());
// TODO check
//callback.canceled();
			}
    		
    	};

    	private static final MonitorCondition onPutCondition = new OnPutCondition();

    	private PVField getMonitoredField(PropertyListType propertyListType, String[] propertyList)
    		throws CAException
    	{
    		if (propertyListType == PropertyListType.ALL)
    			return channelField.getPVField();
    		
    		// take first field as interest
    		final String fieldName = propertyList[0];
    		PVField pvField;
			if (channel.getFieldName() == null)
				pvField = channel.getPVRecord().getSubField(fieldName);
			else
				pvField = pvProperty.findProperty(channelField.getPVField(), fieldName);
 		
    		if (pvField == null)
    			// TODO better exception
    			throw new CAStatusException(CAStatus.DEFUNCT);
    		
    		return pvField;
    	}
    	
		/* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#createMonitor(org.epics.ca.server.ProcessVariableValueCallback, boolean, int, int, org.epics.ca.client.Channel.MonitorTrigger, org.epics.pvData.pv.PVField, org.epics.ca.PropertyListType, java.lang.String[])
		 */
		@Override
		public Object createMonitor(ProcessVariableValueCallback callback, boolean copyData, int offset, int count,
				MonitorTrigger monitorTrigger, PVField monitorTriggerData,
				PropertyListType propertyListType, String[] propertyList)
			throws CAException {

        	final PVField thisPVField = channelField.getPVField();
			final PVRecord record = channel.getPVRecord();
	
			/*
			// whole record
			if (propertyListType == PropertyListType.ALL && channel.getFieldName() == null)
				monitorTrigger = monitorTrigger.ON_PUT;
			*/
			
            record.lock();
            try
            {
            	final MonitorCondition condition;
            	switch (monitorTrigger)
            	{
	            	case ON_PUT:
	            		condition = onPutCondition;
	            		break;
            		case ON_CHANGE:
            			condition = new OnChangeCondition(getMonitoredField(propertyListType, propertyList));
            			break;
            		case ON_ABSOLUTE_CHANGE:
            		case ON_RELATIVE_CHANGE:
            			condition = new OnDiffChangeCondition(getMonitoredField(propertyListType, propertyList),
            						monitorTriggerData, monitorTrigger == MonitorTrigger.ON_RELATIVE_CHANGE);
            			break;
            		case ON_EVENT_ONLY:
            		default:
            			throw new CAStatusException(CAStatus.NOSUPPORT);
            	}

            	final PVMonitorImpl listener = new PVMonitorImpl(record, callback, copyData, offset, count, condition);
	        	record.registerListener(listener);
	
				final PVField data;
				// "this" field
				if (propertyListType == PropertyListType.ALL ||
					(propertyListType == PropertyListType.VALUE && channel.getPrimaryFieldName().equals(channel.getFieldName())))
				{
					// get all
					data = thisPVField;
					record.addListener(listener);
				}
				// whole record
				else if (channel.getFieldName() == null)
				{
					DynamicSubsetOfPVStructure dynamicStructure = new DynamicSubsetOfPVStructure(record);
					// subfields
					for (String propertyName : propertyList) {
						PVField pvField = record.getSubField(propertyName);
						if (pvField != null) {
							dynamicStructure.appendPVField(pvField);
							record.addListener(listener);
						}
					}
					data = dynamicStructure;
				}
				// record.value properties
				else
				{
					DynamicSubsetOfPVStructure dynamicStructure = new DynamicSubsetOfPVStructure(record);
					// properties
					for (String propertyName : propertyList) {
						PVField pvField = pvProperty.findProperty(thisPVField, propertyName);
						if (pvField != null) {
							dynamicStructure.appendPVField(pvField);
							record.addListener(listener);
						}
					}
					data = dynamicStructure;
				}

            	// TODO temp (no processing is done)
            	// this method never blocks...
				callback.postData(data);

				return listener;
            } finally {
            	record.unlock();
            }
		}


		/* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#destroyMonitor(java.lang.Object)
		 */
		@Override
		public void destroyMonitor(Object id) {
            final PVRecord record = channel.getPVRecord();
            record.lock();
            try
            {
            	record.unregisterListener((PVListener)id);
            } finally {
            	record.unlock();
            }
		}

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return name;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.err.println("Message received [" + messageType + "] : " + message);
            //Thread.dumpStack();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            // TODO
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            // TODO
        }
        
    }
}
