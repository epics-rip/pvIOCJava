 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;

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
            pvStructure.message("no pvAuxInfo with name supportFactory. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        
        if(supportName.equals(pdrvInt32InputSupportName))
            return new BaseInt32Input(pvStructure,pdrvInt32InputSupportName);
        if(supportName.equals(pdrvInt32InterruptSupportName))
            return new BaseInt32Interrupt(pvStructure,pdrvInt32InterruptSupportName);
        if(supportName.equals(pdrvInt32AverageSupportName))
            return new BaseInt32Average(pvStructure,pdrvInt32AverageSupportName);
        if(supportName.equals(pdrvInt32OutputSupportName))
            return new BaseInt32Output(pvStructure,pdrvInt32OutputSupportName);
        
        if(supportName.equals(pdrvFloat64InputSupportName))
            return new BaseFloat64Input(pvStructure,pdrvFloat64InputSupportName);
        if(supportName.equals(pdrvFloat64InterruptSupportName))
            return new BaseFloat64Interrupt(pvStructure,pdrvFloat64InterruptSupportName);
        if(supportName.equals(pdrvFloat64AverageSupportName))
            return new BaseFloat64Average(pvStructure,pdrvFloat64AverageSupportName);
        if(supportName.equals(pdrvFloat64OutputSupportName))
            return new BaseFloat64Output(pvStructure,pdrvFloat64OutputSupportName);
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
   
    private static final String pdrvInt32InputSupportName = "pdrvInt32InputFactory";
    private static final String pdrvInt32InterruptSupportName = "pdrvInt32InterruptFactory";
    private static final String pdrvInt32AverageSupportName = "pdrvInt32AverageFactory";
    private static final String pdrvInt32OutputSupportName = "pdrvInt32OutputFactory";
    
    private static final String pdrvFloat64InputSupportName = "pdrvFloat64InputFactory";
    private static final String pdrvFloat64InterruptSupportName = "pdrvFloat64InterruptFactory";
    private static final String pdrvFloat64AverageSupportName = "pdrvFloat64AverageFactory";
    private static final String pdrvFloat64OutputSupportName = "pdrvFloat64OutputFactory";
}
