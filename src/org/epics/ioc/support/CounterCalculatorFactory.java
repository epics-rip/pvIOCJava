/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class CounterCalculatorFactory {
    
    public static LinkSupport create(DBLink dbLink) {
        return new CounterCalculatorImpl(dbLink);
    }
    
    private static String supportName = "counterCalculator";
    
    private static class CounterCalculatorImpl extends AbstractLinkSupport implements CalculatorSupport
    {
        private PVLink pvLink;
        private CalcArgArraySupport calcArgArraySupport = null;
        private DBField valueDBField;
        private PVDouble valuePVField= null;;
        private PVDouble minPVField = null;
        private PVDouble maxPVField = null;
        private PVDouble incPVField = null;

        private CounterCalculatorImpl(DBLink dbLink) {
            super(supportName,dbLink);
            pvLink = dbLink.getPVLink();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            minPVField = calcArgArraySupport.getPVDouble("min");
            if(minPVField==null) {
                pvLink.message("min not found", MessageType.error);
                return;
            }
            maxPVField = calcArgArraySupport.getPVDouble("max");
            if(maxPVField==null) {
                pvLink.message("max not found", MessageType.error);
                return;
            }
            incPVField = calcArgArraySupport.getPVDouble("inc");
            if(incPVField==null) {
                pvLink.message("inc not found", MessageType.error);
                return;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            double value = valuePVField.get();
            double min = minPVField.get();
            double max = maxPVField.get();
            double inc = incPVField.get();
            value += inc;
            if(inc>0) {
                if(value>max) value = min;
                if(value<min) value = min;
            } else {
                if(value<min) value = max;
                if(value>max) value = max;
            }
            valuePVField.put(value);
            valueDBField.postPut();
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
            valuePVField = (PVDouble)dbField.getPVField();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.recordSupport.CalculatorSupport#setCalcArgSupport(org.epics.ioc.recordSupport.CalcArgArraySupport)
         */
        public void setCalcArgArraySupport(CalcArgArraySupport calcArgArraySupport) {
            this.calcArgArraySupport = calcArgArraySupport;
        }
    }
}
