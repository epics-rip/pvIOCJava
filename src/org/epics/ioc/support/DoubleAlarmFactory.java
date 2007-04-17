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
 * Support for a doubleAlarm link.
 * @author mrk
 *
 */
public class DoubleAlarmFactory {
    /**
     * Create support for a doubleAlarm link.
     * @param dbLink The link.
     * @return An interface to the support or null if the supportName was not "doubleAlarm".
     */
    public static Support create(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        String supportName = pvLink.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvLink.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new DoubleAlarmImpl(dbLink);
    }
    
    private static String supportName = "doubleAlarm";
    
    private static class DoubleAlarmImpl extends AbstractLinkSupport
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVDouble pvHighMajor;
        private PVDouble pvHighMinor;
        private PVDouble pvLowMinor;
        private PVDouble pvLowMajor;
        private PVDouble pvHystersis;
        
        private PVDouble pvValue;
        private double lalm;
       
        private DoubleAlarmImpl(DBLink dbLink) {
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
                super.message("setField was not called with a double field", MessageType.error);
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
            pvHighMajor = configStructure.getDoubleField("highMajor");
            pvHighMinor = configStructure.getDoubleField("highMinor");
            pvLowMinor = configStructure.getDoubleField("lowMinor");
            pvLowMajor = configStructure.getDoubleField("lowMajor");
            pvHystersis = configStructure.getDoubleField("hystersis");
            lalm =pvValue.get();
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
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
            pvActive = null;
            pvHighMajor = null;
            pvHighMinor = null;
            pvLowMinor = null;
            pvLowMajor = null;
            pvHystersis = null;
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(noop) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
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
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            } else if ((val >= high || ((lalm==high) && (val >= high-hyst)))){
                String message = pvLink.getFullFieldName() + " high ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.minor)) lalm = high;
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            } else if ((val <= low || ((lalm==low) && (val <= low+hyst)))){
                String message = pvLink.getFullFieldName() + " low ";
                if (alarmSupport.setAlarm(message, AlarmSeverity.minor)) lalm = low;
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            } else {
                lalm = val;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvDouble) {
                super.message("setField: field type is not double", MessageType.error);
                return;
            }
            pvValue = (PVDouble)pvField;
        }
    }
}
