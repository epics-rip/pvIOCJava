/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.process.ProcessCallbackRequester;
import org.epics.ioc.process.ProcessContinueRequester;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class SupportStateFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param dbField The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(DBStructure dbField) {
        return new SupportStateImpl(dbField);
    }
    
    private static String supportName = "supportState";
    
    
    private static class SupportStateImpl extends AbstractSupport
    implements ProcessCallbackRequester,ProcessContinueRequester
    {
        private IOCDB masterIOCDB = IOCDBFactory.getMaster();
        private PVString pvnamePVString = null;
        private Enumerated supportStateEnumerated = null;
        private PVInt supportStatePVInt = null;
        private DBField supportStateDBField = null;
        private Enumerated supportStateCommandEnumerated = null;
        private PVInt supportStateCommandPVInt = null;
        private DBField supportStateCommandDBField = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;
        
        private String pvnamePrevious = null;
        
        private DBRecord pvnameDBRecord = null;
        private DBField pvnameDBField = null;
        private Support pvnameSupport = null;
        
        private SupportStateCommand supportStateCommand = null;
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;
        private String errorMessage = null;
        private SupportState supportState = null;
       
        private SupportStateImpl(DBStructure dbField) {
            super(supportName,dbField);
            
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            DBField dbField = super.getDBField();
            DBRecord dbRecord = dbField.getDBRecord();
            PVField pvField = dbField.getPVField();
            String name = pvField.getSupportName();
            if(!name.equals(supportName)) {
                pvField.message("support " + name + " is not " + supportName, MessageType.fatalError);
                return;
            }
            DBField temp = dbField.getParent();
            if(!(temp instanceof DBStructure)) {
                pvField.message("parent is not a structure", MessageType.fatalError);
                return;
            }
            DBStructure dbParent = (DBStructure)temp;
            PVStructure pvParent = dbParent.getPVStructure();
            alarmSupport = AlarmFactory.findAlarmSupport(dbParent);
            if(alarmSupport==null) {
                pvParent.message("no alarmSupport", MessageType.error);
                return;
            }
            pvnamePVString = pvParent.getStringField("pvname");
            if(pvnamePVString==null) return;
            PVStructure pvStructure = pvParent.getStructureField("supportState", "supportState");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateEnumerated = SupportState.getSupportState(dbField);
            if(supportStateEnumerated==null) return;
            supportStatePVInt = supportStateEnumerated.getIndexField();
            supportStateDBField = dbRecord.findDBField(supportStatePVInt);
            pvStructure = pvParent.getStructureField("value", "supportStateCommand");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateCommandEnumerated = SupportStateCommand.getSupportStateCommand(dbField);
            if(supportStateCommandEnumerated==null) return;
            supportStateCommandPVInt = supportStateCommandEnumerated.getIndexField();
            supportStateCommandDBField = dbRecord.findDBField(supportStateCommandPVInt);
            recordProcess = dbRecord.getRecordProcess();
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            String pvName = pvnamePVString.get();
            if(pvName==null) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            supportState = null;
            requestResult = RequestResult.success;
            supportStateCommand = SupportStateCommand.getSupportStateCommand(
                    supportStateCommandPVInt.get());
            recordProcess.requestProcessCallback(this);
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            String pvName = pvnamePVString.get();
            if(!pvName.equals(pvnamePrevious)) {
                String recordName = pvName;
                String fieldName = null;
                int index = pvName.indexOf('.');
                if(index>=0) {
                    recordName = pvName.substring(0, index);
                    fieldName = pvName.substring(index+1);
                }
                pvnameDBRecord = masterIOCDB.findRecord(recordName);
                if(pvnameDBRecord==null) {
                    requestResult = RequestResult.failure;
                    errorMessage = "recordName " + recordName + " not found";
                    recordProcess.processContinue(this);
                    return;
                }
                pvnameDBRecord.lock();
                try {
                    if(fieldName==null) {
                        pvnameDBField = pvnameDBRecord.getDBStructure();
                    } else {
                        PVField pvField = pvnameDBRecord.getPVRecord().findProperty(fieldName);
                        if(pvField==null) {
                            requestResult = RequestResult.failure;
                            errorMessage = "fieldName " + fieldName + " not found";
                            recordProcess.processContinue(this);
                            return;
                        }
                        pvnameDBField = pvnameDBRecord.findDBField(pvField);
                    }
                    pvnameSupport = pvnameDBField.getSupport();
                    if(pvnameSupport==null) {
                        requestResult = RequestResult.failure;
                        errorMessage = "support for "
                            + pvnameDBField.getPVField().getFullName()
                            + " not found";
                        recordProcess.processContinue(this);
                        return;
                    }
                } finally {
                    pvnameDBRecord.unlock();
                }
            }
            if(pvnameDBRecord==null) {
                requestResult = RequestResult.failure;
                errorMessage = "pvnameDBRecord is null";
                recordProcess.processContinue(this);
                return;
            }
            if(pvnameSupport==null) {
                requestResult = RequestResult.failure;
                errorMessage = "pvname support is null";
                recordProcess.processContinue(this);
                return;
            }
            pvnameDBRecord.lock();
            try {
                supportState = pvnameSupport.getSupportState();
                switch(supportStateCommand) {
                case idle:
                    break;
                case initialize:
                    if(supportState!=SupportState.readyForInitialize) {
                        pvnameSupport.uninitialize();
                    }
                    pvnameSupport.initialize(); break;
                case start:
                    if(supportState!=SupportState.readyForStart) {
                        if(supportState==SupportState.ready) {
                            pvnameSupport.stop();
                        } else if(supportState==SupportState.readyForInitialize) {
                            pvnameSupport.initialize();
                        }
                        supportState = pvnameSupport.getSupportState();
                        if(supportState!=SupportState.readyForStart) {
                            requestResult = RequestResult.failure;
                            errorMessage = "support is not readyForStart";
                            recordProcess.processContinue(this);
                            return;
                        }
                    }
                    pvnameSupport.start(); break;
                case stop:
                    if(supportState!=SupportState.ready) {
                        requestResult = RequestResult.failure;
                        errorMessage = "support is not ready";
                        recordProcess.processContinue(this);
                        return;
                    }
                    pvnameSupport.stop(); break;
                case uninitialize:
                    pvnameSupport.uninitialize(); break;
                }
                supportState = pvnameSupport.getSupportState();
                recordProcess.processContinue(this);
            } finally {
                pvnameDBRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(requestResult!=RequestResult.success) {
                alarmSupport.setAlarm(errorMessage, AlarmSeverity.major);
            }
            if(supportStateCommand!=SupportStateCommand.idle) {
                supportStateCommandPVInt.put(0);
                supportStateCommandDBField.postPut();
            }
            if(supportState!=null) {
                int currentIndex = supportStatePVInt.get();
                int newIndex = supportState.ordinal();
                if(currentIndex!=newIndex) {
                    supportStatePVInt.put(newIndex);
                    supportStateDBField.postPut();
                }
            }
            supportProcessRequester.supportProcessDone(requestResult);
        }
        
        private enum SupportStateCommand {
            idle, initialize, start, stop, uninitialize;
            
            public static SupportStateCommand getSupportStateCommand(int value) {
                switch(value) {
                case 0: return SupportStateCommand.idle;
                case 1: return SupportStateCommand.initialize;
                case 2: return SupportStateCommand.start;
                case 3: return SupportStateCommand.stop;
                case 4: return SupportStateCommand.uninitialize;
                }
                throw new IllegalArgumentException("SupportStateCommand getSupportStateCommand("
                    + ((Integer)value).toString() + ") is not a valid SupportStateCommand");
            }
            
            private static final String[] supportStateCommandChoices = {
                "idle", "initialize", "start", "stop", "uninitialize"
            };
            /**
             * Convenience method for code that accesses a supportStateCommand structure.
             * @param dbField A field which is potentially a supportStateCommand structure.
             * @return The Enumerated interface only if dbField has an Enumerated interface and defines
             * the supportStateCommand choices.
             */
            public static Enumerated getSupportStateCommand(DBField dbField) {
                PVField pvField = dbField.getPVField();
                if(pvField.getField().getType()!=Type.pvStructure) {
                    pvField.message("field is not a structure", MessageType.error);
                    return null;
                }
                DBStructure dbStructure = (DBStructure)dbField;
                Create create = dbStructure.getCreate();
                if(create==null || !(create instanceof Enumerated)) {
                    pvField.message("interface Enumerated not found", MessageType.error);
                    return null;
                }
                Enumerated enumerated = (Enumerated)create;
                PVStringArray pvChoices = enumerated.getChoicesField();
                int len = pvChoices.getLength();
                if(len!=supportStateCommandChoices.length) {
                    pvField.message("not an supportStateCommand structure", MessageType.error);
                    return null;
                }
                StringArrayData data = new StringArrayData();
                pvChoices.get(0, len, data);
                String[] choices = data.data;
                for (int i=0; i<len; i++) {
                    if(!choices[i].equals(supportStateCommandChoices[i])) {
                        pvField.message("not an supportStateCommand structure", MessageType.error);
                        return null;
                    }
                }
                pvChoices.setMutable(false);
                return enumerated;
            }
        }

    }
}
