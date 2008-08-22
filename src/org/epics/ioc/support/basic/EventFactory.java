/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.db.DBField;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.EventAnnounce;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScannerFactory;

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
        private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
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
            DBField dbParent = dbField.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvField = pvProperty.findProperty(pvParent, "value");
            if(pvField==null) {
                pvParent.message("value field not found", MessageType.error);
                return;
            }
            DBField valueDBField = dbField.getDBRecord().findDBField(pvField);
            pvField = valueDBField.getPVField();
            if(pvField.getField().getType()!=Type.pvString) {
                super.message("illegal field type. Must be strinng", MessageType.error);
            }
            pvEventName = (PVString)pvField;
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
    }
}
