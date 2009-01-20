/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.device;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;

import org.epics.ioc.support.*;
import org.epics.ioc.util.*;


/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class PowerSupplyFactory {
    /**
     * Create the support for the record or structure.
     * @param pvStructure The structure or record for which to create support.
     * @return The support instance.
     */
    public static Support create(PVStructure pvStructure) {
        PVRecord pvRecord = pvStructure.getPVRecord();
        RecordSupport recordSupport = masterSupportDatabase.getRecordSupport(pvRecord);
        String supportName = recordSupport.getSupport(pvStructure).getSupportName();
        if(!supportName.equals(powerSupplyCurrentName)) {
            pvStructure.message("no support for " + supportName, MessageType.fatalError);
            return null;
        }
        // we want the parent of the parent
        PVStructure pvParent = pvStructure.getParent();
        if(pvParent==null) {
            pvStructure.message("no parent", MessageType.fatalError);
            return null;
        }
        pvParent = pvParent.getParent();
        if(pvParent==null) {
            pvStructure.message("no parent of the parent", MessageType.fatalError);
            return null;
        }
        PVDouble pvCurrent = getPVDouble(pvParent,"current.value");
        if(pvCurrent==null) return null;
        PVDouble pvVoltage = getPVDouble(pvParent,"voltage.value");
        if(pvVoltage==null) return null;
        PVDouble pvPower = getPVDouble(pvParent,"power.value");
        if(pvPower==null) return null;
       
        return new PowerSupplyCurrentImpl(pvStructure,pvCurrent,pvVoltage,pvPower);
    }
    
    private static PVDouble getPVDouble(PVStructure pvParent,String fieldName) {
        PVField pvField = pvProperty.findProperty(pvParent, fieldName);
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
    
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final SupportDatabase masterSupportDatabase = SupportDatabaseFactory.get(masterPVDatabase);
    private static final String powerSupplyCurrentName = "powerSupplyCurrent";
    private static final PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    
    
    static private class PowerSupplyCurrentImpl extends AbstractSupport
    {
        private PVDouble powerPVField = null;
        private PVDouble currentPVField = null;
        private PVDouble voltagePVField = null;
        
        private double power;
        private double voltage;
        private double current;
        
        private PowerSupplyCurrentImpl(PVStructure pvStructure,PVDouble currentPVField, PVDouble voltagePVField, PVDouble powerPVField) {
            super(powerSupplyCurrentName,pvStructure);
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
