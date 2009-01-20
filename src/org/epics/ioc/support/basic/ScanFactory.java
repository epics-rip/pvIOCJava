/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;
import org.epics.ioc.util.*;

import org.epics.ioc.util.EventScanner;

import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanField;
import org.epics.ioc.util.ScanFieldFactory;
import org.epics.ioc.util.ScanType;
import org.epics.ioc.util.ScannerFactory;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFactory {
    private static Executor executor 
        = ExecutorFactory.create("scanFieldModify", ThreadPriority.lower);
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    /**
     * Create support for the scan field.
     * @param pvStructure The interface to the scan field.
     * @return The support or null if the scan field is improperly defined.
     */
    public static Support create(PVStructure pvStructure) {
        ScanField  scanField = ScanFieldFactory.create(pvStructure.getPVRecord());
        if(scanField==null) return null;
        return new ScanImpl(pvStructure,scanField);
    }
    
    private static class ScanImpl extends AbstractSupport implements PVListener
    {
        private ScanField scanField;
        private PVRecord pvRecord = null;
        
        private boolean isActive = false;
        private boolean isStarted = false;
        
        private PVStructure pvScanType;
        private PVInt pvScanTypeIndex;
        
        private PVDouble pvRate;
        private PVString pvEventName;
        
        private ScanType scanType = null;
        private double scanRate;
        private ThreadPriority threadPriority = null;
        private String eventName = null;
        private ScanModify scanModify = null;
        
        private ScanImpl(PVStructure pvScan,ScanField scanField) {
            super("scan",pvScan);
            this.scanField = scanField;
            pvRecord = pvScan.getPVRecord();
            pvRecord.registerListener(this);
            pvScanType = pvScan.getStructureField("type");
            pvScanTypeIndex = scanField.getScanTypeIndexPV();
            pvRate = scanField.getRatePV();
            pvEventName = scanField.getEventNamePV(); 
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
         */
        public void beginGroupPut(PVRecord pvRecord) {}

        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
         */
        public void dataPut(PVStructure requested, PVField pvField) {
            if(!isStarted || !isActive) return;
            if(pvField!=pvScanTypeIndex) return;
            callScanModify();
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
         */
        public void endGroupPut(PVRecord pvRecord) {}

        public void dataPut(PVField pvField) {
            if(!isStarted || !isActive) return;
            if(pvField==pvEventName) {
                if(scanType==ScanType.periodic) return;
            }
            callScanModify();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
         */
        public void unlisten(PVRecord pvRecord) {}
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#getName()
         */
        public String getRequesterName() {
            return supportName;
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
        
        private void addListeners() {
            pvScanType.addListener(this);
            pvRate.addListener(this);
            pvEventName.addListener(this);
        }
        
        private void removeListeners() {
            pvScanType.removeListener(this);
            pvRate.removeListener(this);
            pvEventName.removeListener(this);
        }
        
        private void callScanModify() {
            if(scanModify!=null) {
                scanModify.modify();
            } else {
                scanType = scanField.getScanType();
                scanRate = scanField.getRate();
                threadPriority = scanField.getPriority();
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
                executor.execute(this);
            }
            
            
            private void startScanner() {
                if(!isActive || !isStarted) return;
                boolean result = true;
                switch (scanType) {
                case passive: break;
                case event:
                    result = eventScanner.addRecord(pvRecord);
                    if(result) isEvent = true;
                    break;
                case periodic:
                    result = periodicScanner.addRecord(pvRecord);
                    if(result) isPeriodic = true;
                    break;
                }
                update(!result);
            }
            
            private void stopScanner() {
                boolean result = true;
                if(isEvent) {
                    result = eventScanner.removeRecord(pvRecord, eventName, threadPriority);
                    isEvent = false;
                } else if(isPeriodic) {
                    result = periodicScanner.removeRecord(pvRecord, scanRate, threadPriority);
                    isPeriodic = false;
                }
                if(!result && pvScanTypeIndex!=null) {
                    pvScanTypeIndex.put(0);
                }
                update(!result);
            }
            
            private void update(boolean setPassive) {
                pvRecord.lock();
                try {
                    if(setPassive) {
                        pvScanTypeIndex.put(0);
                    }
                    scanType = scanField.getScanType();
                    scanRate = scanField.getRate();
                    threadPriority = scanField.getPriority();
                    eventName = scanField.getEventName();
                } finally {
                    pvRecord.unlock();
                }
            }
        }
    }
}
