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

public class ArrayAddCalculatorFactory {
    public static Support create(DBStructure dbStructure) {
        return new ArrayAddCalculator(dbStructure);
    }

    private static String supportName = "arrayAddCalculator";

    private static class ArrayAddCalculator extends AbstractCalculatorSupport
    {
        private ArrayAddCalculator(DBStructure dbStructure) {
            super(supportName,dbStructure);
        }


        private ArgType[] argTypes = new ArgType[] {
            new ArgType("a",Type.pvArray,Type.pvDouble),
            new ArgType("b",Type.pvArray,Type.pvDouble)
        };
        private PVDoubleArray aPV = null;
        private DoubleArrayData aData = new DoubleArrayData();
        private double[] a;
        private int aLength;
        private PVDoubleArray bPV = null;
        private DoubleArrayData bData = new DoubleArrayData();
        private double[] b;
        private int bLength;

        private DBField valueDB = null;
        private PVDoubleArray valuePV = null;
        private DoubleArrayData valueData = new DoubleArrayData();
        private double[] value;
        private int valueLength;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.pvArray;}

        protected void setArgPVFields(PVField[] pvArgs) {
            aPV = (PVDoubleArray)pvArgs[0];
            bPV = (PVDoubleArray)pvArgs[1];
        };

        protected void setValueDBField(DBField dbValue) {
            this.valueDB = dbValue;
            valuePV = (PVDoubleArray)dbValue.getPVField();
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
            valueDB.postPut();
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
