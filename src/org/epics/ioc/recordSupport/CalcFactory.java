/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.recordSupport;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.support.*;
import org.epics.ioc.util.*;

/**
 * Record that implements a calculation.
 * @author mrk
 *
 */
public class CalcFactory {
    /**
     * Create support for a CalcRecord.
     * @param dbStructure
     * @return
     */
    public static Support create(DBStructure dbStructure) {
        return new CalcImpl(dbStructure);
    }
    
    private enum ProcessState {
        calcArgArraySupport,
        calculatorSupport,
        doubleAlarmSupport,
        linkArraySupport
    }
    
    static private class CalcImpl extends AbstractSupport implements SupportProcessRequestor
    {
        private static String supportName = "calcRecord";
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private DBField valueDBField = null;
        private CalcArgArraySupport calcArgArraySupport = null;
        private LinkSupport doubleAlarmSupport = null;
        private CalculatorSupport calculatorSupport = null;
        private Support linkArraySupport = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private RequestResult finalResult = RequestResult.success;
        private ProcessState processState = ProcessState.calcArgArraySupport;
        
        
        private CalcImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getDBRecord();
            pvRecord = dbRecord.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvRecord.getField();
            DBField[] dbFields = dbRecord.getDBStructure().getFieldDBFields();
            int index;
            if(valueDBField==null) {
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("record does not have value field", MessageType.error);
                    return;
                }
                valueDBField = dbFields[index];
            }
            index = structure.getFieldIndex("calcArgArray");
            if(index<0) {
                super.message("record does not have field calcArgArray", MessageType.error);
                return;
            }
            DBNonScalarArray calcArgArrayDBField = (DBNonScalarArray)dbFields[index];
            calcArgArraySupport = (CalcArgArraySupport)calcArgArrayDBField.getSupport();
            if(calcArgArraySupport==null) {
                super.message("calcArgArraySupport does not have support", MessageType.error);
                return;
            }
            index = structure.getFieldIndex("calculator");
            if(index<0) {
                super.message("record does not have field calculator", MessageType.error);
                return;
            }
            DBLink calculatorDBField = (DBLink)dbFields[index];
            calculatorSupport = (CalculatorSupport)calculatorDBField.getSupport();
            if(calculatorSupport==null) {
                super.message("field calculator does not have support", MessageType.error);
                return;
            }
            index = structure.getFieldIndex("doubleAlarm");
            if(index<0) {
                super.message("record does not have field doubleAlarm", MessageType.error);
                return;
            }
            DBLink doubleAlarmDBField = (DBLink)dbFields[index];
            doubleAlarmSupport = (LinkSupport)doubleAlarmDBField.getSupport();
            index = structure.getFieldIndex("linkArray");
            if(index<0) {
                super.message("record does not have linkArray field", MessageType.error);
                return;
            }
            DBNonScalarArray linkArrayDBField = (DBNonScalarArray)dbFields[index];
            linkArraySupport = linkArrayDBField.getSupport();
            calcArgArraySupport.initialize();
            supportState = calcArgArraySupport.getSupportState();
            if(supportState!=SupportState.readyForStart) return;
            calculatorSupport.setField(valueDBField);
            calculatorSupport.setCalcArgArraySupport(calcArgArraySupport);
            calculatorSupport.initialize();
            supportState = calculatorSupport.getSupportState();
            if(supportState!=SupportState.readyForStart) {
                if(calcArgArraySupport!=null) calcArgArraySupport.uninitialize();
                return;
            }
            if(doubleAlarmSupport!=null) {
                doubleAlarmSupport.setField(valueDBField);
                doubleAlarmSupport.initialize();
                supportState = doubleAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    linkArraySupport.uninitialize();
                    calculatorSupport.uninitialize();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.setField(valueDBField);
                linkArraySupport.initialize();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    linkArraySupport.uninitialize();
                    calculatorSupport.uninitialize();
                    if(doubleAlarmSupport!=null) doubleAlarmSupport.uninitialize();
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
            SupportState supportState = SupportState.ready;
            calcArgArraySupport.start();
            supportState = calcArgArraySupport.getSupportState();
            if(supportState!=SupportState.ready) return;
            calculatorSupport.start();
            supportState = calculatorSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                calcArgArraySupport.stop();
                return;
            }
            doubleAlarmSupport.start();
            supportState = doubleAlarmSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                calcArgArraySupport.stop();
                calculatorSupport.stop();
                return;
            }
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    calcArgArraySupport.stop();
                    calculatorSupport.stop();
                    doubleAlarmSupport.stop();
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            calcArgArraySupport.stop();
            calculatorSupport.stop();
            doubleAlarmSupport.stop();
            if(calcArgArraySupport!=null) calcArgArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            calcArgArraySupport.uninitialize();
            calculatorSupport.uninitialize();
            doubleAlarmSupport.uninitialize();
            if(calcArgArraySupport!=null) calcArgArraySupport.uninitialize();
            calcArgArraySupport = null;
            calculatorSupport = null;
            doubleAlarmSupport = null;
            linkArraySupport = null;
            valueDBField = null;
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
            this.supportProcessRequestor = supportProcessRequestor;
            processState = ProcessState.calcArgArraySupport;
            finalResult = RequestResult.success;
            calcArgArraySupport.process(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        @Override
        public void setField(DBField dbField) {
            valueDBField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult.compareTo(finalResult)>0) {
                finalResult = requestResult;
            }
            switch(processState) {
            case calcArgArraySupport:
                processState = ProcessState.calculatorSupport;
                calculatorSupport.process(this);
                return;
            case calculatorSupport:
                processState = ProcessState.doubleAlarmSupport;
                doubleAlarmSupport.process(this);
                return;
            case doubleAlarmSupport:
                processState = ProcessState.linkArraySupport;
                linkArraySupport.process(this);
                return;
            case linkArraySupport:
                supportProcessRequestor.supportProcessDone(finalResult);
                return;
            }
        }
    }
}
