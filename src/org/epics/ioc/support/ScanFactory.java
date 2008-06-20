/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBListener;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.RecordListener;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanField;
import org.epics.ioc.util.ScanFieldFactory;
import org.epics.ioc.util.ScanPriority;
import org.epics.ioc.util.ScanType;
import org.epics.ioc.util.ScannerFactory;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFactory {
    private static IOCExecutor iocExecutor 
        = IOCExecutorFactory.create("scanFieldModify", ScanPriority.lower);
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    private static final String supportName = "scan";
    /**
     * Create support for the scan field.
     * @param dbStructure The interface to the scan field.
     * @return The support or null if the scan field is improperly defined.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        ScanField  scanField = ScanFieldFactory.create(dbStructure.getDBRecord());
        if(scanField==null) return null;
        String supportName = pvStructure.getSupportName();
        if(!supportName.equals(supportName)) {
            pvStructure.message(
                    "ScanFactory create supportName is " + supportName
                    + " but expected scan",
                    MessageType.fatalError);
                return null;
        }
        try {
            return new ScanImpl(dbStructure,scanField);
        } catch (IllegalStateException e) {
            pvStructure.message(
                "ScanFactory create failure " + e.getMessage(),
                MessageType.fatalError);
            return null;
        }  
    }
    
    private static class ScanImpl extends AbstractSupport
    implements ScanSupport,DBListener
    {
        private ScanField scanField;
        private DBRecord dbRecord = null;
        private PVProcessSelf pvProcessSelf;
        private RecordListener recordListener;
        
        private boolean isActive = false;
        private boolean isStarted = false;
        
        private PVStructure pvScanType;
        private DBField dbScanType;
        private PVInt pvScanTypeIndex;
        private DBField dbScanTypeIndex;
        
        private PVDouble pvRate;
        private DBField dbRate;
        private PVString pvEventName;
        private DBField dbEventName;
        
        private ScanType scanType = null;
        private double scanRate;
        private ScanPriority scanPriority = null;
        private String eventName = null;
        private ScanModify scanModify = null;
        
        private ScanImpl(DBStructure dbScan,ScanField scanField) {
            super(supportName,dbScan);
            this.scanField = scanField;
            dbRecord = dbScan.getDBRecord();
            recordListener = dbRecord.createRecordListener(this);
            pvScanType = dbScan.getPVStructure().getStructureField("type", "scanType");
            dbScanType = dbRecord.findDBField(pvScanType);
            pvScanTypeIndex = scanField.getScanTypeIndexPV();
            dbScanTypeIndex = dbRecord.findDBField(pvScanTypeIndex);
            pvRate = scanField.getRatePV();
            dbRate = dbRecord.findDBField(pvRate); 
            pvEventName = scanField.getEventNamePV();
            dbEventName = dbRecord.findDBField(pvEventName); 
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginProcess()
         */
        public void beginProcess() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
         */
        public void dataPut(DBField requested, DBField dbField) {
            if(!isStarted || !isActive) return;
            if(dbField.getPVField()!=pvScanTypeIndex) return;
            callScanModify();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
         */
        public void dataPut(DBField dbField) {
            if(!isStarted || !isActive) return;
            if(dbField==dbEventName) {
                if(scanType==ScanType.periodic) return;
            }
            callScanModify();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endProcess()
         */
        public void endProcess() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
         */
        public void unlisten(RecordListener listener) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#getName()
         */
        public String getRequesterName() {
            return supportName;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize(org.epics.ioc.process.SupportCreation)
         */
        public void initialize() {
            if(scanField.getProcessSelf()) {
                pvProcessSelf = new PVProcessSelf(this,dbRecord,scanField);
                if(!pvProcessSelf.becomeProcessor()) {
                    super.message(
                        "could not become recordProcessor",
                        MessageType.fatalError);
                    pvProcessSelf = null;
                    return;
                }
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            stop();
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            setSupportState(SupportState.ready);
            isStarted = true;
            if(isActive) {
                addListeners();
                callScanModify();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            removeListeners();
            isStarted = false;
            setSupportState(SupportState.readyForStart);
            if(scanModify!=null) {
                scanModify.modify();
                scanModify = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#allSupportStarted()
         */
        @Override
        public void allSupportStarted() {
            if(isStarted && !isActive) {
                isActive = true;
                addListeners();
                callScanModify();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.message("process is being called. Why?", MessageType.error);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#canProcessSelf()
         */
        public boolean canProcessSelf() {
            return scanField.getProcessSelf();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfRequest(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean processSelfRequest(RecordProcessRequester recordProcessRequester) {
            if(!scanField.getProcessSelf()) return false;
            return pvProcessSelf.processSelf(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfSetActive()
         */
        public void processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            pvProcessSelf.processSelfSetActive(recordProcessRequester);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfProcess(boolean)
         */
        public void processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive) {
            pvProcessSelf.startScan(recordProcessRequester,leaveActive);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfSetInactive()
         */
        public void processSelfSetInactive(RecordProcessRequester recordProcessRequester) {
            pvProcessSelf.setInactive(recordProcessRequester);
        }
        
        private void addListeners() {
            dbScanType.addListener(recordListener);
            dbRate.addListener(recordListener);
            dbEventName.addListener(recordListener);
        }
        
        private void removeListeners() {
            dbScanType.removeListener(recordListener);
            dbRate.removeListener(recordListener);
            dbEventName.removeListener(recordListener);
        }
        
        private void callScanModify() {
            if(scanModify!=null) {
                scanModify.modify();
            } else {
                scanType = scanField.getScanType();
                scanRate = scanField.getRate();
                scanPriority = scanField.getPriority();
                eventName = scanField.getEventName();
                if(scanType==ScanType.event || scanType==ScanType.periodic) {
                    scanModify = new ScanModify();
                    scanModify.modify();
                }
            }
        }
         
        private class ScanModify implements Runnable {
            private boolean isPeriodic = false;
            private boolean isEvent = false;
            
            private ScanModify() {}
            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                    stopScanner();
                    startScanner();
            }
            
            public void modify() {
                iocExecutor.execute(this);
            }
            
            
            private void startScanner() {
                boolean result = true;
                switch (scanType) {
                case passive: break;
                case event:
                    result = eventScanner.addRecord(dbRecord);
                    if(result) isEvent = true;
                    break;
                case periodic:
                    result = periodicScanner.addRecord(dbRecord);
                    if(result) isPeriodic = true;
                    break;
                }
                update(!result);
            }
            
            private void stopScanner() {
                boolean result = true;
                if(isEvent) {
                    result = eventScanner.removeRecord(dbRecord, eventName, scanPriority);
                    isEvent = false;
                } else if(isPeriodic) {
                    result = periodicScanner.removeRecord(dbRecord, scanRate, scanPriority);
                    isPeriodic = false;
                }
                if(!result && pvScanTypeIndex!=null) {
                    pvScanTypeIndex.put(0);
                    dbScanTypeIndex.postPut();
                }
                update(!result);
            }
            
            private void update(boolean setPassive) {
                dbRecord.lock();
                try {
                    if(setPassive) {
                        pvScanTypeIndex.put(0);
                        dbScanTypeIndex.postPut();
                    }
                    scanType = scanField.getScanType();
                    scanRate = scanField.getRate();
                    scanPriority = scanField.getPriority();
                    eventName = scanField.getEventName();
                } finally {
                    dbRecord.unlock();
                }
            }
        }
    }
    
    private static class PVProcessSelf implements RecordProcessRequester
    {
        private RecordProcess recordProcess = null;
        private ScanField scanField = null;
        private boolean isActive = false;
        private RecordProcessRequester recordProcessRequester = null;
        
        private PVProcessSelf(ScanImpl scanImpl,DBRecord dbRecord,ScanField scanField) {
            recordProcess = dbRecord.getRecordProcess();
            this.scanField = scanField;
        }
        
        private boolean becomeProcessor() {
            return recordProcess.setRecordProcessRequester(this);
        }

        private boolean processSelf(RecordProcessRequester recordProcessRequester) {
            if(!scanField.getProcessSelf()) return false;
            if(isActive) return false;
            isActive = true;
            this.recordProcessRequester = recordProcessRequester;
            return true;
        }
        
        private void processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.setActive(this);
        }
        
        private void startScan(RecordProcessRequester recordProcessRequester,boolean leaveActive) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.process(this, leaveActive, null);
        }
        
        private void setInactive(RecordProcessRequester recordProcessRequester) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.setInactive(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return recordProcess.getRecord().getPVRecord().getRecordName();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            recordProcess.getRecord().getPVRecord().message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            isActive = false;
            RecordProcessRequester recordProcessRequester = this.recordProcessRequester;
            this.recordProcessRequester = null;
            recordProcessRequester.recordProcessComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            recordProcessRequester.recordProcessResult(requestResult);
        }
    }
}
