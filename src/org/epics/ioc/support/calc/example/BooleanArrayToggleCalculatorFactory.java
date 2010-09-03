/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc.example;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.calc.AbstractCalculatorSupport;
import org.epics.ioc.support.calc.ArgType;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.BooleanArrayData;
import org.epics.pvData.pv.PVBooleanArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.Type;

/**
 * This example expects no arguments and that the value field is a boolean array.
 * Each time the record is processed each element is toggled between true and false.
 * @author mrk
 *
 */
public class BooleanArrayToggleCalculatorFactory {
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new BooleanArrayToggleCalculator(pvRecordStructure);
    }

    private static String supportName = "booleanArrayToggleCalculator";

    private static class BooleanArrayToggleCalculator extends AbstractCalculatorSupport
    {
        private BooleanArrayToggleCalculator(PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
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
             valuePV.postPut();
        }
    }
}
