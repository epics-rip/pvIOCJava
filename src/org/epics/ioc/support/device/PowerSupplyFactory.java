/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.device;

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;


/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class PowerSupplyFactory {
    /**
     * Create the support for the record or structure.
     * @param pvField The structure or record for which to create support.
     * @return The support instance.
     */
    public static Support create(PVField pvField) {
        PVAuxInfo pvAuxInfo = pvField.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvField.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvField.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
       
        if(!supportName.equals(powerSupplyFactory)) {
            pvField.message("no support for " + supportName, MessageType.fatalError);
            return null;
        }
        // we want the parent of the parent
        PVStructure pvParent = pvField.getParent();
        if(pvParent==null) {
            pvField.message("no parent", MessageType.fatalError);
            return null;
        }
        pvParent = pvParent.getParent();
        if(pvParent==null) {
            pvField.message("no parent of the parent", MessageType.fatalError);
            return null;
        }
        PVDouble pvCurrent = getPVDouble(pvParent,"current.value");
        if(pvCurrent==null) return null;
        PVDouble pvVoltage = getPVDouble(pvParent,"voltage.value");
        if(pvVoltage==null) return null;
        PVDouble pvPower = getPVDouble(pvParent,"power.value");
        if(pvPower==null) return null;
       
        return new PowerSupplyCurrentImpl(pvField,pvCurrent,pvVoltage,pvPower);
    }
    
    private static PVDouble getPVDouble(PVStructure pvParent,String fieldName) {
        PVField pvField = pvParent.getSubField(fieldName);
        if(pvField==null) {
            pvParent.message(fieldName + " does not exist", MessageType.fatalError);
            return null;
        }
        if(pvField.getField().getType()!=Type.scalar) {
            pvParent.message(fieldName + " is not a double", MessageType.fatalError);
            return null;
        }
        PVScalar pvScalar = (PVScalar)pvField;
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvDouble) {
            pvParent.message(fieldName + " is not a double", MessageType.fatalError);
            return null;
        }
        return (PVDouble)pvField;
    }
    
    private static final String powerSupplyFactory = "org.epics.ioc.powerSupplyFactory";
    
    
    static private class PowerSupplyCurrentImpl extends AbstractSupport
    {
        private PVDouble powerPVField = null;
        private PVDouble currentPVField = null;
        private PVDouble voltagePVField = null;
        
        private double power;
        private double voltage;
        private double current;
        
        private PowerSupplyCurrentImpl(PVField pvStructure,PVDouble currentPVField, PVDouble voltagePVField, PVDouble powerPVField) {
            super(powerSupplyFactory,pvStructure);
            this.powerPVField = powerPVField;
            this.currentPVField = currentPVField;
            this.voltagePVField = voltagePVField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            power = powerPVField.get();
            voltage = voltagePVField.get();
            if(voltage==0.0) {
                current = 0.0;
            } else {
                current = power/voltage;
            }
            currentPVField.put(current);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
