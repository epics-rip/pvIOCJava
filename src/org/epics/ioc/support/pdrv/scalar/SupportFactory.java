 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.Support;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.ScalarType;

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
            pvRecordStructure.message("no pvAuxInfo with name supportFactory. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvRecordStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        
        if(supportName.equals(pdrvInt32InputSupportName))
            return new BaseInt32Input(pvRecordStructure,pdrvInt32InputSupportName);
        if(supportName.equals(pdrvInt32InterruptSupportName))
            return new BaseInt32Interrupt(pvRecordStructure,pdrvInt32InterruptSupportName);
        if(supportName.equals(pdrvInt32AverageSupportName))
            return new BaseInt32Average(pvRecordStructure,pdrvInt32AverageSupportName);
        if(supportName.equals(pdrvInt32OutputSupportName))
            return new BaseInt32Output(pvRecordStructure,pdrvInt32OutputSupportName);
        
        if(supportName.equals(pdrvFloat64InputSupportName))
            return new BaseFloat64Input(pvRecordStructure,pdrvFloat64InputSupportName);
        if(supportName.equals(pdrvFloat64InterruptSupportName))
            return new BaseFloat64Interrupt(pvRecordStructure,pdrvFloat64InterruptSupportName);
        if(supportName.equals(pdrvFloat64AverageSupportName))
            return new BaseFloat64Average(pvRecordStructure,pdrvFloat64AverageSupportName);
        if(supportName.equals(pdrvFloat64OutputSupportName))
            return new BaseFloat64Output(pvRecordStructure,pdrvFloat64OutputSupportName);
        pvRecordStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
   
    private static final String pdrvInt32InputSupportName = "org.epics.ioc.pdrvInt32InputFactory";
    private static final String pdrvInt32InterruptSupportName = "org.epics.ioc.pdrvInt32InterruptFactory";
    private static final String pdrvInt32AverageSupportName = "org.epics.ioc.pdrvInt32AverageFactory";
    private static final String pdrvInt32OutputSupportName = "org.epics.ioc.pdrvInt32OutputFactory";
    
    private static final String pdrvFloat64InputSupportName = "org.epics.ioc.pdrvFloat64InputFactory";
    private static final String pdrvFloat64InterruptSupportName = "org.epics.ioc.pdrvFloat64InterruptFactory";
    private static final String pdrvFloat64AverageSupportName = "org.epics.ioc.pdrvFloat64AverageFactory";
    private static final String pdrvFloat64OutputSupportName = "org.epics.ioc.pdrvFloat64OutputFactory";
}
