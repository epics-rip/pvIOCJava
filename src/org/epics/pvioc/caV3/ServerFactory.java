/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */

/**
 * @author mes
 */

package org.epics.pvioc.caV3;

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

import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.RunnableReady;
import org.epics.pvdata.misc.ThreadCreate;
import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.misc.ThreadReady;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.BooleanArrayData;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordClient;
import org.epics.pvioc.pvCopy.PVCopy;
import org.epics.pvioc.pvCopy.PVCopyFactory;
import org.epics.pvioc.pvCopy.PVCopyMonitor;
import org.epics.pvioc.pvCopy.PVCopyMonitorRequester;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.util.RequestResult;

import com.cosylab.epics.caj.cas.handlers.AbstractCASResponseHandler;

public class ServerFactory {
	/**
     * This starts the Channel Access Server.
     */
    public static void start() {
        new ThreadInstance();
    }
    
    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final Pattern periodPattern = Pattern.compile("[.]");
  
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
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
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
            String options = null;
            String[] names = periodPattern.split(aliasName,2);
            recordName = names[0];
            PVRecord pvRecord = masterPVDatabase.findRecord(recordName);
            if(pvRecord==null) {
                throw new CAStatusException(CAStatus.DEFUNCT, "Failed to find record " + recordName);
            }
            if(names.length==2) {
                options = names[1];
            } else {
                options = "field(value,alarm,timeStamp,display,control,valueAlarm)";
            }
            return new ChannelProcessVariable(aliasName,pvRecord,options, eventCallback);
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
    private static class ChannelProcessVariable extends ProcessVariable implements RecordProcessRequester,PVCopyMonitorRequester,PVRecordClient
    {
        private static final String[] YES_NO_LABELS = new String[] { "false", "true" };
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean done = false;

        private DBRType dbrType;
        private Type type;
        private ScalarType scalarType = null;
        private PVField valuePVField = null;
        private PVStructure valuePVStructure = null;
        private PVScalarArray valuePVArray = null;
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
        private boolean process = false;
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
        //used by getTimeStamp
        private TimeStamp timeStamp = TimeStampFactory.create();
        private PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();

        /**
         * Channel PV constructor.
         * @param pvName channelName.
         * @param eventCallback event callback, can be <code>null</code>.
         */
        public ChannelProcessVariable(
                String aliasName,
                PVRecord pvRecord,
                String options,
                ProcessVariableEventCallback eventCallback)
                throws CAStatusException, IllegalArgumentException, IllegalStateException
        {
            super(aliasName, eventCallback);
            this.pvRecord = pvRecord;
            this.eventCallback = eventCallback;
            String opt = getOption(options,"process");
            if(opt!=null) process = Boolean.valueOf(opt);
            String request = null;
            int ind = options.indexOf("field(");
            if(ind>=0) {
                request = options.substring(ind);
            } else {
                request = "field(" + options + ")";
            }
            CreateRequest createRequest = CreateRequest.create();
            PVStructure pvRequest = createRequest.createRequest(request);
            if(pvRequest==null) {
            	message(createRequest.getMessage(), MessageType.error);
            }
            pvCopy = PVCopyFactory.create(pvRecord, pvRequest, "");
            if(pvCopy==null) {
                throw new CAStatusException(CAStatus.DEFUNCT, "illegal request. Could not create pvCopy");
            }
            pvCopyStructure = pvCopy.createPVStructure();
            copyBitSet = new BitSet(pvCopyStructure.getNumberFields());
            copyBitSet.set(0);
            pvRecord.lock();
            try {
                pvCopy.updateCopyFromBitSet(pvCopyStructure, copyBitSet);
            } finally{
                pvRecord.unlock();
            }
            PVField[] pvFields = pvCopyStructure.getPVFields();
            for(int i=0; i<pvFields.length; i++) {
                PVField pvField = pvFields[i];
                if(pvField.getFieldName().equals("alarm")) continue;
                if(pvField.getFieldName().equals("timeStamp")) continue;
                valuePVField = pvField;
                break;
            }
            if(valuePVField==null) {
                throw new CAStatusException(CAStatus.DEFUNCT, "illegal request. No value field");
            }
            initializeChannelDBRType();            
            if(process) {
                recordProcess = pvRecord.getRecordProcess();
                processToken = recordProcess.requestProcessToken(this);
            	if(processToken==null) {
                    throw new CAStatusException(CAStatus.DEFUNCT, "Could not become processor ");
                }
            	canProcess = true;
            }
            pvRecord.registerClient(this);
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#destroy()
         */
        @Override
        public void destroy() {
        	if(isDestroyed) return;
        	isDestroyed = true;
            super.destroy();
            if(canProcess) {
            	recordProcess.releaseProcessToken(processToken);
            	processToken = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVRecordClient#detach(org.epics.pvdata.pv.PVRecord)
         */
        @Override
		public void detach(PVRecord pvRecord) {
			destroy();
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
        	if(isDestroyed) return CAStatus.CHANDESTROY;
            this.dbr = dbr;
        	if(process) {
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
            pvCopy.initCopy(pvCopyStructure, copyBitSet);
            getData(dbr);
            return CAStatus.NORMAL;
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
         */
        public CAStatus write(DBR dbr, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
        	if(isDestroyed) return CAStatus.CHANDESTROY;
        	this.dbr = dbr;
            if(process) {
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
            pvRecord.lock();
            try {
                pvCopy.updateRecord(pvCopyStructure, copyBitSet);
            } finally {
                pvRecord.unlock();
            }
            return CAStatus.NORMAL;
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
         */
        @Override
        public void recordProcessComplete() {
            if(getProcessActive) {
            	pvCopy.initCopy(pvCopyStructure, copyBitSet);
                getData(dbr);
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
         * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.RequestResult)
         */
        @Override
        public void recordProcessResult(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                message("recordProcessResult " + requestResult.toString(),MessageType.warning);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.ProcessSelfRequester#becomeProcessor(org.epics.pvioc.support.RecordProcess)
         */
        @Override
        public void becomeProcessor()
        {
        	if(getProcessActive) {
        		recordProcess.process(processToken, true);
        	} else if(putProcessActive) {
        		pvRecord.lock();
        		try {
        			putValueField(dbr);
        			copyBitSet.clear();
        			pvCopy.updateRecord(pvCopyStructure, copyBitSet);
        		} finally {
        			pvRecord.unlock();
        		}
        		recordProcess.process(processToken, false);
        	}
        }
		/* (non-Javadoc)
		 * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			message("canNotProcess " + reason,MessageType.warning);
			recordProcessComplete();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
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
        	if(isDestroyed) return;
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
         * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#dataChanged()
         */
        @Override
        public void dataChanged() {
            DBR dbr = AbstractCASResponseHandler.createDBRforReading(this);
            pvCopy.initCopy(monitorPVStructure,monitorChangeBitSet);
            getData(dbr,monitorPVStructure);
            eventCallback.postEvent(Monitor.VALUE|Monitor.LOG, dbr);
            monitorChangeBitSet.clear();
            monitorOverrunBitSet.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#unlisten()
         */
        @Override
        public void unlisten() {
            interestDelete();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
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
            if(valuePVField.getField().getType()==Type.structure) {
                valuePVStructure = (PVStructure)valuePVField;
                // look for enumerated structure
                if(valuePVStructure.getSubField("index")==null || valuePVStructure.getSubField("choices")==null) {
                    valuePVField = valuePVStructure.getSubField("value");
                    if(valuePVField==null) {
                        throw new CAStatusException(CAStatus.DEFUNCT, "illegal value");
                    }
                } else {
                    valuePVStructure = pvCopyStructure;
                }
            } else {
                valuePVStructure = pvCopyStructure;
            }
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
                valuePVArray = (PVScalarArray)valuePVField;
                elementCount = valuePVArray.getCapacity();
                scalarType = valuePVArray.getScalarArray().getElementType();
                dbrType = getChannelDBRType(scalarType);
                return;
            } else if(type==Type.structure) {
                PVStructure pvStructure = (PVStructure)valuePVField;
                valueIndexPV = pvStructure.getIntField("index");
                PVArray pvArray = pvStructure.getScalarArrayField("choices",ScalarType.pvString);
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
            throw new CAStatusException(CAStatus.DEFUNCT, "illegal value type");
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

        private String getOption(String request,String option) {
            int start = request.indexOf("[" +option);
            if(start<0) return null;
            request = request.substring(start+1);
            int end = request.indexOf(']');
            if(end<0) {
                message("getOption bad option " + request,MessageType.error);
                return null;
            }
            request = request.substring(0, end);
            int eq = request.indexOf('=');
            if(eq<0) {
                message("getOption bad option " + request,MessageType.error);
                return null;
            }
            request = request.substring(eq+1);
            return request;
        }

        private void getData(DBR dbr) {
            PVField[] pvFields = pvCopyStructure.getPVFields();
            String[] fieldNames = pvCopyStructure.getStructure().getFieldNames();
            getValueField(dbr,valuePVField);
            for(int i=0; i<pvFields.length; i++) {
                PVField pvField = pvFields[i];
                if(fieldNames[i].equals("timeStamp")) {
                    getTimeStampField(dbr,(PVStructure)pvField);
                } else if(fieldNames[i].equals("alarm")) {
                    getAlarmField(dbr,(PVStructure)pvField);
                }
            }
            getExtraInfo(dbr);
        }
        
        private void getData(DBR dbr,PVStructure pvStructure) {
            boolean gotValue = false;
            PVField pvField = pvStructure.getSubField("value");
            if(pvField!=null) {
                gotValue = true;
                getValueField(dbr,pvField);
            }
            PVField[] pvFields = pvStructure.getPVFields();
            String[] fieldNames = pvStructure.getStructure().getFieldNames();
            for(int i=0; i<pvFields.length; i++) {
                pvField = pvFields[i];
                if(fieldNames[i].equals("timeStamp")) {
                    getTimeStampField(dbr,(PVStructure)pvField);
                } else if(fieldNames[i].equals("alarm")) {
                    getAlarmField(dbr,(PVStructure)pvField);
                } else if(!gotValue) {
                    if(pvField.getField().getType()!=Type.structure) {
                        getValueField(dbr,pvField);
                        gotValue = true;
                    } else {
                        PVStructure xxx = (PVStructure)pvField;
                        pvField = xxx.getSubField("value");
                        if(pvField!=null) {
                            gotValue = true;
                            getValueField(dbr,pvField);
                        }
                    }
                }
            }
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
                    ((STRING) dbr).getStringValue()[0] = convert.toString((PVScalar)pvField);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    if(type==Type.scalar) {
                        if(scalarType==ScalarType.pvBoolean) {
                            PVBoolean pvBoolean = (PVBoolean)pvField;
                            value[0] = (short)((pvBoolean.get()) ? 1 : 0);
                        } else {
                            pvField.message("illegal enum", MessageType.error);
                        }
                    } else {
                        if (valueIndex!=-1) {
                            PVInt pvInt = (PVInt)((PVStructure)pvField).getSubField(valueIndex);
                            value[0] = (short) pvInt.get();
                        } else {
                            pvField.message("illegal enum", MessageType.error);
                        }
                    }
                } else if (dbrType == DBRType.BYTE) {
                    ((BYTE) dbr).getByteValue()[0] = convert.toByte((PVScalar)pvField);
                }
            } else {
                int dbrCount = dbr.getCount();
                if (dbrType == DBRType.DOUBLE) {
                    double[] value = ((DOUBLE) dbr).getDoubleValue();
                    convert.toDoubleArray((PVScalarArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.INT) {
                    int[] value = ((INT) dbr).getIntValue();
                    convert.toIntArray((PVScalarArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.SHORT) {
                    short[] value = ((SHORT) dbr).getShortValue();
                    convert.toShortArray((PVScalarArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.FLOAT) {
                    float[] value = ((FLOAT) dbr).getFloatValue();
                    convert.toFloatArray((PVScalarArray)pvField, 0, dbrCount, value, 0);
                } else if (dbrType == DBRType.STRING) {
                    String[] value = ((STRING) dbr).getStringValue();
                    convert.toStringArray((PVScalarArray)pvField, 0, dbrCount,
                            value, 0);
                } else if (dbrType == DBRType.ENUM) {
                    short[] value = ((ENUM) dbr).getEnumValue();
                    ScalarArray array = (ScalarArray)pvField.getField();
                    if(array.getElementType()==ScalarType.pvBoolean) {
                        PVBooleanArray pvBooleanArray = (PVBooleanArray)pvField;
                        BooleanArrayData data = new BooleanArrayData();
                        int count = pvBooleanArray.get(0, dbrCount, data);
                        boolean[] bools = data.data;
                        System.arraycopy(bools, 0, value, 0, count);
                    } else {
                        pvField.message("illegal enum", MessageType.error);
                    }
                } else if (dbrType == DBRType.BYTE) {
                    byte[] value = ((BYTE) dbr).getByteValue();
                    convert.toByteArray((PVScalarArray)pvField, 0, dbr.getCount(), value, 0);
                }
            }
        }

        
        private void getTimeStampField(DBR dbr,PVStructure field) {
            pvTimeStamp.attach(field);
            pvTimeStamp.get(timeStamp);
            final long TS_EPOCH_SEC_PAST_1970=7305*86400;
            ((TIME)dbr).setTimeStamp(new gov.aps.jca.dbr.TimeStamp(
                timeStamp.getSecondsPastEpoch()-TS_EPOCH_SEC_PAST_1970,
                timeStamp.getNanoSeconds()));
        }

        private void getAlarmField(DBR dbr,PVStructure pvAlarm) {
            PVInt pvSeverity = pvAlarm.getIntField("severity");
            STS sts = (STS)dbr; 
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(pvSeverity.get());
            switch (alarmSeverity)
            {
            case NONE:
                sts.setSeverity(Severity.NO_ALARM);
                sts.setStatus(Status.NO_ALARM);
                break;
            case MINOR:
                sts.setSeverity(Severity.MINOR_ALARM);
                // for now only SOFT_ALARM
                sts.setStatus(Status.SOFT_ALARM);
                break;
            case MAJOR:
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
                    ScalarArray array = (ScalarArray)valuePVField.getField();
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
            	PVStructure pvDisplay = null;
            	if(valuePVStructure.getSubField("display")!=null) {
            		pvDisplay = valuePVStructure.getStructureField("display");
            	}
                if(pvDisplay!=null) {
                    PVString unitsField = pvDisplay.getStringField("units");
                    gr.setUnits(unitsField.get());

                    if (dbr instanceof PRECISION)
                    {
                        // default;
                        short precision = (short)6;
                        // display has no precision field
                        // set precision
                        ((PRECISION)dbr).setPrecision(precision);
                    }

                    // all done via super-set double
                    PVDouble lowField = pvDisplay.getDoubleField("limitLow");
                    gr.setLowerDispLimit(lowField.get());
                    PVDouble highField = pvDisplay.getDoubleField("limitHigh");
                    gr.setUpperDispLimit(highField.get());
                }
                PVStructure pvValueAlarm = null;
            	if(valuePVStructure.getSubField("valueAlarm")!=null) {
            		pvValueAlarm = valuePVStructure.getStructureField("valueAlarm");
            	}
                if(pvValueAlarm!=null) {
                	double lowAlarmLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("lowAlarmLimit"));
                	double lowWarningLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("lowWarningLimit"));
                	double highWarningLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("highWarningLimit"));
                	double highAlarmLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("highAlarmLimit"));
                    gr.setLowerAlarmLimit(lowAlarmLimit);
                    gr.setLowerWarningLimit(lowWarningLimit);
                    gr.setUpperWarningLimit(highWarningLimit);
                    gr.setUpperAlarmLimit(highAlarmLimit);
                }
            }
            if (dbr instanceof CTRL) {
            	PVStructure pvControl = null;
            	if(valuePVStructure.getSubField("control")!=null) {
            		pvControl = valuePVStructure.getStructureField("control");
            	}
            	if(pvControl!=null) {
            		final CTRL ctrl = (CTRL)dbr;
            		// all done via double as super-set type
            		PVDouble lowField = pvControl.getDoubleField("limitLow");
            		ctrl.setLowerCtrlLimit(lowField.get());

            		PVDouble highField = pvControl.getDoubleField("limitHigh");
            		ctrl.setUpperCtrlLimit(highField.get());
            		PVStructure pvValueAlarm = null;
                	if(valuePVStructure.getSubField("valueAlarm")!=null) {
                		pvValueAlarm = valuePVStructure.getStructureField("valueAlarm");
                	}
            		if(pvValueAlarm!=null) {
                    	double lowAlarmLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("lowAlarmLimit"));
                    	double lowWarningLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("lowWarningLimit"));
                    	double highWarningLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("highWarningLimit"));
                    	double highAlarmLimit = convert.toDouble((PVScalar)pvValueAlarm.getSubField("highAlarmLimit"));
                        ctrl.setLowerAlarmLimit(lowAlarmLimit);
                        ctrl.setLowerWarningLimit(lowWarningLimit);
                        ctrl.setUpperWarningLimit(highWarningLimit);
                        ctrl.setUpperAlarmLimit(highAlarmLimit);
                    }
            	}
            }
        }
    }
    
}
