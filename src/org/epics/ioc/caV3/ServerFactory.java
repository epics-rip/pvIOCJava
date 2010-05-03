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

package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.cas.Server;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.dbr.BYTE;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DOUBLE;
import gov.aps.jca.dbr.ENUM;
import gov.aps.jca.dbr.FLOAT;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.INT;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.SHORT;
import gov.aps.jca.dbr.STRING;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.epics.ioc.install.IOCDatabase;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadReady;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.BooleanArrayData;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVBooleanArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Type;
import org.epics.pvData.pvCopy.PVCopy;
import org.epics.pvData.pvCopy.PVCopyFactory;
import org.epics.pvData.pvCopy.PVCopyMonitor;
import org.epics.pvData.pvCopy.PVCopyMonitorRequester;

import com.cosylab.epics.caj.cas.handlers.AbstractCASResponseHandler;

public class ServerFactory {
    /**
     * This starts the Channel Access Server.
     */
    public static void start() {
        new ThreadInstance();
    }

    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final IOCDatabase iocDatabase = IOCDatabaseFactory.create(masterPVDatabase);
    private static final ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final Pattern periodPattern = Pattern.compile("[.]");
    private static final Pattern leftBracePattern = Pattern.compile("[{]");
    private static final Pattern rightBracePattern = Pattern.compile("[}]");
    private static final PVProperty pvProperty = PVPropertyFactory.getPVProperty();


    private static class ThreadInstance implements RunnableReady {

        private ThreadInstance() {
            threadCreate.create("caV3Server", 3, this);
        }

        /**
         * JCA server context.
         */
        private ServerContext context = null;

        /**
         * Initialize JCA context.
         * @throws CAException  throws on any failure.
         */
        private void initialize() throws CAException {

            // Get the JCALibrary instance.
            JCALibrary jca = JCALibrary.getInstance();

            // Create server implmentation
            CAServerImpl server = new CAServerImpl();

            // Create a context with default configuration values.
            context = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, server);

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
        throws CAStatusException, IllegalArgumentException,IllegalStateException
        {
            String recordName = null;
            String fieldName = null;
            String options = null;
            String[] names = periodPattern.split(aliasName,2);
            recordName = names[0];
            PVRecord pvRecord = masterPVDatabase.findRecord(recordName);
            if(pvRecord==null) {
                throw new CAStatusException(CAStatus.DEFUNCT, "Failed to find record " + recordName);
            }
            if(names.length==2) {
                names = leftBracePattern.split(names[1], 2);
                fieldName = names[0];
                if(fieldName.length()==0) fieldName = null;
                if(names.length==2) {
                    names = rightBracePattern.split(names[1], 2);
                    options = names[0];
                }
            }
            if(fieldName==null || fieldName.length()<=0 || fieldName.equals("VAL")) fieldName = "value";
            PVField pvField = pvRecord.getPVStructure().getSubField(fieldName);
            if(pvField==null) {
                throw new CAStatusException(CAStatus.DEFUNCT, "Failed to find field " + fieldName);
            }
            return new ChannelProcessVariable(aliasName,pvRecord,pvField,options, eventCallback);
        }

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.Server#processVariableExistanceTest(java.lang.String, java.net.InetSocketAddress, gov.aps.jca.cas.ProcessVariableExistanceCallback)
         */
        public ProcessVariableExistanceCompletion processVariableExistanceTest(
                String aliasName, InetSocketAddress clientAddress,
                ProcessVariableExistanceCallback asyncCompletionCallback)
        throws CAException, IllegalArgumentException, IllegalStateException {
            String recordName = null;
            String[] names = periodPattern.split(aliasName,2);
            recordName = names[0];
            PVRecord pvRecord = masterPVDatabase.findRecord(recordName);
            boolean exists = ((pvRecord==null) ? false : true);
            return exists ? ProcessVariableExistanceCompletion.EXISTS_HERE : ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
        }
    }

    /**
     * Channel process variable implementation. 
     */
    private static class ChannelProcessVariable extends ProcessVariable implements RecordProcessRequester,PVCopyMonitorRequester
    {
        private static final String[] YES_NO_LABELS = new String[] { "false", "true" };
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean done = false;

        private DBRType dbrType;
        private Type type;
        private ScalarType scalarType = null;
        private String options = null;
        private PVField valuePVField = null;
        private PVArray valuePVArray = null;
        private PVScalar valuePVScalar = null;
        private PVInt valueIndexPV = null;
        private PVStringArray valueChoicesPV = null;
        private int valueIndex = -1;

        private PVCopy pvCopy = null;
        private PVStructure pvCopyStructure = null;
        private BitSet copyBitSet = null;
        
        private PVRecord pvRecord = null;
        private RecordProcess recordProcess = null;
        private DBR dbr = null;
        private boolean getProcess = false;
        private boolean putProcess = false;
        private boolean canProcess = false;
        private boolean processActive = false;
        private boolean getProcessActive = false;
        private boolean putProcessActive = false;
        private ProcessToken processToken = null;
        
        private PVCopyMonitor pvCopyMonitor = null;
        private PVStructure monitorPVStructure = null;
        private BitSet monitorChangeBitSet = null;
        private BitSet monitorOverrunBitSet = null;

        private int elementCount = 1;

        private String[] enumLabels = null;

        /**
         * Channel PV constructor.
         * @param pvName channelName.
         * @param eventCallback event callback, can be <code>null</code>.
         */
        public ChannelProcessVariable(
                String aliasName,PVRecord pvRecord,PVField valuePV,
                String options, ProcessVariableEventCallback eventCallback)
                throws CAStatusException, IllegalArgumentException, IllegalStateException
        {
            super(aliasName, eventCallback);
            this.pvRecord = pvRecord;
            this.options = options;
            this.eventCallback = eventCallback;
            PVStructure pvTimeStamp = null;
            PVStructure pvAlarm = null;
            PVStructure pvDisplay = null;
            PVStructure pvControl = null;
            int nfields = 1; // valueField is 1st
            PVField pvTemp= pvProperty.findProperty(valuePV, "alarm");
            if(pvTemp==null) pvTemp = pvProperty.findPropertyViaParent(valuePV, "alarm");
            if(pvTemp!=null) {
                nfields++;
                pvAlarm = (PVStructure)pvTemp;
            }
            pvTemp= pvProperty.findProperty(valuePV, "timeStamp");
            if(pvTemp==null) pvTemp = pvProperty.findPropertyViaParent(valuePV, "timeStamp");
            if(pvTemp!=null) {
                nfields++;
                pvTimeStamp = (PVStructure)pvTemp;
            }
            pvTemp= pvProperty.findProperty(valuePV, "display");
            if(pvTemp==null) pvTemp = pvProperty.findPropertyViaParent(valuePV, "display");
            if(pvTemp!=null) {
                nfields++;
                pvDisplay = (PVStructure)pvTemp;
            }
            pvTemp= pvProperty.findProperty(valuePV, "control");
            if(pvTemp==null) pvTemp = pvProperty.findPropertyViaParent(valuePV, "control");
            if(pvTemp!=null) {
                nfields++;
                pvControl = (PVStructure)pvTemp;
            }
            String option = getOption("shareData");
            boolean shareData = Boolean.getBoolean(option);
            PVStructure pvRequest = pvDataCreate.createPVStructure(null, pvRecord.getRecordName(), new Field[0]);
            PVString pvString = null;
            if(shareData) {
            	PVStructure pvStruct = pvDataCreate.createPVStructure(pvRequest, "value", new Field[0]);
            	PVStructure pvLeaf = pvDataCreate.createPVStructure(pvStruct, "leaf", new Field[0]);
            	pvString = (PVString)pvDataCreate.createPVScalar(pvLeaf,"source", ScalarType.pvString);
                pvString.put(valuePV.getFullFieldName());
                pvLeaf.appendPVField(pvString);
                pvString = (PVString)pvDataCreate.createPVScalar(pvLeaf,"shareData", ScalarType.pvString);
                pvString.put("true");
                pvLeaf.appendPVField(pvString);
                pvStruct.appendPVField(pvLeaf);
                pvRequest.appendPVField(pvStruct);
            	
            } else {
            	pvString = (PVString)pvDataCreate.createPVScalar(pvRequest,"value", ScalarType.pvString);
                pvString.put(valuePV.getFullFieldName());
                pvRequest.appendPVField(pvString);
            }
            if(pvAlarm!=null) {
                pvString = (PVString)pvDataCreate.createPVScalar(pvRequest,"alarm", ScalarType.pvString);
                pvString.put(pvAlarm.getFullFieldName());
                pvRequest.appendPVField(pvString);
            }
            if(pvTimeStamp!=null) {
                pvString = (PVString)pvDataCreate.createPVScalar(pvRequest,"timeStamp", ScalarType.pvString);
                pvString.put(pvTimeStamp.getFullFieldName());
                pvRequest.appendPVField(pvString);
            }
            if(pvDisplay!=null) {
                pvString = (PVString)pvDataCreate.createPVScalar(pvRequest,"display", ScalarType.pvString);
                pvString.put(pvDisplay.getFullFieldName());
                pvRequest.appendPVField(pvString);
            }
            if(pvControl!=null) {
                pvString = (PVString)pvDataCreate.createPVScalar(pvRequest,"control", ScalarType.pvString);
                pvString.put(pvControl.getFullFieldName());
                pvRequest.appendPVField(pvString);
            }
            pvCopy = PVCopyFactory.create(pvRecord, pvRequest, "");
            pvCopyStructure = pvCopy.createPVStructure();
            copyBitSet = new BitSet(pvCopyStructure.getNumberFields());
            copyBitSet.set(0);
            pvCopy.updateCopyFromBitSet(pvCopyStructure, copyBitSet, true);
            valuePVField = pvCopyStructure.getSubField("value");
            initializeChannelDBRType();
            
            option = getOption("getProcess");
            if(option!=null) getProcess = Boolean.valueOf(option);
            option = getOption("putProcess");
            if(option!=null) putProcess = Boolean.valueOf(option);
            if(getProcess||putProcess) {
                recordProcess = iocDatabase.getLocateSupport(pvRecord).getRecordProcess();
                processToken = recordProcess.requestProcessToken(this);
            	if(processToken==null) {
                    throw new CAStatusException(CAStatus.DEFUNCT, "Could not become processor ");
                }
            	canProcess = true;
            }   
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#destroy()
         */
        @Override
        public void destroy() {
            super.destroy();
            if(canProcess) {
            	recordProcess.releaseProcessToken(processToken);
            	processToken = null;
            }
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#getType()
         */
        @Override
        public DBRType getType() {
            return dbrType;
        }

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#getEnumLabels()
         */
        @Override
        public String[] getEnumLabels() {
            return enumLabels;
        }



        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#getDimensionSize(int)
         */
        @Override
        public int getDimensionSize(int dimension) {
            return elementCount;
        }

        /* (non-Javadoc)if(pvAlarm!=null) pvAlarm = pvStructure.getStructureField("alarm");
         * @see gov.aps.jca.cas.ProcessVariable#getMaxDimension()
         */
        @Override
        public int getMaxDimension() {
            return elementCount > 1 ? 1 : 0;
        }

        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
         */
        public CAStatus read(DBR dbr, ProcessVariableReadCallback asyncReadCallback) throws CAException {
            this.dbr = dbr;
        	if(getProcess) {
                boolean ok = true;
                synchronized(this) {
                    if(processActive) {
                        ok = false;
                    } else {
                        processActive = true;
                        getProcessActive = true;
                    }
                }
                if(!ok) {
                    message("process already active",MessageType.warning);
                    return CAStatus.DBLCLFAIL;
                }
                done = false;
                recordProcess.queueProcessRequest(processToken);
                lock.lock();
                try {
                    while(!done) {
                        try {
                            waitDone.await();
                        } catch(InterruptedException e) {}
                    }
                } finally {
                    lock.unlock();
                }
                synchronized(this) {
                    processActive = false;
                }
                return CAStatus.NORMAL;
            }
            pvCopy.initCopy(pvCopyStructure, copyBitSet, true);
            getData(dbr,pvCopyStructure);
            return CAStatus.NORMAL;
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
         */
        public CAStatus write(DBR dbr, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
            if(putProcess) {
                boolean ok = true;
                synchronized(this) {
                    if(processActive) {
                        ok = false;
                    } else {
                        processActive = true;
                        putProcessActive = true;
                    }
                }
                if(!ok) {
                    message("process already active",MessageType.warning);
                    return CAStatus.DBLCLFAIL;
                }
                done = false;
                recordProcess.queueProcessRequest(processToken);
                lock.lock();
                try {
                    while(!done) {
                        try {
                            waitDone.await();
                        } catch(InterruptedException e) {}
                    }
                } finally {
                    lock.unlock();
                }
                synchronized(this) {
                    processActive = false;
                }
            }
            copyBitSet.clear();
            putValueField(dbr);
            pvCopy.updateRecord(pvCopyStructure, copyBitSet, true);
            return CAStatus.NORMAL;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
         */
        @Override
        public void recordProcessComplete() {
            if(getProcessActive) {
            	pvCopy.initCopy(pvCopyStructure, copyBitSet, true);
                getData(dbr,pvCopyStructure);
                dbr = null;
                getProcessActive = false;
                recordProcess.setInactive(processToken);
            } else if(putProcessActive) {
                dbr = null;
                putProcessActive = false;
            }
            lock.lock();
            try {
                done = true;
                waitDone.signal();
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        @Override
        public void recordProcessResult(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                message("recordProcessResult " + requestResult.toString(),MessageType.warning);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
         */
        @Override
        public void becomeProcessor()
        {
        	if(getProcessActive) {
        		recordProcess.process(processToken, true, null);
        	} else if(putProcessActive) {
        		pvRecord.lock();
        		try {
        			putValueField(dbr);
        			copyBitSet.clear();
        			pvCopy.updateRecord(pvCopyStructure, copyBitSet, true);
        		} finally {
        			pvRecord.unlock();
        		}
        		recordProcess.process(processToken, false, null);
        	}
        }
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			message("canNotProcess " + reason,MessageType.warning);
			recordProcessComplete();
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
		 */
		@Override
		public void lostRightToProcess(){
			throw new IllegalStateException("lostRightToProcess");
		}
		/* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#interestDelete()
         */
        @Override
        public void interestDelete() {
            super.interestDelete();
            PVCopyMonitor pvCopyMonitor = null;
            synchronized(this) {
            	if(this.pvCopyMonitor==null) return;
            	pvCopyMonitor = this.pvCopyMonitor;
            }
            pvCopyMonitor.stopMonitoring();
            synchronized(this) {
                pvCopyMonitor = null;
                monitorPVStructure = null;
                monitorChangeBitSet = null;
                monitorOverrunBitSet = null;
            }
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#interestRegister()
         */
        @Override
        public void interestRegister() {
        	PVCopyMonitor pvCopyMonitor = null;
            synchronized(this) {
                if(this.pvCopyMonitor!=null) {
                    throw new IllegalStateException("interestRegister but already monitoring");
                }
                this.pvCopyMonitor = pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
            }
            monitorPVStructure = pvCopy.createPVStructure();
            monitorChangeBitSet = new BitSet(monitorPVStructure.getNumberFields());
            monitorOverrunBitSet = new BitSet(monitorPVStructure.getNumberFields());
            super.interestRegister();
            pvCopyMonitor.startMonitoring(monitorChangeBitSet,monitorOverrunBitSet);
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#dataChanged()
         */
        @Override
        public void dataChanged() {
            DBR dbr = AbstractCASResponseHandler.createDBRforReading(this);
            pvCopy.initCopy(monitorPVStructure,monitorChangeBitSet, true);
            getData(dbr,monitorPVStructure);
            eventCallback.postEvent(Monitor.VALUE|Monitor.LOG, dbr);
            monitorChangeBitSet.clear();
            monitorOverrunBitSet.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#unlisten()
         */
        @Override
        public void unlisten() {
            interestDelete();
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
        /**
         * Extract value field type and return DBR type equvivalent.
         * @return DBR type.
         * @throws CAStatusException
         */
        private void initializeChannelDBRType() throws CAStatusException {
            Field field = valuePVField.getField();
            type = field.getType();
            if(type==Type.scalar) {
                valuePVScalar = (PVScalar)valuePVField;
                Scalar scalar = (Scalar)field;
                scalarType = scalar.getScalarType();
                dbrType = getChannelDBRType(scalar.getScalarType());
                elementCount = 1;
                return;
            } else if(type==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                elementCount = valuePVArray.getCapacity();
                scalarType = valuePVArray.getArray().getElementType();
                dbrType = getChannelDBRType(scalarType);
                return;
            } else if(type==Type.structure) {
                PVStructure pvStructure = (PVStructure)valuePVField;
                valueIndexPV = pvStructure.getIntField("index");
                PVArray pvArray = pvStructure.getArrayField("choices",ScalarType.pvString);
                if(pvArray!=null) {
                    valueChoicesPV = (PVStringArray)pvArray;
                }
                if (valueIndexPV!=null && valueChoicesPV!=null)
                {
                    valueIndex = valueIndexPV.getFieldOffset();
                    // this is done only once..
                    int count = valueChoicesPV.getLength();
                    StringArrayData data = new StringArrayData();
                    enumLabels = new String[count];
                    int num = valueChoicesPV.get(0, count, data);
                    System.arraycopy(data.data, 0, enumLabels, 0, num);
                    dbrType = DBRType.ENUM;
                    return;
                }
            }
            throw new RuntimeException("unsupported type");
        }

        /**
         * Convert DB type to DBR type.
         * @return DBR type.
         * @throws CAStatusException
         */
        private final DBRType getChannelDBRType(ScalarType type) {
            switch (type) {
            case pvBoolean:
                enumLabels = YES_NO_LABELS;
                return DBRType.ENUM;
            case pvByte:
                return DBRType.BYTE;
            case pvShort:
                return DBRType.SHORT;
            case pvInt:
            case pvLong:
                return DBRType.INT;
            case pvFloat:
                return DBRType.FLOAT;
            case pvDouble:
                return DBRType.DOUBLE;
            case pvString:
                return DBRType.STRING;
            default:
                throw new RuntimeException("unsupported type");
            }
        }

        private String getOption(String option) {
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

        private void getData(DBR dbr, PVStructure pvStructure) {
            PVField[] pvFields = pvStructure.getPVFields();
            for(int i=0; i<pvFields.length; i++) {
                PVField pvField = pvFields[i];
                if(pvField.getField().getFieldName().equals("value")) {
                    getValueField(dbr,pvField);
                } else if(pvField.getField().getFieldName().equals("timeStamp")) {
                    getTimeStampField(dbr,(PVStructure)pvField);
                } else if(pvField.getField().getFieldName().equals("alarm")) {
                    getAlarmField(dbr,(PVStructure)pvField);
                }
            }
            getExtraInfo(dbr);
        }
        
        private void getValueField(DBR dbr, PVField pvField) {
            if (elementCount == 1) {
                if (dbrType == DBRType.DOUBLE) {
                    ((DOUBLE) dbr).getDoubleValue()[0] = convert.toDouble((PVScalar)pvField);
                } else if (dbrType == DBRType.INT) {
                    ((INT) dbr).getIntValue()[0] = convert.toInt((PVScalar)pvField);
                } else if (dbrType == DBRType.SHORT) {
                    ((SHORT) dbr).getShortValue()[0] = convert.toShort((PVScalar)pvField);
                } else if (dbrType == DBRType.FLOAT) {
                    ((FLOAT) dbr).getFloatValue()[0] = convert.toFloat((PVScalar)pvField);
                } else if (dbrType == DBRType.STRING) {
                    ((STRING) dbr).getStringValue()[0] = convert.getString((PVScalar)pvField);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    if(type==Type.scalar) {
                        if(scalarType==ScalarType.pvBoolean) {
                            PVBoolean pvBoolean = (PVBoolean)pvField;
                            value[0] = (short)((pvBoolean.get()) ? 1 : 0);
                        } else {
                            valuePVField.message("illegal enum", MessageType.error);
                        }
                    } else {
                        if (valueIndex!=-1) {
                            PVInt pvInt = (PVInt)((PVStructure)pvField).getSubField(valueIndex);
                            value[0] = (short) pvInt.get();
                        } else {
                            valuePVField.message("illegal enum", MessageType.error);
                        }
                    }
                } else if (dbrType == DBRType.BYTE) {
                    ((BYTE) dbr).getByteValue()[0] = convert.toByte((PVScalar)pvField);
                }
            } else {
                int dbrCount = dbr.getCount();
                if (dbrType == DBRType.DOUBLE) {
                    double[] value = ((DOUBLE) dbr).getDoubleValue();
                    convert.toDoubleArray((PVArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.INT) {
                    int[] value = ((INT) dbr).getIntValue();
                    convert.toIntArray((PVArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.SHORT) {
                    short[] value = ((SHORT) dbr).getShortValue();
                    convert.toShortArray((PVArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.FLOAT) {
                    float[] value = ((FLOAT) dbr).getFloatValue();
                    convert.toFloatArray((PVArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.STRING) {
                    String[] value = ((STRING) dbr).getStringValue();
                    convert.toStringArray((PVArray) pvField, 0, dbrCount,
                            value, 0);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    Array array = (Array)pvField.getField();
                    if(array.getElementType()==ScalarType.pvBoolean) {
                        PVBooleanArray pvBooleanArray = (PVBooleanArray)pvField;
                        BooleanArrayData data = new BooleanArrayData();
                        int count = pvBooleanArray.get(0, dbrCount, data);
                        boolean[] bools = data.data;
                        System.arraycopy(bools, 0, value, 0, count);
                    } else {
                        valuePVField.message("illegal enum", MessageType.error);
                    }
                } else if (dbrType == DBRType.BYTE) {
                    byte[] value = ((BYTE) dbr).getByteValue();
                    convert.toByteArray((PVArray)pvField, 0, dbr.getCount(), value, 0);
                }
            }
        }

        private void getTimeStampField(DBR dbr,PVStructure field) {
            TimeStamp timeStamp = TimeStampFactory.getTimeStamp(field);

            final long TS_EPOCH_SEC_PAST_1970=7305*86400;
            ((TIME)dbr).setTimeStamp(new gov.aps.jca.dbr.TimeStamp(timeStamp.getSecondsPastEpoch()-TS_EPOCH_SEC_PAST_1970, timeStamp.getNanoSeconds()));
        }

        private void getAlarmField(DBR dbr,PVStructure pvAlarm) {
            PVInt pvSeverity = pvAlarm.getIntField("severity.index");
            STS sts = (STS)dbr; 
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(pvSeverity.get());
            switch (alarmSeverity)
            {
            case none:
                sts.setSeverity(Severity.NO_ALARM);
                sts.setStatus(Status.NO_ALARM);
                break;
            case minor:
                sts.setSeverity(Severity.MINOR_ALARM);
                // for now only SOFT_ALARM
                sts.setStatus(Status.SOFT_ALARM);
                break;
            case major:
                sts.setSeverity(Severity.MAJOR_ALARM);
                // for now only SOFT_ALARM
                sts.setStatus(Status.SOFT_ALARM);
                break;
            default:
                sts.setSeverity(Severity.INVALID_ALARM);
            sts.setStatus(Status.UDF_ALARM);
            }
        }
        
        private boolean putValueField(DBR dbr) {
            copyBitSet.clear();
            if(dbrType!=DBRType.ENUM) copyBitSet.set(valuePVField.getFieldOffset());
            if (elementCount == 1) {
                if (dbrType == DBRType.DOUBLE) {
                    double[] value = ((DOUBLE) dbr).getDoubleValue();
                    convert.fromDouble(valuePVScalar, value[0]);
                } else if (dbrType == DBRType.INT) {
                    int[] value = ((INT) dbr).getIntValue();
                    convert.fromInt(valuePVScalar, value[0]);
                } else if (dbrType == DBRType.SHORT) {
                    short[] value = ((SHORT) dbr).getShortValue();
                    convert.fromShort(valuePVScalar, value[0]);
                } else if (dbrType == DBRType.FLOAT) {
                    float[] value = ((FLOAT) dbr).getFloatValue();
                    convert.fromFloat(valuePVScalar, value[0]);
                } else if (dbrType == DBRType.STRING) {
                    String[] value = ((STRING) dbr).getStringValue();
                    convert.fromString(valuePVScalar, value[0]);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    if(valuePVField.getField().getType()==Type.scalar) {
                        PVScalar pvScalar = valuePVScalar;
                        if(pvScalar.getScalar().getScalarType()==ScalarType.pvBoolean) {
                            PVBoolean pvBoolean = (PVBoolean)valuePVField;
                            pvBoolean.put((value[0]==0) ? false : true);
                            copyBitSet.set(pvBoolean.getFieldOffset());
                        } else {
                            valuePVField.message("illegal enum", MessageType.error);
                        }
                    } else {                               
                        if (valueIndexPV != null)  {
                            valueIndexPV.put(value[0]);
                            copyBitSet.set(valueIndexPV.getFieldOffset());
                        } else {
                            valuePVField.message("illegal enum",MessageType.error);
                        }
                    }
                } else if (dbrType == DBRType.BYTE) {
                    byte[] value = ((BYTE) dbr).getByteValue();
                    convert.fromInt(valuePVScalar, value[0]);
                }
            } else {
                int dbrCount = dbr.getCount();
                if (dbrType == DBRType.DOUBLE) {
                    double[] value = ((DOUBLE) dbr).getDoubleValue();
                    convert.fromDoubleArray(valuePVArray, 0, dbrCount,
                            value, 0);
                } else if (dbrType == DBRType.INT) {
                    int[] value = ((INT) dbr).getIntValue();
                    convert.fromIntArray(valuePVArray, 0, dbrCount, value,
                            0);
                } else if (dbrType == DBRType.SHORT) {
                    short[] value = ((SHORT) dbr).getShortValue();
                    convert.fromShortArray(valuePVArray, 0, dbrCount, value,
                            0);
                } else if (dbrType == DBRType.FLOAT) {
                    float[] value = ((FLOAT) dbr).getFloatValue();
                    convert.fromFloatArray(valuePVArray, 0, dbrCount, value,
                            0);
                } else if (dbrType == DBRType.STRING) {
                    String[] values = ((STRING) dbr).getStringValue();
                    convert.fromStringArray(valuePVArray, 0, dbr
                            .getCount(), values, 0);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    Array array = (Array)valuePVField.getField();
                    if(array.getElementType()==ScalarType.pvBoolean) {
                        PVBooleanArray pvBooleanArray = (PVBooleanArray)valuePVField;
                        boolean[] bools = new boolean[dbrCount];
                        for(int i=0; i<dbrCount; i++) {
                            bools[i] = (value[i]==0) ? false : true;
                        }
                        pvBooleanArray.put(0, dbrCount, bools, 0);
                    } else {
                        valuePVField.message("illegal enum", MessageType.error);
                    }
                } else if (dbrType == DBRType.BYTE) {
                    byte[] value = ((BYTE) dbr).getByteValue();
                    convert.fromByteArray(valuePVArray, 0, dbrCount, value,
                            0);
                }
            }
            return false;
        }



        private void getExtraInfo(DBR dbr) {
            // labels
            if (dbr instanceof LABELS)
                ((LABELS)dbr).setLabels(enumLabels);
            if (dbr instanceof GR) {
                final GR gr = (GR)dbr;
                PVStructure pvDisplay = pvCopyStructure.getStructureField("display");
                if(pvDisplay!=null) {
                    PVString unitsField = pvDisplay.getStringField("units");
                    gr.setUnits(unitsField.get());

                    if (dbr instanceof PRECISION)
                    {
                        // default;
                        short precision = (short)6;
                        PVInt pvInt = pvDisplay.getIntField("resolution");
                        if(pvInt!=null) {
                            precision = (short)pvInt.get();
                        }   
                        // set precision
                        ((PRECISION)dbr).setPrecision(precision);
                    }

                    // all done via super-set double
                    PVDouble lowField = pvDisplay.getDoubleField("limit.low");
                    gr.setLowerDispLimit(lowField.get());
                    PVDouble highField = pvDisplay.getDoubleField("limit.high");
                    gr.setUpperDispLimit(highField.get());
                }
            }
            if (dbr instanceof CTRL) {
                PVStructure pvControl = pvCopyStructure.getStructureField("control");
                if(pvControl!=null) {
                    final CTRL ctrl = (CTRL)dbr;
                    // all done via double as super-set type
                    PVDouble lowField = pvControl.getDoubleField("limit.low");
                    ctrl.setLowerCtrlLimit(lowField.get());

                    PVDouble highField = pvControl.getDoubleField("limit.high");
                    ctrl.setUpperCtrlLimit(highField.get());
                }
            }
        }
    }
}
