/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class DoubleRecordFactory {
    public static Support create(PVStructure pvStructure) {
        return new DoubleRecordSupport(pvStructure);
    }
    
    private enum ProcessState {
        input,
        linkArraySupport
    }
    
    static private class DoubleRecordSupport extends AbstractSupport
    implements SupportProcessRequestor
    {
        private static String supportName = "doubleRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private PVLink link = null;
        private PVData value = null;
        private LinkSupport support = null;
        private LinkSupport linkArraySupport = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private ProcessState processState = ProcessState.input;
        
        private DoubleRecordSupport(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
            dbRecord = (DBRecord)pvStructure.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            dbRecord.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVAccess pvAccess = PVAccessFactory.createPVAccess(dbRecord);
            PVData pvData;
            pvAccess.findField(null);
            AccessSetResult result = pvAccess.findField("input");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field input does not exist",
                        MessageType.error);
                return;
            }
            pvData = pvAccess.getField();
            if(pvData.getField().getType()!=Type.pvLink) {
                dbRecord.message(
                        "field input is not a link",
                        MessageType.error);
                return;
            }
            link = (PVLink)pvData;
            pvAccess.findField(null);
            result = pvAccess.findField("value");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field value does not exist",
                        MessageType.error);
                return;
            }
            pvData = pvAccess.getField();
            if(!pvData.getField().getType().isNumeric()) {
                dbRecord.message(
                        "field value is not numeric",
                        MessageType.error);
                return;
            }
            value = pvData;
            support = (LinkSupport)((DBData)link).getSupport();
            if(support!=null) {
                support.initialize();
                supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
                support.setField(value);
            } else {
                supportState = SupportState.readyForStart;
            }
            pvAccess.findField(null);
            result = pvAccess.findField("linkArray");
            if(result==AccessSetResult.thisRecord) {
                DBData dbData = (DBData)pvAccess.getField();
                linkArraySupport = (LinkSupport)dbData.getSupport();
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
         * @see org.epics.ioc.process.Support#start()
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
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            supportState = SupportState.readyForStart;
            if(support!=null) support.stop();
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
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
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            if(support!=null) {
                processState = ProcessState.input;
                support.process(this);
            } else if(linkArraySupport!=null) {
                processState = ProcessState.linkArraySupport;
                linkArraySupport.process(this);
            } else {
                supportProcessRequestor.supportProcessDone(RequestResult.success);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(processState==ProcessState.linkArraySupport
            || requestResult!=RequestResult.success
            || linkArraySupport==null) {
                supportProcessRequestor.supportProcessDone(requestResult);
                return;
            }
            processState = ProcessState.linkArraySupport;
            linkArraySupport.process(this);
        }
    }
}
