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

public class BooleanArrayToggleCalculatorFactory {
    public static Support create(PVStructure pvStructure) {
        return new BooleanArrayToggleCalculator(pvStructure);
    }

    private static String supportName = "booleanArrayToggleCalculator";

    private static class BooleanArrayToggleCalculator extends AbstractCalculatorSupport
    {
        private BooleanArrayToggleCalculator(PVStructure pvStructure) {
            super(supportName,pvStructure);
        }


        private ArgType[] argTypes = new ArgType[0];

        private PVBooleanArray valuePV = null;
        private BooleanArrayData valueData = new BooleanArrayData();
        private boolean[] value;
        private int valueLength;

        protected ArgType[] getArgTypes() { return argTypes;}

        protected Type getValueType() { return Type.scalarArray;}

        protected void setArgPVFields(PVField[] pvArgs) {
        };

        protected void setValuePVField(PVField pvValue) {
            valuePV = (PVBooleanArray)pvValue;
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
                 value[i] = (value[i] ? false : true);
             }
  
        }
    }
}
