 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

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
        if(supportName.equals(pdrvSerialInputName))
            return new BaseSerialInput(pvStructure,pdrvSerialInputName);
        if(supportName.equals(pdrvSerialInterruptName))
            return new BaseSerialInterrupt(pvStructure,pdrvSerialInterruptName);
        if(supportName.equals(pdrvSerialOutputName))
            return new BaseSerialOutput(pvStructure,pdrvSerialOutputName);
        if(supportName.equals(pdrvSerialDiscardName))
            return new BaseSerialDiscard(pvStructure,pdrvSerialDiscardName);
        if(supportName.equals(pdrvScalarCommandName))
            return new BaseScalarCommand(pvStructure,pdrvScalarCommandName);
        if(supportName.equals(pdrvScalarQueryName))
            return new BaseScalarQuery(pvStructure,pdrvScalarQueryName);
        if(supportName.equals(pdrvStringNoopName))
            return new BaseStringNoop(pvStructure,pdrvStringNoopName);
        
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvSerialInputName = "pdrvSerialInputFactory";
    private static final String pdrvSerialInterruptName = "pdrvSerialInterruptFactory";
    private static final String pdrvSerialOutputName = "pdrvSerialOutputFactory"; 
    private static final String pdrvSerialDiscardName = "pdrvSerialDiscardFactory"; 
    private static final String pdrvScalarCommandName = "pdrvScalarCommandFactory"; 
    private static final String pdrvScalarQueryName = "pdrvScalarQueryFactory"; 
    private static final String pdrvStringNoopName = "pdrvStringNoopFactory"; 
}
