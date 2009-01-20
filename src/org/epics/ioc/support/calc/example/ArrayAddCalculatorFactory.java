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

public class ArrayAddCalculatorFactory {
    public static Support create(PVStructure pvStructure) {
        return new ArrayAddCalculator(pvStructure);
    }

    private static String supportName = "arrayAddCalculator";

    private static class ArrayAddCalculator extends AbstractCalculatorSupport
    {
        private ArrayAddCalculator(PVStructure pvStructure) {
            super(supportName,pvStructure);
        }


        private ArgType[] argTypes = new ArgType[] {
            new ArgType("a",Type.scalar,ScalarType.pvDouble),
            new ArgType("b",Type.scalar,ScalarType.pvDouble)
        };
        private PVDoubleArray aPV = null;
        private DoubleArrayData aData = new DoubleArrayData();
        private double[] a;
        private int aLength;
        private PVDoubleArray bPV = null;
        private DoubleArrayData bData = new DoubleArrayData();
        private double[] b;
        private int bLength;

        private PVDoubleArray valuePV = null;
        private DoubleArrayData valueData = new DoubleArrayData();
        private double[] value;
        private int valueLength;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.scalarArray;}

        protected void setArgPVFields(PVField[] pvArgs) {
            aPV = (PVDoubleArray)pvArgs[0];
            bPV = (PVDoubleArray)pvArgs[1];
        };

        protected void setValuePVField(PVField pvValue) {
            valuePV = (PVDoubleArray)pvValue;
        };

        public void process(SupportProcessRequester supportProcessRequester) {
            valueLength = valuePV.getLength();
            valuePV.get(0,valueLength,valueData);
            value = valueData.data;
            aLength = aPV.getLength();
            aPV.get(0,aLength,aData);
            a = aData.data;
            bLength = bPV.getLength();
            bPV.get(0,bLength,bData);
            b = bData.data;
            compute();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        private void compute() {

           int len = aLength;
           if(len>bLength) len = bLength;
           if(valueLength!=len) {
               valueLength = len;
               valuePV.setLength(valueLength);
               valuePV.get(0,valueLength,valueData);
               value = valueData.data;
           }
           for(int i=0; i<valueLength; i++) {
               value[i] = Math.abs(a[i] + b[i]);
           }
  
        }
    }
}
