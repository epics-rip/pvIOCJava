/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFactory {
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
    implements ScanSupport,ScanFieldModifyListener
    {
        private ScanField scanField;
        private DBRecord dbRecord = null;
        private boolean isActive = false;
        private PVProcessSelf pvProcessSelf;
        
        private ScanImpl(DBStructure dbScan,ScanField scanField) {
            super(supportName,dbScan);
            this.scanField = scanField;
            dbRecord = dbScan.getDBRecord();
            pvProcessSelf = new PVProcessSelf(this,dbRecord,scanField);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanFieldModifyListener#modified()
         */
        public void modified() {
            stopScanner();
            startScanner();
        }
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
            scanField.addModifyListener(this);
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(isActive) {
                stopScanner();
                isActive = false;
            }
            scanField.removeModifyListener(this);
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#allSupportStarted()
         */
        @Override
        public void allSupportStarted() {
            isActive = true;
            startScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            dbRecord.getPVRecord().message("process is being called. Why?", MessageType.error);
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
        
        private void startScanner() {
            if(!isActive) return;
            ScanType scanType = scanField.getScanType();
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.addRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.schedule(dbRecord);
                break;
            }
        }
        
        private void stopScanner() {
            if(!isActive) return;
            ScanType scanType = scanField.getScanType();
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.removeRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.unschedule(dbRecord);
                break;
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
        
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            // TODO Auto-generated method stub
            
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
