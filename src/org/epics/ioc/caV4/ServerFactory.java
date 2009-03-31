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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.epics.ca.CAException;
import org.epics.ca.CAStatus;
import org.epics.ca.CAStatusException;
import org.epics.ca.PropertyListType;
import org.epics.ca.core.impl.server.ServerContextImpl;
import org.epics.ca.core.impl.server.plugins.DefaultBeaconServerDataProvider;
import org.epics.ca.server.ProcessVariable;
import org.epics.ca.server.ProcessVariableAttachCallback;
import org.epics.ca.server.ProcessVariableEventCallback;
import org.epics.ca.server.ProcessVariableExistanceCallback;
import org.epics.ca.server.ProcessVariableExistanceCompletion;
import org.epics.ca.server.ProcessVariableReadCallback;
import org.epics.ca.server.ProcessVariableWriteCallback;
import org.epics.ca.server.Server;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDMonitor;
import org.epics.ioc.ca.CDMonitorFactory;
import org.epics.ioc.ca.CDMonitorRequester;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelAccess;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.AbstractPVField;
import org.epics.pvData.factory.BaseField;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.misc.ThreadReady;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.BooleanArrayData;
import org.epics.pvData.pv.ByteArrayData;
import org.epics.pvData.pv.DoubleArrayData;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.FloatArrayData;
import org.epics.pvData.pv.IntArrayData;
import org.epics.pvData.pv.LongArrayData;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVBooleanArray;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVByteArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVFloatArray;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVIntArray;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVLongArray;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVShortArray;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.ShortArrayData;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

public class ServerFactory {
    /**
     * This starts the Channel Access Server.
     */
    public static void start() {
        new ThreadInstance();
    }
    
    private static String CHANNEL_PROVIDER_NAME = "local";
    
    private static Executor executor = ExecutorFactory.create("caV4Monitor", ThreadPriority.low);
    private static final ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    
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
         * @see gov.aps.jca.cas.Server#processVariableAttach(java.lang.String, gov.aps.jca.cas.ProcessVariableEventCallback, gov.aps.jca.cas.ProcessVariableAttachCallback)
         */
        public ProcessVariable processVariableAttach(String aliasName,
                ProcessVariableEventCallback eventCallback,
                ProcessVariableAttachCallback asyncCompletionCallback)
                throws CAStatusException, IllegalArgumentException,
                IllegalStateException {
            return new ChannelProcessVariable(aliasName, eventCallback);
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
    private static class ChannelProcessVariable extends ProcessVariable implements ChannelListener
    {
        private static final String[] DESIRED_PROPERTIES = new String[] {
            "timeStamp","alarm","display","control"
        };

        private final Channel channel;
        private ChannelField channelField;
        private GetRequest getRequest = null;
        private PutRequest putRequest = null;
        private MonitorRequest monitorRequest = null;;
        
        /**
         * Channel PV constructor.
         * @param pvName channelName.
         * @param eventCallback event callback, can be <code>null</code>.
         */
        public ChannelProcessVariable(String pvName, ProcessVariableEventCallback eventCallback)
            throws CAStatusException, IllegalArgumentException, IllegalStateException
        {
            super(pvName, eventCallback);

            channel = channelAccess.createChannel(pvName, DESIRED_PROPERTIES, CHANNEL_PROVIDER_NAME, this);
            if (channel == null)
                throw new CAStatusException(CAStatus.DEFUNCT);
            channel.connect();
            this.eventCallback = eventCallback;
            
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
		private static FieldCreate fieldFactory = FieldFactory.getFieldCreate();
	    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();

	    class DynamicSubsetOfPVStructure extends AbstractPVField implements PVStructure
	    {
		    class DynamicSubsetOfStructure extends BaseField implements Structure
		    {
		    	private Field[] cachedFields;
		    	
		    	public DynamicSubsetOfStructure() {
		    		super(null, Type.structure);
		    	}

				/* (non-Javadoc)
				 * @see org.epics.pvData.pv.Structure#getField(java.lang.String)
				 */
				public Field getField(String fieldName) {
					return pvFieldsMap.get(fieldName).getField();
				}

				/* (non-Javadoc)
				 * @see org.epics.pvData.pv.Structure#getFieldIndex(java.lang.String)
				 */
				public int getFieldIndex(String fieldName) {
					throw new UnsupportedOperationException("dynamic type");
				}

				/* (non-Javadoc)
				 * @see org.epics.pvData.pv.Structure#getFieldNames()
				 */
				public String[] getFieldNames() {
					return pvFieldsMap.keySet().toArray(new String[pvFieldsMap.size()]);
				}

				/* (non-Javadoc)
				 * @see org.epics.pvData.pv.Structure#getFields()
				 */
				public Field[] getFields() {
					if (cachedFields == null) {
						cachedFields = new Field[pvFieldsMap.size()];
						final Collection<PVField> pvFields = pvFieldsMap.values(); 
						int i = 0;
						for (PVField pvField : pvFields)
							cachedFields[i++] = pvField.getField();
					}
					return cachedFields;
				}
				
				public void changed() {
					cachedFields = null;
				}
				
			    /* (non-Javadoc)
			     * @see org.epics.pvData.factory.BaseField#toString()
			     */
			    public String toString() { return getString(0);}
			    /* (non-Javadoc)
			     * @see org.epics.pvData.factory.BaseField#toString(int)
			     */
			    public String toString(int indentLevel) {
			        return getString(indentLevel);
			    }

			    private String getString(int indentLevel) {
			        StringBuilder builder = new StringBuilder();
			        builder.append(super.toString(indentLevel));
			        convert.newLine(builder,indentLevel);
			        builder.append(String.format("structure  {"));
			        final Field[] fields = getFields();
			        for(int i=0, n= fields.length; i < n; i++) {
			            builder.append(fields[i].toString(indentLevel + 1));
			        }
			        convert.newLine(builder,indentLevel);
			        builder.append("}");
			        return builder.toString();
			    }

			    /* (non-Javadoc)
				 * @see java.lang.Object#hashCode()
				 */
				@Override
				public int hashCode() {
					final int PRIME = 31;
					int result = super.hashCode();
					result = PRIME * result + Arrays.hashCode(getFields());
					return result;
				}
				
				/* (non-Javadoc)
				 * @see java.lang.Object#equals(java.lang.Object)
				 */
				@Override
				public boolean equals(Object obj) {
					if (this == obj)
						return true;
					if (!super.equals(obj))
						return false;
					if (getClass() != obj.getClass())
						return false;
					final DynamicSubsetOfStructure other = (DynamicSubsetOfStructure) obj;
					if (!Arrays.equals(getFields(), other.getFields()))
						return false;
					return true;
				}
				
		    	
		    }

		    private PVStructure supersetStructure;
		    private LinkedHashMap<String, PVField> pvFieldsMap = new LinkedHashMap<String, PVField>();
		    private PVField[] cachedPVFieldsArray = null;
	    	
	    	public DynamicSubsetOfPVStructure(PVStructure supersetStructure) {
	    		// TODO this is ugly... any we do not need any auxInfo!!!
	    		super(null, fieldFactory.createScalar(null, ScalarType.pvBoolean));
	    		replaceField(new DynamicSubsetOfStructure());
	    		this.supersetStructure = supersetStructure;
	    	}
	    	
			/* (non-Javadoc)
	         * @see org.epics.pvData.factory.AbstractPVField#postPut()
	         */
	        public void postPut() {
	            super.postPut();
	            for(PVField pvField : pvFieldsMap.values()) {
	                postPutNoParent((AbstractPVField)pvField);
	            }
	        }

	        /* (non-Javadoc)
	         * @see org.epics.pvData.pv.PVStructure#postPut(org.epics.pvData.pv.PVField)
	         */
	        public void postPut(PVField subField) {
	            Iterator<PVListener> iter;
	            iter = super.pvListenerList.iterator();
	            while(iter.hasNext()) {
	                PVListener pvListener = iter.next();
	                pvListener.dataPut(this,subField);
	            }
	            PVStructure pvParent = super.getParent();
	            if(pvParent!=null) pvParent.postPut(subField);
	        }

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVStructure#replacePVField(java.lang.String, org.epics.pvData.pv.PVField)
			 */
			public void replacePVField(String fieldName, PVField newPVField) {
				pvFieldsMap.remove(fieldName);
				appendPVField(newPVField);
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVStructure#appendPVField(org.epics.pvData.pv.PVField)
			 */
			public void appendPVField(PVField pvField) {
				pvFieldsMap.put(pvField.getField().getFieldName(), pvField);
				
				cachedPVFieldsArray = null;
				((DynamicSubsetOfStructure)getField()).changed();
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVStructure#getPVFields()
			 */
			public PVField[] getPVFields() {
				if (cachedPVFieldsArray == null)
					cachedPVFieldsArray = pvFieldsMap.values().toArray(new PVField[pvFieldsMap.size()]);
				return cachedPVFieldsArray;
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVStructure#getStructure()
			 */
			public Structure getStructure() {
				return (Structure)getField();
			}

			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.PVStructure#getSubField(java.lang.String)
			 */
			public PVField getSubField(String fieldName) {
				return findSubField(fieldName,this);
			}

		    private PVField findSubField(String fieldName,PVStructure pvStructure) {
		        if(fieldName==null || fieldName.length()<1) return null;
		        int index = fieldName.indexOf('.');
		        String name = fieldName;
		        String restOfName = null;
		        if(index>0) {
		            name = fieldName.substring(0, index);
		            if(fieldName.length()>index) {
		                restOfName = fieldName.substring(index+1);
		            }
		        }
		        // TODO
		        //PVField pvField = pvFieldsMap.get(name);

		        PVField[] pvFields = pvStructure.getPVFields();
		        PVField pvField = null;
		        for(PVField pvf : pvFields) {
		            if(pvf.getField().getFieldName().equals(name)) {
		                pvField = pvf;
		                break;
		            }
		        }
		        if(pvField==null) return null;
		        if(restOfName==null) return pvField;
		        if(pvField.getField().getType()!=Type.structure) return null;
		        return findSubField(restOfName,(PVStructure)pvField);
		    }
		    
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getBooleanField(java.lang.String)
		     */
		    public PVBoolean getBooleanField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvBoolean) {
		                return (PVBoolean)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type boolean ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getByteField(java.lang.String)
		     */
		    public PVByte getByteField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvByte) {
		                return (PVByte)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type byte ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getShortField(java.lang.String)
		     */
		    public PVShort getShortField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvShort) {
		                return (PVShort)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type short ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getIntField(java.lang.String)
		     */
		    public PVInt getIntField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvInt) {
		                return (PVInt)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type int ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getLongField(java.lang.String)
		     */
		    public PVLong getLongField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvLong) {
		                return (PVLong)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type long ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getFloatField(java.lang.String)
		     */
		    public PVFloat getFloatField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvFloat) {
		                return (PVFloat)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type float ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getDoubleField(java.lang.String)
		     */
		    public PVDouble getDoubleField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvDouble) {
		                return (PVDouble)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type double ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getStringField(java.lang.String)
		     */
		    public PVString getStringField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        if(pvField.getField().getType()==Type.scalar) {
		            Scalar scalar = (Scalar)pvField.getField();
		            if(scalar.getScalarType()==ScalarType.pvString) {
		                return (PVString)pvField;
		            }
		        }
		        super.message("fieldName " + fieldName + " does not have type string ",
		                MessageType.error);
		        return null;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getStructureField(java.lang.String)
		     */
		    public PVStructure getStructureField(String fieldName) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        Field field = pvField.getField();
		        Type type = field.getType();
		        if(type!=Type.structure) {
		            super.message(
		                "fieldName " + fieldName + " does not have type structure ",
		                MessageType.error);
		            return null;
		        }
		        return (PVStructure)pvField;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvData.pv.PVStructure#getArrayField(java.lang.String, org.epics.pvData.pv.ScalarType)
		     */
		    public PVArray getArrayField(String fieldName, ScalarType elementType) {
		        PVField pvField = findSubField(fieldName,this);
		        if(pvField==null) return null;
		        Field field = pvField.getField();
		        Type type = field.getType();
		        if(type!=Type.scalarArray) {
		            super.message(
		                "fieldName " + fieldName + " does not have type array ",
		                MessageType.error);
		            return null;
		        }
		        Array array = (Array)field;
		        if(array.getElementType()!=elementType) {
		            super.message(
		                    "fieldName "
		                    + fieldName + " is array but does not have elementType " + elementType.toString(),
		                    MessageType.error);
		                return null;
		        }
		        return (PVArray)pvField;
		    }
		    
			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.Serializable#serialize(java.nio.ByteBuffer)
			 */
			public void serialize(ByteBuffer buffer) {
		        for (PVField pvField : pvFieldsMap.values())
		        	pvField.serialize(buffer);
			}
			/* (non-Javadoc)
			 * @see org.epics.pvData.pv.Serializable#deserialize(java.nio.ByteBuffer)
			 */
			public void deserialize(ByteBuffer buffer) {
		        for (PVField pvField : pvFieldsMap.values())
		        	pvField.deserialize(buffer);
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#hashCode()
			 */
			@Override
			public int hashCode() {
				final int PRIME = 31;
				int result = 1;
				result = PRIME * result + Arrays.hashCode(cachedPVFieldsArray);
				result = PRIME * result + ((supersetStructure == null) ? 0 : supersetStructure.hashCode());
				result = PRIME * result + ((pvFieldsMap == null) ? 0 : pvFieldsMap.hashCode());
				return result;
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#equals(java.lang.Object)
			 */
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				final DynamicSubsetOfPVStructure other = (DynamicSubsetOfPVStructure) obj;
				if (supersetStructure == null) {
					if (other.supersetStructure != null)
						return false;
				} else if (!supersetStructure.equals(other.supersetStructure))
					return false;
				if (pvFieldsMap == null) {
					if (other.pvFieldsMap != null)
						return false;
				} else if (!pvFieldsMap.equals(other.pvFieldsMap))
					return false;
				return true;
			}
	    	
	    }
	    
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

            /*
			// not synced, but now it does not harm
            if (getRequest == null) getRequest = new GetRequest();
            
            //characteristicsGetRequest.fill(value);
            getRequest.get(asyncReadCallback);
            */
		}

		/* (non-Javadoc)
		 * @see org.epics.ca.server.ProcessVariable#write(org.epics.pvData.pv.PVField, int, org.epics.ca.server.ProcessVariableWriteCallback)
		 */
		@Override
		public CAStatus write(PVField value, int offset, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
			/*
            // not synced, but now it does not harm
            if (putRequest == null) putRequest = new PutRequest();

            return putRequest.put(value);
            */
			//return CA
			
            // TODO temp (no processing is done)
            final PVRecord record = channel.getPVRecord();
            record.lock();
            try
            {
    			// TODO test only...
    			copyValue(value, channelField.getPVField(), offset);
            } finally {
            	record.unlock();
            }
            
            return CAStatus.NORMAL;
		}
		
		private void copyValue(PVField from, PVField to, int offset) throws CAException
		{
			final Field field = to.getField();
			if (!field.equals(from.getField()))
				// TODO better exception
				throw new CAStatusException(CAStatus.BADTYPE, "data not compatible");
			
			switch (field.getType())
			{
				case scalar:
					switch (((Scalar)field).getScalarType())
					{
					case pvBoolean:
						((PVBoolean)to).put(((PVBoolean)from).get());
						return;
					case pvByte:
						((PVByte)to).put(((PVByte)from).get());
						return;
					case pvDouble:
						((PVDouble)to).put(((PVDouble)from).get());
						return;
					case pvFloat:
						((PVFloat)to).put(((PVFloat)from).get());
						return;
					case pvInt:
						((PVInt)to).put(((PVInt)from).get());
						return;
					case pvLong:
						((PVLong)to).put(((PVLong)from).get());
						return;
					case pvShort:
						((PVShort)to).put(((PVShort)from).get());
						return;
					case pvString:
						((PVString)to).put(((PVString)from).get());
						return;
					default:
						throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
					}
				case scalarArray:
					switch (((Array)field).getElementType())
					{
					case pvBoolean:
						{
							BooleanArrayData data = new BooleanArrayData();
							PVBooleanArray fromArray = (PVBooleanArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVBooleanArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvByte:
						{
							ByteArrayData data = new ByteArrayData();
							PVByteArray fromArray = (PVByteArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVByteArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvDouble:
						{
							DoubleArrayData data = new DoubleArrayData();
							PVDoubleArray fromArray = (PVDoubleArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVDoubleArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvFloat:
						{
							FloatArrayData data = new FloatArrayData();
							PVFloatArray fromArray = (PVFloatArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVFloatArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvInt:
						{
							IntArrayData data = new IntArrayData();
							PVIntArray fromArray = (PVIntArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVIntArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvLong:
						{
							LongArrayData data = new LongArrayData();
							PVLongArray fromArray = (PVLongArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVLongArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvShort:
						{
							ShortArrayData data = new ShortArrayData();
							PVShortArray fromArray = (PVShortArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVShortArray)to).put(offset, len, data.data, 0);
							return;
						}
					case pvString:
						{
							StringArrayData data = new StringArrayData();
							PVStringArray fromArray = (PVStringArray)from;
							final int len = fromArray.getLength();
							fromArray.get(0, len, data);
							((PVStringArray)to).put(offset, len, data.data, 0);
							return;
						}
					default:
						throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
					}

				case structure:
					PVStructure fromStructure = (PVStructure)from;
					PVStructure toStructure = (PVStructure)to;
					for (PVField fromField : fromStructure.getPVFields())
					{
						PVField toField = toStructure.getSubField(fromField.getField().getFieldName());
						if (toField != null)
							copyValue(fromField, toField, offset);
						// TODO else do not complain here..
					}
					break;
						
				default:
					throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
			}
		}

		/* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#interestDelete()
         */
        @Override
        public void interestDelete() {
            super.interestDelete();
            // stop monitoring
            monitorRequest.stop();
        }

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#interestRegister()
         */
        @Override
        public void interestRegister() {
            if(monitorRequest==null) {
                monitorRequest = new MonitorRequest();
                monitorRequest.lookForChange();
            }
            super.interestRegister();
            // start monitoring
            monitorRequest.start();
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
        
        
        private String getOption(String option) {
            String options = channel.getOptions();
            if(options==null) return null;
            int start = options.indexOf(option);
            if(start<0) return null;
            String rest = options.substring(start + option.length());
            if(rest==null || rest.length()<1 || rest.charAt(0)!='=') {
                message("getOption bad option " + rest,MessageType.error);
                return null;
            }
            rest = rest.substring(1);
            return rest;
        }
        
        private class GetRequest implements ChannelGetRequester, ChannelFieldGroupListener {        
            private final ChannelFieldGroup channelFieldGroup;
            private final ChannelGet channelGet;
            private RequestResult result;
            private PVField value;

            private GetRequest() {
                channelFieldGroup = channel.createFieldGroup(this);
                channelFieldGroup.addChannelField(channelField);

                String processValue = getOption("getProcess");
                boolean process = false;
                if(processValue!=null && processValue.equals("true")) process = true;
                channelGet = channel.createChannelGet(channelFieldGroup, this, process);
            }
            
            private synchronized void get(ProcessVariableReadCallback asyncReadCallback) {
                result = null;
                channelGet.get();
                
                // TODO this blocks :S
                if (result == null)
                {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        result = RequestResult.failure;
                    }
                }

                final CAStatus status = result == RequestResult.success ? CAStatus.NORMAL : CAStatus.GETFAIL;
                
                asyncReadCallback.processVariableReadCompleted(value, status);
                
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGetRequester#getDone(org.epics.ioc.util.RequestResult)
             */
            public synchronized void getDone(RequestResult requestResult) {
                result = requestResult;
                notify();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
             */
            public boolean nextDelayedGetField(PVField pvField) {
                // nothing to do
                return false;
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
             */
            public boolean nextGetField(ChannelField channelField, PVField pvField) {
            	this.value = pvField;
            	return false;
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // noop            
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return name + "-" + getClass().getName();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                // delegate to parent
                ChannelProcessVariable.this.message(message,messageType);
            }
            
        }

        private class PutRequest implements ChannelPutRequester,ChannelFieldGroupListener
        {
            private final ChannelFieldGroup channelFieldGroup;
            private final ChannelPut channelPut;
            private RequestResult result;       
            private PVField value2Put;
            
            private PutRequest() {
                channelFieldGroup = channel.createFieldGroup(this);
                channelFieldGroup.addChannelField(valueChannelField);
                String processValue = getOption("putProcess");
                boolean process = false;
                if(processValue!=null && processValue.equals("true")) process = true;
                channelPut = channel.createChannelPut(channelFieldGroup, this, process);
            }

            // note that this method is synced
            private synchronized CAStatus put(PVField value) {
                result = null;
                value2Put = value;
                channelPut.put();
                
                // if not completed wait
                if (result == null)
                {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        return CAStatus.PUTFAIL;
                    }
                }

                return result == RequestResult.success ? CAStatus.NORMAL : CAStatus.PUTFAIL;
            } 

            /*
             * (non-Javadoc)
             * 
             * @see org.epics.ioc.ca.ChannelPutRequester#putDone(org.epics.ioc.util.RequestResult)
             */
            public synchronized void putDone(RequestResult requestResult) {
                result = requestResult;
                // TODO this always returns null (javaIOC bug?)
                if (result == null)
                    result = RequestResult.success;
                notify();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
             */
            public boolean nextDelayedPutField(PVField field) {
                return false;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return name + "-" + getClass().getName();
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                // delegate to parent
                ChannelProcessVariable.this.message(message,messageType);
            }
              
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutRequester#nextPutField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
             */
            public boolean nextPutField(ChannelField channelField, PVField pvField) {
                return false;
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // noop
            }
        }

        private class MonitorRequest implements CDMonitorRequester, ChannelFieldGroupListener {
            private ChannelField severityField;
            private ChannelField timeStampField;
            private final CDMonitor cdMonitor;
            
            public MonitorRequest() {
                severityField = valueChannelField.findProperty("alarm.severity.index");           
                timeStampField = valueChannelField.findProperty("timeStamp");
                cdMonitor = CDMonitorFactory.create(channel, this);
            }
            
            private void lookForChange() {
            //    if(scalarType!=null && scalarType.isNumeric()) {
              //      cdMonitor.lookForChange(valueChannelField, true);
               // } else {
                    cdMonitor.lookForPut(valueChannelField, true);
               // }
                if (severityField != null) cdMonitor.lookForPut(severityField, true);
                if (timeStampField != null) cdMonitor.lookForPut(timeStampField, false);
            }
            
            private void start() {
                cdMonitor.start(3,executor);
            }
            private void stop() {
                cdMonitor.stop();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitorRequester#dataOverrun(int)
             */
            public void dataOverrun(int number) {
                // noop
                
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitorRequester#monitorCD(org.epics.ioc.ca.CD)
             */
            public void monitorCD(CD cd) {
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return name + "-" + getClass().getName();
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                ChannelProcessVariable.this.message(message,messageType);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO noop
                
            }

        }
           
    }
}
