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
public class ArrayIncrementCalculatorFactory {
    
    public static LinkSupport create(DBLink dbLink) {
        return new ArrayIncrementCalculatorImpl(dbLink);
    }
    
    private static String supportName = "arrayIncrementCalculator";
    
    private static class ArrayIncrementCalculatorImpl extends AbstractLinkSupport implements CalculatorSupport
    {
        private static Convert convert = ConvertFactory.getConvert();
        private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private PVLink pvLink;
        private CalcArgArraySupport calcArgArraySupport = null;
        private DBField valueDBField;
        private PVArray valuePVField= null;
        private PVDoubleArray pvDoubleArray = null;
        private DoubleArrayData doubleArrayData = new DoubleArrayData();

        private ArrayIncrementCalculatorImpl(DBLink dbLink) {
            super(supportName,dbLink);
            pvLink = dbLink.getPVLink();
            Array array = fieldCreate.createArray("private", Type.pvDouble);
            pvDoubleArray = (PVDoubleArray)pvDataCreate.createPVArray(pvLink, array, 0, true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            int length = valuePVField.getLength();
            convert.copyArray(valuePVField, 0, pvDoubleArray, 0, length);
            pvDoubleArray.get(0, length, doubleArrayData);
            double[] data = doubleArrayData.data;
            for(int i=0; i<length; i++) {
                data[i] = data[i] + 1.0;
            }
            convert.copyArray(pvDoubleArray, 0, valuePVField, 0, length);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
            valuePVField = (PVArray)dbField.getPVField();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.recordSupport.CalculatorSupport#setCalcArgSupport(org.epics.ioc.recordSupport.CalcArgArraySupport)
         */
        public void setCalcArgArraySupport(CalcArgArraySupport calcArgArraySupport) {
            this.calcArgArraySupport = calcArgArraySupport;
        }
    }
}
