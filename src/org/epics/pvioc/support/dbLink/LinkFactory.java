 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.dbLink;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.Support;


/**
 * Factory to create support for Channel Access links.
 * @author mrk
 *
 */
public class LinkFactory {
    /**
     * Create link support for database links.
     * @param pvRecordField The field for which to create support.
     * @return A Support interface or null if the support is not found.
     */
    public static Support create(PVRecordField pvRecordField) {
        PVAuxInfo pvAuxInfo = pvRecordField.getPVField().getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvRecordField.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvRecordField.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        if(supportName.equals(dbProcessLinkName + "Factory")) {
            return new ProcessLinkBase(dbProcessLinkName,pvRecordField);
        } else if(supportName.equals(dbInputLinkName + "Factory")) {
            return new InputLinkBase(dbInputLinkName,pvRecordField);
        } else if(supportName.equals(dbOutputLinkName + "Factory")) {
            return new OutputLinkBase(dbOutputLinkName,pvRecordField);
        }
        pvRecordField.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String dbProcessLinkName = "org.epics.pvioc.dbProcessLink";
    private static final String dbInputLinkName = "org.epics.pvioc.dbInputLink";
    private static final String dbOutputLinkName = "org.epics.pvioc.dbOutputLink";
    
}
