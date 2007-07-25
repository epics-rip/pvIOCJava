/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.util.*;

/**
 * Support a field which must have type string.
 * The string value is an event name. Each time process is called the event is announced.
 * @author mrk
 *
 */
public class EventFactory {
    /**
     * Create the support for the field.
     * @param dbField The field which must have type string.
     * @return The support instance.
     */
    public static Support create(DBField dbField) {
        return new EventImpl(dbField);
    }
    
    
    static private class EventImpl extends AbstractSupport
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordSupport#processRecord(org.epics.ioc.process.RecordProcessRequester)
         */
        private static String supportName = "event";
        private SupportState supportState = SupportState.readyForInitialize;
        private PVRecord pvRecord;
        DBField dbField;
        private PVString pvEventName = null;
        private EventScanner eventScanner = null;
        private EventAnnounce eventAnnounce = null;
        private String eventName = null;
        
        private EventImpl(DBField dbField) {
            super(supportName,dbField);
            this.dbField = dbField;
            pvRecord = dbField.getDBRecord().getPVRecord();
            eventScanner = ScannerFactory.getEventScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(pvEventName==null) {
                PVField pvField = dbField.getPVField();
                if(pvField.getField().getType()!=Type.pvString) {
                    super.message("field " + pvField.getFullName()
                        + " is not a string",MessageType.error);
                    return;
                }
                pvEventName = (PVString)pvField;
            }
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            supportState = SupportState.ready;
            eventName = pvEventName.get();
            if(eventName!=null) {
                eventAnnounce = eventScanner.addEventAnnouncer(eventName, pvRecord.getRecordName());
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(eventName!=null) {
                eventScanner.removeEventAnnouncer(eventAnnounce, pvRecord.getRecordName());
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
            pvEventName = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(supportState!=SupportState.ready) {
                super.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        MessageType.error);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
            }
            String newName = pvEventName.get();
            if(newName!=eventName) {
                eventScanner.removeEventAnnouncer(eventAnnounce, pvRecord.getRecordName());
                eventAnnounce = null;
                eventName = newName;
                if(eventName!=null) {
                    eventAnnounce = eventScanner.addEventAnnouncer(eventName, pvRecord.getRecordName());
                }
            }
            if(eventAnnounce!=null) eventAnnounce.announce();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvString) {
                super.message("illegal field type. Must be strinng", MessageType.error);
            }
            pvEventName = (PVString)pvField;
        }
    }
}
