/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.support.calc.*;
import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.support.*;

public class ArrayIncrementCalculatorFactory {
    public static Support create(DBStructure dbStructure) {
        return new ArrayIncrementCalculator(dbStructure);
    }

    private static String supportName = "arrayIncrementCalculator";

    private static class ArrayIncrementCalculator extends AbstractCalculatorSupport
    {
        private ArrayIncrementCalculator(DBStructure dbStructure) {
            super(supportName,dbStructure);
        }


        private ArgType[] argTypes = new ArgType[0];

        private DBField valueDB = null;
        private PVDoubleArray valuePV = null;
        private DoubleArrayData valueData = new DoubleArrayData();
        private double[] value;
        private int valueLength;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.pvArray;}

        protected void setArgPVFields(PVField[] pvArgs) {
        };

        protected void setValueDBField(DBField dbValue) {
            this.valueDB = dbValue;
            valuePV = (PVDoubleArray)dbValue.getPVField();
        };

        public void process(SupportProcessRequester supportProcessRequester) {
            valueLength = valuePV.getLength();
            valuePV.get(0,valueLength,valueData);
            value = valueData.data;
            compute();
            valueDB.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        private void compute() {

           for(int i=0; i<valueLength; i++) {
               value[i] = value[i] + 1.0;
           }
  
        }
    }
}
