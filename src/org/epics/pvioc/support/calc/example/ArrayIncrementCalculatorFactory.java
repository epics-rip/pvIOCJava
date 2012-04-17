/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.calc.example;

import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.calc.AbstractCalculatorSupport;
import org.epics.pvioc.support.calc.ArgType;
import org.epics.pvioc.util.RequestResult;


/**
 * This example expects no arguments and a value field that is a double array.
 * Each time the record io processed 1 is added to each element.
 * @author mrk
 *
 */
public class ArrayIncrementCalculatorFactory {
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new ArrayIncrementCalculator(pvRecordStructure);
    }

    private static String supportName = "arrayIncrementCalculator";

    private static class ArrayIncrementCalculator extends AbstractCalculatorSupport
    {
        private ArrayIncrementCalculator(PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
        }


        private ArgType[] argTypes = new ArgType[0];
        private PVDoubleArray valuePV = null;
        private DoubleArrayData valueData = new DoubleArrayData();
        private double[] value;
        private int valueLength;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.scalarArray;}

        protected void setArgPVFields(PVField[] pvArgs) {
        };

        protected void setValuePVField(PVField pvValue) {
            valuePV = (PVDoubleArray)pvValue;
            valueLength = valuePV.getLength();
            if(valueLength>0) return;
            valueLength = valuePV.getCapacity();
            if(valueLength==0) return;
            valuePV.get(0,valueLength,valueData);
            value = valueData.data;
            for(int i=0; i<valueLength; i++) {
                value[i] = i;
            }
            valuePV.setLength(valueLength);
        };

        public void process(SupportProcessRequester supportProcessRequester) {
            valueLength = valuePV.getLength();
            valuePV.get(0,valueLength,valueData);
            value = valueData.data;
            compute();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        private void compute() {

           for(int i=0; i<valueLength; i++) {
               value[i] = value[i] + 1.0;
           }
           valuePV.postPut();
        }
    }
}
