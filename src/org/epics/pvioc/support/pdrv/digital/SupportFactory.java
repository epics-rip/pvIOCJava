 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.digital;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.Support;

/**
 * Factory to create portDriver link support.
 * @author mrk
 *
 */
public class SupportFactory {
    /**
     * Create support for portDriver.
     * @param pvRecordStructure The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        PVAuxInfo pvAuxInfo = pvRecordStructure.getPVStructure().getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvRecordStructure.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvRecordStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        
        if(supportName.equals(pdrvUInt32DigitalInputSupportName))
            return new BaseUInt32DigitalInput(pvRecordStructure,pdrvUInt32DigitalInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInterruptSupportName))
            return new BaseUInt32DigitalInterrupt(pvRecordStructure,pdrvUInt32DigitalInterruptSupportName);
        if(supportName.equals(pdrvUInt32DigitalOutputSupportName))
            return new BaseUInt32DigitalOutput(pvRecordStructure,pdrvUInt32DigitalOutputSupportName);
        pvRecordStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String pdrvUInt32DigitalInputSupportName = "org.epics.pvioc.pdrvUInt32DigitalInputFactory";
    private static final String pdrvUInt32DigitalInterruptSupportName = "org.epics.pvioc.pdrvUInt32DigitalInterruptFactory";
    private static final String pdrvUInt32DigitalOutputSupportName = "org.epics.pvioc.pdrvUInt32DigitalOutputFactory";
}
