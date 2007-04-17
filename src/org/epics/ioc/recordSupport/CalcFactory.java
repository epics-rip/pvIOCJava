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
     * @return The Support interface.
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
    
    static private class CalcImpl extends AbstractSupport implements SupportProcessRequester
    {
        private static String supportName = "calcRecord";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBField valueDBField = null;
        private CalcArgArraySupport calcArgArraySupport = null;
        private Support doubleAlarmSupport = null;
        private CalculatorSupport calculatorSupport = null;
        private Support linkArraySupport = null;
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult finalResult = RequestResult.success;
        private ProcessState processState = ProcessState.calcArgArraySupport;
        
        
        private CalcImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvStructure.getField();
            DBField[] dbFields = dbStructure.getFieldDBFields();
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
            if(index>=0) {
                DBLink doubleAlarmDBField = (DBLink)dbFields[index];
                doubleAlarmSupport = (LinkSupport)doubleAlarmDBField.getSupport();
            }
            index = structure.getFieldIndex("linkArray");
            if(index>=0) {
                DBNonScalarArray linkArrayDBField = (DBNonScalarArray)dbFields[index];
                linkArraySupport = linkArrayDBField.getSupport();
            }
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
            if(doubleAlarmSupport!=null) doubleAlarmSupport.stop();
            if(calcArgArraySupport!=null) calcArgArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            calcArgArraySupport.uninitialize();
            calculatorSupport.uninitialize();
            if(doubleAlarmSupport!=null) doubleAlarmSupport.uninitialize();
            if(calcArgArraySupport!=null) calcArgArraySupport.uninitialize();
            calcArgArraySupport = null;
            calculatorSupport = null;
            doubleAlarmSupport = null;
            linkArraySupport = null;
            valueDBField = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
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
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
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
                if(doubleAlarmSupport!=null) {
                    processState = ProcessState.doubleAlarmSupport;
                    doubleAlarmSupport.process(this);
                    return;
                }
            case doubleAlarmSupport:
                if(linkArraySupport!=null) {
                    processState = ProcessState.linkArraySupport;
                    linkArraySupport.process(this);
                    return;
                }
            case linkArraySupport:
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
        }
    }
}
