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
 * @author mrk
 *
 */
public class BooleanArrayToggleCalculatorFactory {
    
    public static Support create(DBStructure dbStructure) {
        return new ArrayIncrementCalculatorImpl(dbStructure);
    }
    
    private static String supportName = "booleanArrayToggleCalculator";
    
    private static class ArrayIncrementCalculatorImpl extends AbstractSupport implements CalculatorSupport
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        
        private DBField valueDBField = null;
        private PVBooleanArray valuePVField= null;
        private BooleanArrayData booleanArrayData = new BooleanArrayData();

        private ArrayIncrementCalculatorImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBField dbParent = dbStructure.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvField = pvParent.findProperty("value");
            if(pvField==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.message(" field must be boolean array", MessageType.error);
                return;
            }
            Array array = (Array)field;
            if(array.getElementType()!=Type.pvBoolean) {
                super.message(" field must be boolean array", MessageType.error);
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            valuePVField = (PVBooleanArray)pvField;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            int length = valuePVField.getLength();
            valuePVField.get(0, length, booleanArrayData);
            boolean[] data = booleanArrayData.data;
            for(int i=0; i<length; i++) {
                data[i] = (data[i] ? false : true);
            }
            valuePVField.put(0, length, data, 0);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.recordSupport.CalculatorSupport#setCalcArgSupport(org.epics.ioc.recordSupport.CalcArgArraySupport)
         */
        public void setCalcArgArraySupport(CalcArgArraySupport calcArgArraySupport) {
            // nothing to do
        }
    }
}
