 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.serial;

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
        if(supportName.equals(pdrvSerialInputName))
            return new BaseSerialInput(pvRecordStructure,pdrvSerialInputName);
        if(supportName.equals(pdrvSerialInterruptName))
            return new BaseSerialInterrupt(pvRecordStructure,pdrvSerialInterruptName);
        if(supportName.equals(pdrvSerialOutputName))
            return new BaseSerialOutput(pvRecordStructure,pdrvSerialOutputName);
        if(supportName.equals(pdrvSerialDiscardName))
            return new BaseSerialDiscard(pvRecordStructure,pdrvSerialDiscardName);
        if(supportName.equals(pdrvScalarCommandName))
            return new BaseScalarCommand(pvRecordStructure,pdrvScalarCommandName);
        if(supportName.equals(pdrvScalarQueryName))
            return new BaseScalarQuery(pvRecordStructure,pdrvScalarQueryName);
        if(supportName.equals(pdrvStringNoopName))
            return new BaseStringNoop(pvRecordStructure,pdrvStringNoopName);
        
        pvRecordStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvSerialInputName = "org.epics.pvioc.pdrvSerialInputFactory";
    private static final String pdrvSerialInterruptName = "org.epics.pvioc.pdrvSerialInterruptFactory";
    private static final String pdrvSerialOutputName = "org.epics.pvioc.pdrvSerialOutputFactory"; 
    private static final String pdrvSerialDiscardName = "org.epics.pvioc.pdrvSerialDiscardFactory"; 
    private static final String pdrvScalarCommandName = "org.epics.pvioc.pdrvScalarCommandFactory"; 
    private static final String pdrvScalarQueryName = "org.epics.pvioc.pdrvScalarQueryFactory"; 
    private static final String pdrvStringNoopName = "org.epics.pvioc.pdrvStringNoopFactory"; 
}
