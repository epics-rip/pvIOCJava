/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.rpc;

import org.epics.ioc.install.IOCDatabase;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class RecordRemoveFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvStructure The processControlStructure
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVStructure pvStructure) {
        return new RecordRemoveImpl(pvStructure);
    }
    
    private static final String supportName = "org.epics.ioc.rpc.recordRemove";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final IOCDatabase masterSupportDatabase = IOCDatabaseFactory.get(masterPVDatabase);
    
    
    private static class RecordRemoveImpl extends AbstractSupport implements Runnable,ProcessContinueRequester
    {
    	private static final Executor executor = ExecutorFactory.create("recordShowFactory",ThreadPriority.low);
    	private ExecutorNode executorNode = executor.createNode(this);
    	private RecordProcess thisRecordProcess = null;
        private SupportProcessRequester supportProcessRequester = null;
        private PVString pvRecordName = null;
        
        private PVString pvStatus = null;
        
        private RecordRemoveImpl(PVStructure pvStructure) {
            super(RecordRemoveFactory.supportName,pvStructure); 
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
        	thisRecordProcess = recordSupport.getRecordProcess();
            PVStructure pvStructure = (PVStructure)super.getPVField();
            pvRecordName = pvStructure.getStringField("arguments.recordName");
            if(pvRecordName==null) return;
            pvStatus = pvStructure.getStringField("result.status");
            if(pvStatus==null) return;
            super.initialize(recordSupport);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
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
                RecordProcess recordProcess = masterSupportDatabase.getLocateSupport(pvRecord).getRecordProcess();
                if(recordProcess==null) {
                    pvStatus.put("recordProcess not found");
                } else {
                   pvRecord.lock();
                   try {
                	   masterPVDatabase.removeRecord(pvRecord);
                	   pvRecord.detachClients();
                	   recordProcess.stop();
                	   recordProcess.uninitialize();
                   } finally {
                	   pvRecord.unlock();
                   }
                }
            }
            thisRecordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
         */
        @Override
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
