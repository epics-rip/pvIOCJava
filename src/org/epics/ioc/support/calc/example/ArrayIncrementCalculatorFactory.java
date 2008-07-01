/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.DoubleArrayData;
import org.epics.ioc.pv.PVDoubleArray;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.calc.AbstractCalculatorSupport;
import org.epics.ioc.support.calc.ArgType;
import org.epics.ioc.util.RequestResult;

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

        public ArgType[] getArgTypes() { return argTypes;}

        public Type getValueType() { return Type.pvArray;}

        public void setArgPVFields(PVField[] pvArgs) {
        };

        public void setValueDBField(DBField dbValue) {
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

        public void compute() {

           for(int i=0; i<valueLength; i++) {
               value[i] = value[i] + 1.0;
           }
  
        }
    }
}
