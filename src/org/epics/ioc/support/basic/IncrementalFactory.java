/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Record that implements incremental outputs.
 * It requires the following:
 * value double
 *     desired
 *         value double
 *         input
 *         incremental boolean supportName = incremental
 *         rateOfChange
 * It requires field rateOfChange which must be numeric.
 * It optionally supports the following field incremental
 * If present it must be a boolean.
 * If the support exists it calls the support for input.
 * @author mrk
 *
 */
public class IncrementalFactory {
    /**
     * Create the support for the record or structure.
     * @param pvField The field for which to create support. It must have type boolean.
     * @return The support instance.
     */
    public static Support create(PVField pvField) {
        if(pvField.getField().getType()!=Type.scalar) {
            pvField.message("type is not boolean", MessageType.error);
        }
        PVScalar pvScalar = (PVScalar)pvField;
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvBoolean) {
            pvField.message("type is not boolean", MessageType.error);
        }
        return new IncrementalImpl((PVBoolean)pvField);
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    
    static private class IncrementalImpl extends AbstractSupport
    {
        private static String supportName = "incremental";
        private PVBoolean pvIncremental = null;
        private PVScalar pvValue = null;
        private PVScalar pvDesiredValue = null;
        private PVScalar pvRateOfChange = null;
        
        private double desiredValue = 0.0;
        private double value = 0.0;
        private boolean incremental = true;
        private double rateOfChange = 0.0;
        
        private IncrementalImpl(PVBoolean pvBoolean) {
            super(supportName,pvBoolean);
            pvIncremental = pvBoolean;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVStructure parent = pvIncremental.getParent();
            PVField pvField = parent.getParent().getSubField("value");
            if(pvField==null || pvField.getField().getType()!=Type.scalar) {
                parent.message("does not have a scalar field named value", MessageType.error);
                return;
            }
            pvDesiredValue = (PVScalar)pvField;
            pvField = parent.getSubField("rateOfChange");
            if(pvField==null || pvField.getField().getType()!=Type.scalar) {
                parent.message("does not have a scalar field named value", MessageType.error);
                return;
            }
            pvRateOfChange = (PVScalar)pvField;
            parent = parent.getParent();
            pvField = parent.getSubField("value");
            if(pvField==null || pvField.getField().getType()!=Type.scalar) {
                parent.message("does not have a scalar field named value", MessageType.error);
                return;
            }
            pvValue = (PVScalar)pvField;
            super.setSupportState(SupportState.readyForStart); 
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            pvValue = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toDouble(pvValue);
            desiredValue = convert.toDouble(pvDesiredValue);
            if(pvIncremental!=null) incremental = pvIncremental.get();
            if(pvRateOfChange!=null) rateOfChange = convert.toDouble(pvRateOfChange);
            if(desiredValue==value) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            double newValue = desiredValue;
            if(incremental) {
                double diff = desiredValue - value;
                if(diff<0.0) {
                    newValue = value - rateOfChange;
                    if(newValue<desiredValue) newValue = desiredValue;
                } else {
                    newValue = value + rateOfChange;
                    if(newValue>desiredValue) newValue = desiredValue;
                }
            }
            convert.fromDouble(pvValue, newValue);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
