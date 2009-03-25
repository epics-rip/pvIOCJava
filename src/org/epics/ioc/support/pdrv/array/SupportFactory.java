 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.array;

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
        
        if(supportName.equals(pdrvInt32ArrayInputSupportName))
            return new BaseInt32ArrayInput(pvStructure,pdrvInt32ArrayInputSupportName);
        if(supportName.equals(pdrvInt32ArrayInterruptSupportName))
            return new BaseInt32ArrayInterrupt(pvStructure,pdrvInt32ArrayInterruptSupportName);
        if(supportName.equals(pdrvInt32ArrayOutputSupportName))
            return new BaseInt32ArrayOutput(pvStructure,pdrvInt32ArrayOutputSupportName);
        
        if(supportName.equals(pdrvFloat64ArrayInputSupportName))
            return new BaseFloat64ArrayInput(pvStructure,pdrvFloat64ArrayInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInterruptSupportName))
            return new BaseFloat64ArrayInterrupt(pvStructure,pdrvFloat64ArrayInterruptSupportName);
        if(supportName.equals(pdrvFloat64ArrayOutputSupportName))
            return new BaseFloat64ArrayOutput(pvStructure,pdrvFloat64ArrayOutputSupportName);
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String pdrvInt32ArrayInputSupportName = "pdrvInt32ArrayInputFactory";
    private static final String pdrvInt32ArrayInterruptSupportName = "pdrvInt32ArrayInterruptFactory";
    private static final String pdrvInt32ArrayOutputSupportName = "pdrvInt32ArrayOutputFactory";
   
    private static final String pdrvFloat64ArrayInputSupportName = "pdrvFloat64ArrayInputFactory";
    private static final String pdrvFloat64ArrayInterruptSupportName = "pdrvFloat64ArrayInterruptFactory";
    private static final String pdrvFloat64ArrayOutputSupportName = "pdrvFloat64ArrayOutputFactory";
}
