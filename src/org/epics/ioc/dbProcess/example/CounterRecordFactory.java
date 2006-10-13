/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.*;

/**
 * Counter record acts as a counter from a min to max value with a specified increment.
 * @author mrk
 *
 */
public class CounterRecordFactory {
    public static Support create(DBStructure dbStructure) {
        return new CounterRecord(dbStructure);
    }
    
    private static class CounterRecord extends AbstractSupport implements ProcessRequestListener {
        private static String supportName = "counterRecord";
        private DBRecord dbRecord = null;
        private DBDouble dbMin = null;
        private DBDouble dbMax = null;
        private DBDouble dbInc = null;
        private DBDouble dbValue = null;
        private DBArray dbProcess = null;
        
        private LinkSupport linkArraySupport = null;
        private ProcessRequestListener listener = null;
        
        private CounterRecord(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getRecord();
            DBData[] dbData = dbStructure.getFieldDBDatas();
            int index;
            index = dbStructure.getFieldDBDataIndex("min");
            if(index<0) throw new IllegalStateException("field min does not exist");
            dbMin = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("max");
            if(index<0) throw new IllegalStateException("field max does not exist");
            dbMax = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("inc");
            if(index<0) throw new IllegalStateException("field inc does not exist");
            dbInc = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("value");
            if(index<0) throw new IllegalStateException("field value does not exist");
            dbValue = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("process");
            if(index<0) throw new IllegalStateException("field process does not exist");
            dbProcess = (DBArray)dbData[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,"initialize")) return;
            linkArraySupport = (LinkSupport)dbProcess.getSupport();
            if(linkArraySupport!=null) {
                linkArraySupport.setField(dbValue);
                linkArraySupport.initialize();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.readyForStart);
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.ready);
            }
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            linkArraySupport = null;
            setSupportState(SupportState.readyForInitialize);
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,"process")) return ProcessReturn.failure;
            double min = dbMin.get();
            double max = dbMax.get();
            double inc = dbInc.get();
            double value = dbValue.get();
            value += inc;
            if(value>max) value = min;
            dbValue.put(value);
            this.listener = listener;
            if(linkArraySupport!=null) {
                return linkArraySupport.process(this);
            }
            return ProcessReturn.success;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public ProcessContinueReturn processContinue() {
            dbRecord.message("why was processContinue called", IOCMessageType.error);
            return ProcessContinueReturn.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete() {
            if(listener!=null) listener.processComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do 
        }
    }
}
