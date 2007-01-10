/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class EventRecordFactory {
    public static Support create(PVStructure pvStructure) {
        return new EventRecordSupport(pvStructure);
    }
    
    
    static private class EventRecordSupport extends AbstractSupport
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordSupport#processRecord(org.epics.ioc.process.RecordProcessRequestor)
         */
        private static String supportName = "eventRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private PVString value = null;
        private EventScanner eventScanner = null;
        private EventAnnounce eventAnnounce = null;
        private String eventName = null;
        
        private EventRecordSupport(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
            dbRecord = (DBRecord)pvStructure.getPVRecord();
            eventScanner = ScannerFactory.getEventScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getProcessRequestorName() {
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
            PVAccess pvAccess = PVAccessFactory.createPVAccess(dbRecord);
            PVData pvData;
            pvAccess.findField(null);
            AccessSetResult result = pvAccess.findField("value");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field value does not exist",
                        MessageType.error);
                return;
            }
            pvData = pvAccess.getField();
            if(pvData.getField().getType()!=Type.pvString) {
                dbRecord.message(
                        "field value is not a string",
                        MessageType.error);
                return;
            }
            value = (PVString)pvData;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            supportState = SupportState.ready;
            eventName = value.get();
            if(eventName!=null) {
                eventAnnounce = eventScanner.addEventAnnouncer(eventName, dbRecord.getRecordName());
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(eventName!=null) {
                eventScanner.removeEventAnnouncer(eventAnnounce, dbRecord.getRecordName());
                eventAnnounce = null;
                eventName = null;
            }
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(supportState!=SupportState.ready) {
                dbRecord.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        MessageType.error);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
            }
            String newName = value.get();
            if(newName!=eventName) {
                eventScanner.removeEventAnnouncer(eventAnnounce, dbRecord.getRecordName());
                eventAnnounce = null;
                eventName = newName;
                if(eventName!=null) {
                    eventAnnounce = eventScanner.addEventAnnouncer(eventName, dbRecord.getRecordName());
                }
            }
            if(eventAnnounce!=null) eventAnnounce.announce();
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
    }
}
