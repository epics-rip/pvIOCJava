 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.support.Support;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.ScalarType;


/**
 * Factory to create support for Channel Access links.
 * @author mrk
 *
 */
public class LinkFactory {
    /**
     * Create link support for database links.
     * @param pvField The field for which to create support.
     * @return A Support interface or null if the support is not found.
     */
    public static Support create(PVField pvField) {
        PVAuxInfo pvAuxInfo = pvField.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvField.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvField.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        if(supportName.equals(dbProcessLinkName + "Factory")) {
            return new ProcessLinkBase(dbProcessLinkName,pvField);
        } else if(supportName.equals(dbInputLinkName + "Factory")) {
            return new InputLinkBase(dbInputLinkName,pvField);
        } else if(supportName.equals(dbOutputLinkName + "Factory")) {
            return new OutputLinkBase(dbOutputLinkName,pvField);
        }
        pvField.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String dbProcessLinkName = "dbProcessLink";
    private static final String dbInputLinkName = "dbInputLink";
    private static final String dbOutputLinkName = "dbOutputLink";
    
}
