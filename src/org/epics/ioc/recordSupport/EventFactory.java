/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.recordSupport;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.util.*;

/**
 * Record that holds a double pvEventName, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class EventFactory {
    public static Support create(DBStructure dbStructure) {
        return new EventImpl(dbStructure);
    }
    
    
    static private class EventImpl extends AbstractSupport
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordSupport#processRecord(org.epics.ioc.process.RecordProcessRequestor)
         */
        private static String supportName = "event";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private PVString pvEventName = null;
        private EventScanner eventScanner = null;
        private EventAnnounce eventAnnounce = null;
        private String eventName = null;
        
        private EventImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getDBRecord();
            pvRecord = dbRecord.getPVRecord();
            eventScanner = ScannerFactory.getEventScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(pvEventName==null) {
                PVField[] pvFields = pvRecord.getFieldPVFields();
                Structure structure = (Structure)pvRecord.getField();
                int index;
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("field pvEventName does not exist",MessageType.error);
                    return;
                }
                PVField pvField = pvFields[index];
                if(pvField.getField().getType()!=Type.pvString) {
                    super.message("field pvEventName is not a string",MessageType.error);
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
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(supportState!=SupportState.ready) {
                super.message(
                        "process called but supportState is "
                        + supportState.toString(),
                        MessageType.error);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
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
            supportProcessRequestor.supportProcessDone(RequestResult.success);
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
