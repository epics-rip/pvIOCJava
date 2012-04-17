/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.EventAnnounce;
import org.epics.pvioc.util.EventScanner;
import org.epics.pvioc.util.RequestResult;
import org.epics.pvioc.util.ScannerFactory;


/**
 * Support a field which must have type string.
 * The string value is an event name. Each time process is called the event is announced.
 * @author mrk
 *
 */
public class EventFactory {
    /**
     * Create the support for the field.
     * @param pvRecordField The field which must have type string.
     * @return The support instance.
     */
    public static Support create(PVRecordField pvRecordField) {
    	PVField pvField = pvRecordField.getPVField();
        if(pvField.getField().getType()!=Type.scalar) {
            pvField.message("illegal field type. Must be strinng", MessageType.error);
            return null;
        }
        PVScalar pvScalar = (PVScalar)pvField;
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvField.message("illegal field type. Must be strinng", MessageType.error);
            return null;
        }
        return new EventImpl(pvRecordField);
    }
    
    
    static private class EventImpl extends AbstractSupport
    {
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordSupport#processRecord(org.epics.pvioc.process.RecordProcessRequester)
         */
        private static final String supportName = "org.epics.pvioc.event";
        private static final EventScanner eventScanner = ScannerFactory.getEventScanner();
        private SupportState supportState = SupportState.readyForInitialize;
        private final PVString pvEventName;
        private final PVRecord pvRecord;
        private EventAnnounce eventAnnounce = null;
        private String eventName = null;
        
        private EventImpl(PVRecordField pvRecordField) {
            super(supportName,pvRecordField);
            this.pvEventName = (PVString)pvRecordField.getPVField();
            pvRecord = pvRecordField.getPVRecord();
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#start()
         */
        @Override
        public void start(AfterStart afterStart) {
            supportState = SupportState.ready;
            eventName = pvEventName.get();
            if(eventName!=null) {
                eventAnnounce = eventScanner.addEventAnnouncer(eventName, pvRecord.getRecordName());
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#stop()
         */
        @Override
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
         * @see org.epics.pvioc.process.Support#uninitialize()
         */
        @Override
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#process(org.epics.pvioc.process.RecordProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            if(supportState!=SupportState.ready) {
                super.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        MessageType.error);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
            }
            String newName = pvEventName.get();
            if(eventName!=null && !eventName.equals(newName)) {
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
