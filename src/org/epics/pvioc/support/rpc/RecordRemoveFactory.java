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
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordStructure;
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
public class RecordRemoveFactory {
    /**
     * Create support for removing a record..
     * @param pvRecordStructure The field supported.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new RecordRemoveImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.pvioc.rpc.recordRemove";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    
    
    private static class RecordRemoveImpl extends AbstractSupport implements Runnable,ProcessContinueRequester
    {
    	private static final Executor executor = ExecutorFactory.create("recordRemoveFactory",ThreadPriority.low);
    	private final PVRecordStructure pvRecordStructure;
    	private ExecutorNode executorNode = executor.createNode(this);
    	private RecordProcess thisRecordProcess = null;
        private SupportProcessRequester supportProcessRequester = null;
        private PVString pvRecordName = null;
        
        private PVString pvStatus = null;
        
        private RecordRemoveImpl(PVRecordStructure pvRecordStructure) {
            super(RecordRemoveFactory.supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
        	thisRecordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            pvRecordName = pvStructure.getStringField("argument.recordName");
            if(pvRecordName==null) return;
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
            PVRecord pvRecord = masterPVDatabase.findRecord(pvRecordName.get());
            if(pvRecord==null) {
                pvStatus.put("record not found");
            } else {
                RecordProcess recordProcess = pvRecord.getRecordProcess();
                if(recordProcess==null) {
                    pvStatus.put("recordProcess not found");
                } else {
                   pvRecord.lock();
                   try {
                	   masterPVDatabase.removeRecord(pvRecord);
                	   pvRecord.detachClients();
                	   recordProcess.stop();
                	   recordProcess.uninitialize();
                	   pvStatus.put("success");
                   } finally {
                	   pvRecord.unlock();
                   }
                }
            }
            thisRecordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.ProcessContinueRequester#processContinue()
         */
        @Override
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
