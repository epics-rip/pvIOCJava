/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.calc.AbstractCalculatorSupport;
import org.epics.ioc.support.calc.ArgType;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.DoubleArrayData;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;


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
