 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.digital;

import org.epics.ioc.support.Support;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;

/**
 * Factory to create portDriver link support.
 * @author mrk
 *
 */
public class SupportFactory {
    /**
     * Create support for portDriver.
     * @param pvStructure The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static Support create(PVStructure pvStructure) {
        PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvStructure.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        
        if(supportName.equals(pdrvUInt32DigitalInputSupportName))
            return new BaseUInt32DigitalInput(pvStructure,pdrvUInt32DigitalInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInterruptSupportName))
            return new BaseUInt32DigitalInterrupt(pvStructure,pdrvUInt32DigitalInterruptSupportName);
        if(supportName.equals(pdrvUInt32DigitalOutputSupportName))
            return new BaseUInt32DigitalOutput(pvStructure,pdrvUInt32DigitalOutputSupportName);
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String pdrvUInt32DigitalInputSupportName = "org.epics.ioc.pdrvUInt32DigitalInputFactory";
    private static final String pdrvUInt32DigitalInterruptSupportName = "org.epics.ioc.pdrvUInt32DigitalInterruptFactory";
    private static final String pdrvUInt32DigitalOutputSupportName = "org.epics.ioc.pdrvUInt32DigitalOutputFactory";
}
