/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.rpc;

import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.property.PVEnumerated;
import org.epics.pvdata.property.PVEnumeratedFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.EventScanner;
import org.epics.pvioc.util.PeriodicScanner;
import org.epics.pvioc.util.RequestResult;
import org.epics.pvioc.util.ScannerFactory;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class IocShowFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvRecordStructure The processControlStructure
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new IocShowImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.pvioc.rpc.iocShow";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final String newLine = String.format("%n");
    private static final Runtime runTime = Runtime.getRuntime();
    
    private static class IocShowImpl extends AbstractSupport
    {
    	private final PVRecordStructure pvRecordStructure;
        private PVEnumerated command = PVEnumeratedFactory.create();
        private PVString pvResult = null;
        private StringBuilder stringBuilder = new StringBuilder();
        private StringBuilder subStringBuilder = new StringBuilder();
        
        private IocShowImpl(PVRecordStructure pvRecordStructure) {
            super(IocShowFactory.supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize(org.epics.pvioc.support.RecordSupport)
         */
        @Override
        public void initialize() {
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            PVStructure pvTemp = pvStructure.getStructureField("argument.command");
            if(pvTemp==null) return;
            if(!command.attach(pvTemp)) {
                super.message("argument.command is not enumerated", MessageType.error);
                return;
            }
            pvResult = pvStructure.getStringField("result.value");
            if(pvResult==null) return;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
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
                RecordProcess recordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
                boolean isActive = recordProcess.isActive();
                boolean isEnabled = recordProcess.isEnabled();
                SupportState supportState = recordProcess.getSupportState();
                int alarmSeverity = 0;
                PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("alarm.severity");
                if(pvField!=null) {
                    PVInt pvint = pvRecord.getPVRecordStructure().getPVStructure().getIntField("alarm.severity");
                    if(pvint!=null) alarmSeverity = pvint.get(); 
                }
                String alarmMessage = "";
                pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("alarm.message");
                if(pvField!=null) {
                    PVString pvString = pvRecord.getPVRecordStructure().getPVStructure().getStringField("alarm.message");
                    if(pvString!=null) alarmMessage = pvString.get();
                }
                if(isActive) subStringBuilder.append(" isActive");
                if(!isEnabled) subStringBuilder.append(" disabled");
                if(supportState!=SupportState.ready) subStringBuilder.append(" supportState " + supportState.name());
                if(alarmSeverity>0) subStringBuilder.append(" alarmSeverity " + alarmSeverity);
                if(alarmMessage!=null && alarmMessage.length()>0) {
                    subStringBuilder.append(" alarmMessage " + alarmMessage);
                }
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
