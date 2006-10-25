/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.*;

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
    
    static private class DoubleRecordSupport extends AbstractSupport implements SupportProcessRequestor
    {
        private static String supportName = "doubleRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink link = null;
        private DBData value = null;
        private LinkSupport support = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private LinkSupport linkArraySupport = null;
        private ProcessState processState = ProcessState.input;
        
        private DoubleRecordSupport(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcessSupport = dbRecord.getRecordProcess().getRecordProcessSupport();
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
            result = dbAccess.setField("linkArray");
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
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(support!=null) {
                if(support.getSupportState()==SupportState.readyForStart) {
                    support.start();
                }
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
            if(super.getSupportState()!=SupportState.ready) return;
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
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public RequestResult process(SupportProcessRequestor supportProcessRequestor) {
            if(supportState!=SupportState.ready) {
                dbRecord.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        IOCMessageType.error);
                return RequestResult.failure;
            }
            processState = ProcessState.input;
            RequestResult requestResult = RequestResult.success;
            if(support!=null) {
                requestResult = support.process(this);
                if(requestResult==RequestResult.active) {
                    this.supportProcessRequestor = supportProcessRequestor;
                    return RequestResult.active;
                }
                if(requestResult!=RequestResult.success) {
                    return requestResult;
                }
            }           
            if(linkArraySupport!=null) {
                processState = ProcessState.linkArraySupport;
                requestResult =  linkArraySupport.process(this);
                if(requestResult==RequestResult.active) {
                    this.supportProcessRequestor = supportProcessRequestor;
                    return RequestResult.active;
                }
                return requestResult;
            }
            return requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            processState = ProcessState.linkArraySupport;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(RequestResult requestResult) {
            if(processState==ProcessState.input) {
                recordProcessSupport.processContinue(this);
                return;
            }
            supportProcessRequestor.processComplete(null);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void processResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do
        }
    }
}
