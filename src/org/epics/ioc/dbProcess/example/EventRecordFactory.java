/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class EventRecordFactory {
    public static Support create(DBStructure dbStructure) {
        return new EventRecordSupport(dbStructure);
    }
    
    
    static private class EventRecordSupport extends AbstractSupport
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordSupport#processRecord(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        private static String supportName = "eventRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private DBString value = null;
        private EventScanner eventScanner = null;
        private EventAnnounce eventAnnounce = null;
        private String eventName = null;
        
        private EventRecordSupport(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getRecord();
            eventScanner = ScannerFactory.getEventScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getProcessRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            IOCDB iocdb = dbRecord.getIOCDB();
            DBAccess dbAccess = iocdb.createAccess(dbRecord.getRecordName());
            DBData dbData;
            AccessSetResult result = dbAccess.setField("value");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field value does not exist",
                        MessageType.error);
                return;
            }
            dbData = dbAccess.getField();
            if(dbData.getField().getType()!=Type.pvString) {
                dbRecord.message(
                        "field value is not a string",
                        MessageType.error);
                return;
            }
            value = (DBString)dbData;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
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
         * @see org.epics.ioc.dbProcess.Support#stop()
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
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
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
