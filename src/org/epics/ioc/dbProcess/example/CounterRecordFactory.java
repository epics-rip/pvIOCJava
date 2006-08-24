/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;

/**
 * @author mrk
 *
 */
public class CounterRecordFactory {
    public static Support create(DBStructure dbStructure) {
        return new CounterRecord(dbStructure);
    }
    
    private static class CounterRecord extends AbstractSupport implements ProcessCompleteListener {
        private static String supportName = "counterRecord";
        private DBRecord dbRecord = null;
        private SupportState supportState = SupportState.readyForInitialize;
        private DBDouble dbMin = null;
        private DBDouble dbMax = null;
        private DBDouble dbInc = null;
        private DBDouble dbValue = null;
        private DBArray dbProcess = null;
        
        private Support processLinkArray = null;
        private ProcessCompleteListener listener = null;
        private ProcessResult result = ProcessResult.success;
        
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
    
        public void initialize() {
            supportState = SupportState.readyForStart;
            processLinkArray = dbProcess.getSupport();
            if(processLinkArray!=null) {
                processLinkArray.initialize();
                supportState = processLinkArray.getSupportState();
            }
            setSupportState(supportState);
        }
    
        public void start() {
            supportState = SupportState.ready;
            if(processLinkArray!=null) {
                processLinkArray.start();
                supportState = processLinkArray.getSupportState();
            }
            setSupportState(supportState);
        }
    
        public void stop() {
            supportState = SupportState.readyForStart;
            if(processLinkArray!=null) processLinkArray.stop();
            setSupportState(supportState);
        }
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            if(processLinkArray!=null) processLinkArray.uninitialize();
            processLinkArray = null;
            setSupportState(supportState);
        }
    
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                    "process called but supportState is "
                    + supportState.toString());
                return ProcessReturn.failure;
            }
            double min = dbMin.get();
            double max = dbMax.get();
            double inc = dbInc.get();
            double value = dbValue.get();
            value += inc;
            if(value>max) value = min;
            dbValue.put(value);
            this.listener = listener;
            if(processLinkArray!=null) {
                return processLinkArray.process(this);
            }
            return ProcessReturn.success;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            if(listener!=null) listener.processComplete(this,result);
        }

        public void processComplete(Support support,ProcessResult result) {
            this.result = result;
            dbRecord.getRecordProcess().getRecordProcessSupport().processContinue(this);
        }
    }
}
