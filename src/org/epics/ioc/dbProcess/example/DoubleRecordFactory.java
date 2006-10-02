/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.util.IOCMessageType;

/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class DoubleRecordFactory {
    public static Support create(DBStructure dbStructure) {
        return new DoubleRecordSupport(dbStructure);
    }
    
    private enum ProcessState {
        input,
        linkArraySupport
    }
    
    static private class DoubleRecordSupport extends AbstractSupport implements ProcessCompleteListener{
        private static String supportName = "doubleRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private DBLink link = null;
        private DBData value = null;
        private LinkSupport support = null;
        private ProcessCompleteListener processListener = null;
        private LinkSupport linkArraySupport = null;
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
            IOCDB iocdb = dbRecord.getIOCDB();
            DBAccess dbAccess = iocdb.createAccess(dbRecord.getRecordName());
            DBData dbData;
            AccessSetResult result = dbAccess.setField("input");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field input does not exist",
                        IOCMessageType.error);
                return;
            }
            dbData = dbAccess.getField();
            if(dbData.getDBDField().getDBType()!=DBType.dbLink) {
                dbRecord.message(
                        "field input is not a link",
                        IOCMessageType.error);
                return;
            }
            link = (DBLink)dbData;
            result = dbAccess.setField("value");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field value does not exist",
                        IOCMessageType.error);
                return;
            }
            dbData = dbAccess.getField();
            if(!dbData.getField().getType().isNumeric()) {
                dbRecord.message(
                        "field value is not numeric",
                        IOCMessageType.error);
                return;
            }
            value = dbData;
            support = (LinkSupport)link.getSupport();
            if(support!=null) {
                support.initialize();
                supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
                support.setField(value);
            } else {
                supportState = SupportState.readyForStart;
            }
            result = dbAccess.setField("process");
            if(result==AccessSetResult.thisRecord) {
                linkArraySupport = (LinkSupport)dbAccess.getField().getSupport();
                if(linkArraySupport!=null) {
                    linkArraySupport.setField(value);
                    linkArraySupport.initialize();
                    supportState = linkArraySupport.getSupportState();
                }
                if(supportState!=SupportState.readyForStart) {
                    if(support!=null) support.uninitialize();
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(support!=null) {
                support.start();
                supportState = support.getSupportState();
            } else {
                supportState = SupportState.ready;
            }
            if(supportState==SupportState.ready) {
                if(linkArraySupport!=null) {
                    linkArraySupport.start();
                    supportState = linkArraySupport.getSupportState();
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            supportState = SupportState.readyForStart;
            if(support!=null) support.stop();
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            if(support!=null) support.uninitialize();
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            support = null;
            value = null;
            link = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                dbRecord.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        IOCMessageType.error);
                return ProcessReturn.failure;
            }
            processState = ProcessState.input;
            ProcessReturn processReturn = ProcessReturn.success;
            if(support!=null) processReturn = support.process(this);
            switch(processReturn) {
            case noop:
            case success:
                if(linkArraySupport!=null) {
                    processState = ProcessState.linkArraySupport;
                    processReturn =  linkArraySupport.process(this);
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
                processState = ProcessState.linkArraySupport;
                ProcessReturn processReturn = linkArraySupport.process(this);
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
