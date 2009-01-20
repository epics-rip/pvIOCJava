/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;
import org.epics.ioc.support.calc.*;


public class ArrayIncrementCalculatorFactory {
    public static Support create(PVStructure pvStructure) {
        return new ArrayIncrementCalculator(pvStructure);
    }

    private static String supportName = "arrayIncrementCalculator";

    private static class ArrayIncrementCalculator extends AbstractCalculatorSupport
    {
        private ArrayIncrementCalculator(PVStructure pvStructure) {
            super(supportName,pvStructure);
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
  
        }
    }
}
