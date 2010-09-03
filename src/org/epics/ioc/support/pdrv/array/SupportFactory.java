 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.array;

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
            pvRecordStructure.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvRecordStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        
        if(supportName.equals(pdrvInt32ArrayInputSupportName))
            return new BaseInt32ArrayInput(pvRecordStructure,pdrvInt32ArrayInputSupportName);
        if(supportName.equals(pdrvInt32ArrayInterruptSupportName))
            return new BaseInt32ArrayInterrupt(pvRecordStructure,pdrvInt32ArrayInterruptSupportName);
        if(supportName.equals(pdrvInt32ArrayOutputSupportName))
            return new BaseInt32ArrayOutput(pvRecordStructure,pdrvInt32ArrayOutputSupportName);
        
        if(supportName.equals(pdrvFloat64ArrayInputSupportName))
            return new BaseFloat64ArrayInput(pvRecordStructure,pdrvFloat64ArrayInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInterruptSupportName))
            return new BaseFloat64ArrayInterrupt(pvRecordStructure,pdrvFloat64ArrayInterruptSupportName);
        if(supportName.equals(pdrvFloat64ArrayOutputSupportName))
            return new BaseFloat64ArrayOutput(pvRecordStructure,pdrvFloat64ArrayOutputSupportName);
        pvRecordStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String pdrvInt32ArrayInputSupportName = "org.epics.ioc.pdrvInt32ArrayInputFactory";
    private static final String pdrvInt32ArrayInterruptSupportName = "org.epics.ioc.pdrvInt32ArrayInterruptFactory";
    private static final String pdrvInt32ArrayOutputSupportName = "org.epics.ioc.pdrvInt32ArrayOutputFactory";
   
    private static final String pdrvFloat64ArrayInputSupportName = "org.epics.ioc.pdrvFloat64ArrayInputFactory";
    private static final String pdrvFloat64ArrayInterruptSupportName = "org.epics.ioc.pdrvFloat64ArrayInterruptFactory";
    private static final String pdrvFloat64ArrayOutputSupportName = "org.epics.ioc.pdrvFloat64ArrayOutputFactory";
}
