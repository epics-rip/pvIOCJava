/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.rpc;

import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.util.RequestResult;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class AddRecordsFactory {
    /**
     * Create support for adding new records to the master database.
     * @param pvRecordStructure The field supported.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new AddRecordsImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.pvioc.rpc.addRecords";
    private static final Install install = InstallFactory.get();
    
    
    private static class AddRecordsImpl extends AbstractSupport implements Runnable,ProcessContinueRequester
    {
    	private static final Executor executor = ExecutorFactory.create("addRecordsFactory",ThreadPriority.low);
    	private final PVRecordStructure pvRecordStructure;
    	private ExecutorNode executorNode = executor.createNode(this);
    	private RecordProcess thisRecordProcess = null;
        private SupportProcessRequester supportProcessRequester = null;
        private PVString pvFileName = null;
        
        private PVString pvStatus = null;
        
        private AddRecordsImpl(PVRecordStructure pvRecordStructure) {
            super(AddRecordsFactory.supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
        	thisRecordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            pvFileName = pvStructure.getStringField("arguments.fileName");
            if(pvFileName==null) return;
            pvStatus = pvStructure.getStringField("result.status");
            if(pvStatus==null) return;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
        	this.supportProcessRequester = supportProcessRequester;
            executor.execute(executorNode);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            pvStatus.put("");
            boolean initOK = install.installRecords(pvFileName.get(),this);
            if(initOK)  pvStatus.put("success");
            thisRecordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.ProcessContinueRequester#processContinue()
         */
        @Override
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        @Override
        public void message(String message, MessageType messageType) {
            pvStatus.put(pvStatus.get() + message);
            super.message(message, messageType);
        }
    }
}
