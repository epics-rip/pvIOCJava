/* generated code */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.calc.AbstractCalculatorSupport;
import org.epics.ioc.support.calc.ArgType;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;



public class CounterCalculatorFactory {
    public static Support create(PVStructure pvStructure) {
        return new CounterCalculator(pvStructure);
    }

    private static String supportName = "counterCalculator";

    private static class CounterCalculator extends AbstractCalculatorSupport
    {
        private CounterCalculator(PVStructure pvStructure) {
            super(supportName,pvStructure);
        }


        private ArgType[] argTypes = new ArgType[] {
            new ArgType("min",Type.scalar,null),
            new ArgType("max",Type.scalar,null),
            new ArgType("inc",Type.scalar,null)
        };
        private PVDouble minPV = null;
        private double min;
        private PVDouble maxPV = null;
        private double max;
        private PVDouble incPV = null;
        private double inc;

        private PVDouble valuePV = null;
        private double value;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.scalar;}

        protected void setArgPVFields(PVField[] pvArgs) {
            minPV = (PVDouble)pvArgs[0];
            maxPV = (PVDouble)pvArgs[1];
            incPV = (PVDouble)pvArgs[2];
        };

        protected void setValuePVField(PVField pvValue) {
            valuePV = (PVDouble)pvValue;
        };

        public void process(SupportProcessRequester supportProcessRequester) {
            value = valuePV.get();
            min = minPV.get();
            max = maxPV.get();
            inc = incPV.get();
            compute();
            valuePV.put(value);
            valuePV.postPut();
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
