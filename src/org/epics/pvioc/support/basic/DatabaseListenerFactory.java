/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;


import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;



/**
 * @author mrk
 *
 */
public class DatabaseListenerFactory {
    
    public static Support create(PVRecordField pvRecordField) {
        PVField pvField = pvRecordField.getPVField();
        if(!pvField.getFieldName().equals("value")) {
            pvRecordField.message("DatabaseListener: Illegal field name. Must be value", MessageType.error);
            return null;
        }
        if(pvField.getField().getType()!=Type.scalar) {
            pvRecordField.message("DatabaseListener: Illegal field type. Must be scalar string", MessageType.error);
            return null;
        }
        PVScalar pvScalar = (PVScalar)pvField;
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvRecordField.message("DatabaseListener: Illegal field type. Must be scalar string", MessageType.error);
            return null;
        }
        return new DatabaseListener(pvRecordField);
    }
    
    private static class DatabaseListener extends AbstractSupport implements Requester, RecordProcessRequester
    {
        private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
        private static final String supportName = "org.epics.pvioc.databaseListener";
        private ReentrantLock lock = new ReentrantLock();
        private PVString pvValue = null;
        private PVRecord pvRecord = null;
        private RecordProcess recordProcess = null;
        private ProcessToken processToken = null;
        private String data = "";

        private DatabaseListener(PVRecordField pvRecordField) {
            super(supportName,pvRecordField);
            pvValue = (PVString)pvRecordField.getPVField();
            pvRecord = pvRecordField.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize(org.epics.pvioc.support.RecordSupport)
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            this.recordProcess = super.getPVRecordField().getPVRecord().getRecordProcess();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#start()
         */
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            recordProcess = pvRecord.getRecordProcess();
            processToken = recordProcess.requestProcessToken(this);
            masterPVDatabase.addRequester(this);
            setSupportState(SupportState.ready);
        }
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            lock.lock();
            pvValue.put(data);
            data = "";
            lock.unlock();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            masterPVDatabase.removeRequester(this);
            setSupportState(SupportState.readyForStart);
        }

        @Override
        public String getRequesterName() {
            return supportName;
        }

        @Override
        public void message(String message, MessageType messageType) {
            String value = messageType.toString() + " " + message + String.format("%n");
            lock.lock();
            data += value;
            lock.unlock();
            recordProcess.queueProcessRequest(processToken);
        }

        @Override
        public void becomeProcessor() {
            recordProcess.process(processToken,false);
        }

        @Override
        public void canNotProcess(String reason) {}

        @Override
        public void lostRightToProcess() {}

        @Override
        public void recordProcessResult(RequestResult requestResult) {}

        @Override
        public void recordProcessComplete() {}
    }
}
