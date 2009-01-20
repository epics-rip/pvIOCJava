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
