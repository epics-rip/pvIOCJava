/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.calc.AbstractCalculatorSupport;
import org.epics.ioc.support.calc.ArgType;
import org.epics.ioc.util.RequestResult;

public class CounterCalculatorFactory {
    public static Support create(DBStructure dbStructure) {
        return new CounterCalculator(dbStructure);
    }

    private static String supportName = "counterCalculator";

    private static class CounterCalculator extends AbstractCalculatorSupport
    {
        private CounterCalculator(DBStructure dbStructure) {
            super(supportName,dbStructure);
        }


        private ArgType[] argTypes = new ArgType[] {
            new ArgType("min",Type.pvDouble,null),
            new ArgType("max",Type.pvDouble,null),
            new ArgType("inc",Type.pvDouble,null)
        };
        private PVDouble minPV = null;
        private double min;
        private PVDouble maxPV = null;
        private double max;
        private PVDouble incPV = null;
        private double inc;

        private DBField valueDB = null;
        private PVDouble valuePV = null;
        private double value;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.pvDouble;}

        protected void setArgPVFields(PVField[] pvArgs) {
            minPV = (PVDouble)pvArgs[0];
            maxPV = (PVDouble)pvArgs[1];
            incPV = (PVDouble)pvArgs[2];
        };

        protected void setValueDBField(DBField dbValue) {
            this.valueDB = dbValue;
            valuePV = (PVDouble)dbValue.getPVField();
        };

        public void process(SupportProcessRequester supportProcessRequester) {
            value = valuePV.get();
            min = minPV.get();
            max = maxPV.get();
            inc = incPV.get();
            compute();
            valuePV.put(value);
            valueDB.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }

        private void compute() {

            value += inc;
            if(inc>0) {
                if(value>max) value = min;
                if(value<min) value = min;
            } else {
                if(value>min) value = max;
                if(value<max) value = max;
            }
  
        }
    }
}
