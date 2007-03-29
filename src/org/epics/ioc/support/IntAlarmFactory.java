/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for an intAlarm link.
 * @author mrk
 *
 */
public class IntAlarmFactory {
    /**
     * Create support for an intAlarm link.
     * @param dbLink The link.
     * @return An interface to the support or null if the supportName was not "intArray".
     */
    public static Support create(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        String supportName = pvLink.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvLink.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new IntAlarmImpl(dbLink);
    }
    
    private static String supportName = "intAlarm";
    
    private static class IntAlarmImpl extends AbstractLinkSupport
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVInt pvHighMajor;
        private PVInt pvHighMinor;
        private PVInt pvLowMinor;
        private PVInt pvLowMajor;
        private PVInt pvHystersis;
        
        private PVInt pvValue;
        private double lalm;
       
        private IntAlarmImpl(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            SupportState supportState = SupportState.readyForStart;
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            noop = false;
            if(pvValue==null) {
                super.message("setField was not called with an int field", MessageType.error);
                noop = true;
                return;
            }
            PVStructure configStructure = super.getConfigStructure("doubleAlarm", false);
            if(configStructure==null) {
                noop = true;
                setSupportState(supportState);
                return;
            }
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = configStructure.getBooleanField("active");
            pvHighMajor = configStructure.getIntField("highMajor");
            pvHighMinor = configStructure.getIntField("highMinor");
            pvLowMinor = configStructure.getIntField("lowMinor");
            pvLowMajor = configStructure.getIntField("lowMajor");
            pvHystersis = configStructure.getIntField("hystersis");            
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(noop) {
                setSupportState(SupportState.ready);
                return;
            }
            lalm =pvValue.get();
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(noop) {
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            pvActive = null;
            pvHighMajor = null;
            pvHighMinor = null;
            pvLowMinor = null;
            pvLowMajor = null;
            pvHystersis = null;
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(noop) {
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            }
            boolean active = pvActive.get();
            if(!active) return;
            double  val = pvValue.get();
            double  hyst = pvHystersis.get();
            double hihi = pvHighMajor.get();
            double high = pvHighMinor.get();
            double low = pvLowMinor.get();
            double lolo = pvLowMajor.get();
            
            if ((val >= hihi || ((lalm==hihi) && (val >= hihi-hyst)))){
                String message = pvLink.getFullFieldName() + " high ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.major)) lalm = hihi; 
            } else if ((val <= lolo || ((lalm==lolo) && (val <= lolo+hyst)))){
                String message = pvLink.getFullFieldName() + " low ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.major)) lalm = lolo;
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            } else if ((val >= high || ((lalm==high) && (val >= high-hyst)))){
                String message = pvLink.getFullFieldName() + " high ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.minor)) lalm = high;
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            } else if ((val <= low || ((lalm==low) && (val <= low+hyst)))){
                String message = pvLink.getFullFieldName() + " low ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.minor)) lalm = low;
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            } else {
                lalm = val;
            }
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvInt) {
                super.message("setField: field type is not double", MessageType.error);
                return;
            }
            pvValue = (PVInt)pvField;
        }
    }
}
