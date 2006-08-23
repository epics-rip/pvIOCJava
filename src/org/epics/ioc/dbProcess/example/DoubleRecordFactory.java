/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.support.ProcessLinkArrayFactory;
import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class DoubleRecordFactory {
    public static Support create(DBStructure dbStructure) {
        return new DoubleRecordSupport(dbStructure);
    }
    
    private enum ProcessState {
        input,
        processLinkArray
    }
    
    static private class DoubleRecordSupport extends AbstractSupport implements ProcessCompleteListener{
        private static String supportName = "doubleRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private DBLink link = null;
        private DBData value = null;
        private LinkSupport support = null;
        private ProcessCompleteListener processListener = null;
        private Support processLinkArray = null;
        private ProcessState processState = ProcessState.input;
        private ProcessResult result = ProcessResult.success;
        
        private DoubleRecordSupport(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getRecord();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            IOCDB iocdb = dbRecord.getRecordProcess().getProcessDB().getIOCDB();
            DBAccess dbAccess = iocdb.createAccess(dbRecord.getRecordName());
            DBData dbData;
            AccessSetResult result = dbAccess.setField("input");
            if(result!=AccessSetResult.thisRecord) {
                errorMessage("field input does not exists");
                return;
            }
            dbData = dbAccess.getField();
            if(dbData.getDBDField().getDBType()!=DBType.dbLink) {
                errorMessage("field input is not a link");
                return;
            }
            link = (DBLink)dbData;
            result = dbAccess.setField("value");
            if(result!=AccessSetResult.thisRecord) {
                errorMessage("field value does not exists");
                return;
            }
            dbData = dbAccess.getField();
            if(!dbData.getField().getType().isNumeric()) {
                errorMessage("field value is not numeric");
                return;
            }
            value = dbData;
            support = (LinkSupport)link.getSupport();
            if(support==null) {
                errorMessage("field input does not have support");
                return;
            }
            support.initialize();
            supportState = support.getSupportState();
            if(supportState!=SupportState.readyForStart) return;
            support.setField(value);
            result = dbAccess.setField("process");
            if(result==AccessSetResult.thisRecord) {
                processLinkArray = dbAccess.getField().getSupport();
                if(processLinkArray!=null) {
                    processLinkArray.initialize();
                    supportState = processLinkArray.getSupportState();
                }
                if(supportState!=SupportState.readyForStart) {
                    support.uninitialize();
                    return;
                }
            }
            setSupportState(supportState);
        }
    
        public void start() {
            support.start();
            supportState = support.getSupportState();
            if(supportState==SupportState.readyForStart) {
                if(processLinkArray!=null) {
                    processLinkArray.start();
                    supportState = processLinkArray.getSupportState();
                }
            }
            setSupportState(supportState);
        }
    
        public void stop() {
            supportState = SupportState.readyForStart;
            support.stop();
            if(processLinkArray!=null) processLinkArray.stop();
            setSupportState(supportState);
        }
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            support.uninitialize();
            if(processLinkArray!=null) processLinkArray.uninitialize();
            support = null;
            value = null;
            link = null;
            setSupportState(SupportState.readyForInitialize);
        }
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                        "process called but supportState is "
                        + supportState.toString());
                return ProcessReturn.failure;
            }
            processState = ProcessState.input;
            ProcessReturn processReturn = support.process(this);
            switch(processReturn) {
            case noop:
            case success:
                if(processLinkArray!=null) {
                    processState = ProcessState.processLinkArray;
                    processReturn =  processLinkArray.process(this);
                    if(processReturn==ProcessReturn.active) {
                        processListener = listener;
                        return ProcessReturn.active;
                    }
                }
                return processReturn;
            case failure: return processReturn;
            case active:
            case alreadyActive:
                processListener = listener;
                return processReturn;
            }
            return ProcessReturn.failure;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            if(result==ProcessResult.failure) {
                processListener.processComplete(this,result);
            }
            if(processState==ProcessState.input) {
                processState = ProcessState.processLinkArray;
                ProcessReturn processReturn = processLinkArray.process(this);
                if(processReturn==ProcessReturn.active) return;
                if(processReturn!=ProcessReturn.success && processReturn!=ProcessReturn.noop) {
                    result = ProcessResult.failure;
                }
            }
            processListener.processComplete(this,result);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            this.result = result;
            dbRecord.getRecordProcess().getRecordProcessSupport().processContinue(this);
        }
    }
}
