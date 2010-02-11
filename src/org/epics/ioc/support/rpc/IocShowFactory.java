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
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScannerFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class IocShowFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvStructure The processControlStructure
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVStructure pvStructure) {
        return new IocShowImpl(pvStructure);
    }
    
    private static final String supportName = "org.epics.ioc.rpc.iocShow";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final IOCDatabase masterSupportDatabase = IOCDatabaseFactory.get(masterPVDatabase);
    private static final String newLine = String.format("%n");
    private static final Runtime runTime = Runtime.getRuntime();
    
    private static class IocShowImpl extends AbstractSupport
    {
        private Enumerated command = null;
        private PVString pvResult = null;
        private StringBuilder stringBuilder = new StringBuilder();
        private StringBuilder subStringBuilder = new StringBuilder();
        
        private IocShowImpl(PVStructure pvStructure) {
            super(IocShowFactory.supportName,pvStructure); 
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
            PVStructure pvStructure = (PVStructure)super.getPVField();
            PVStructure pvTemp = pvStructure.getStructureField("arguments.command");
            if(pvTemp==null) return;
            command = EnumeratedFactory.getEnumerated(pvTemp);
            if(command==null) {
                super.message("arguments.command is not enumerated", MessageType.error);
                return;
            }
            pvResult = pvStructure.getStringField("result.value");
            if(pvResult==null) return;
            super.initialize(recordSupport);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            String cmd = command.getChoice();
            if(cmd.equals("showBadRecords")) {
                showBadRecords();
            } else if(cmd.equals("showThreads")) {
                showThreads();
            } else if(cmd.equals("showMemory")) {
                stringBuilder.setLength(0);
                showMemory();
            } else if(cmd.equals("garbageCollect")) {
                garbageCollect();
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
        private void showBadRecords() {
            stringBuilder.setLength(0);
            PVRecord[] pvRecords = masterPVDatabase.getRecords();
            for(PVRecord pvRecord : pvRecords) {
                subStringBuilder.setLength(0);
                RecordProcess recordProcess = masterSupportDatabase.getLocateSupport(pvRecord).getRecordProcess();
                boolean isActive = recordProcess.isActive();
                boolean isEnabled = recordProcess.isEnabled();
                SupportState supportState = recordProcess.getSupportState();
                String alarmSeverity = null;
                PVField pvField = pvRecord.getPVStructure().getSubField("alarm.severity.choice");
                if(pvField!=null) alarmSeverity = pvField.toString();
                String alarmMessage = null;
                pvField = pvRecord.getPVStructure().getSubField("alarm.message");
                if(pvField!=null) alarmMessage = pvField.toString();
                if(isActive) subStringBuilder.append(" isActive");
                if(!isEnabled) subStringBuilder.append(" disabled");
                if(supportState!=SupportState.ready) subStringBuilder.append(" supportState " + supportState.name());
                if(alarmSeverity!=null && !alarmSeverity.equals("none")) subStringBuilder.append(" alarmSeverity " + alarmSeverity);
                if(alarmMessage!=null && !alarmMessage.equals("null") && alarmMessage.length()>0)
                    subStringBuilder.append(" alarmMessage " + alarmMessage);
                if(subStringBuilder.length()>2) {
                    stringBuilder.append(pvRecord.getRecordName());
                    stringBuilder.append(subStringBuilder.toString());
                    stringBuilder.append(newLine);
                }
            }
            pvResult.put(stringBuilder.toString());
        }
        
        private void showThreads() {
            stringBuilder.setLength(0);
            Thread[] threads = ThreadCreateFactory.getThreadCreate().getThreads();
            for(Thread thread : threads) {
                String name = thread.getName();
                int priority = thread.getPriority();
                stringBuilder.append(name + " priority " + priority + newLine);
            }
            PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
            EventScanner eventScanner = ScannerFactory.getEventScanner();
            stringBuilder.append(periodicScanner.toString());
            stringBuilder.append(eventScanner.toString());
            pvResult.put(stringBuilder.toString());
        }
        
        private void showMemory() {
            long free = runTime.freeMemory();
            long total = runTime.totalMemory();
            long max = runTime.maxMemory();
            stringBuilder.append("freeMemory ");
            stringBuilder.append(free);
            stringBuilder.append(" totalMemory ");
            stringBuilder.append(total);
            stringBuilder.append(" maxMemory ");
            stringBuilder.append(max);
            stringBuilder.append(newLine);
            pvResult.put(stringBuilder.toString());
        }
        
        private void garbageCollect() {
            stringBuilder.setLength(0);
            long start = System.currentTimeMillis();
            runTime.gc();
            long end = System.currentTimeMillis();
            stringBuilder.append("time ");
            stringBuilder.append((end-start));
            stringBuilder.append(" milliseconds ");
            showMemory();
        }
    }
}
