 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

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
     * Create link support for Channel Access links.
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
        if(supportName.equals(caProcessLinkName + "Factory")) {
            return new ProcessLinkBase(caProcessLinkName,pvRecordField);
        } else if(supportName.equals(caInputLinkName + "Factory")) {
            return new InputLinkBase(caInputLinkName,pvRecordField);
        } else if(supportName.equals(caOutputLinkName + "Factory")) {
            return new OutputLinkBase(caOutputLinkName,pvRecordField);
        } else if(supportName.equals(caMonitorLinkName + "Factory")) {
            return new MonitorLinkBase(caMonitorLinkName,pvRecordField);
        } else if(supportName.equals(caMonitorNotifyLinkName + "Factory")) {
            return new MonitorNotifyLinkBase(caMonitorNotifyLinkName,pvRecordField);
        }
        pvRecordField.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String caProcessLinkName = "org.epics.pvioc.caProcessLink";
    private static final String caInputLinkName = "org.epics.pvioc.caInputLink";
    private static final String caOutputLinkName = "org.epics.pvioc.caOutputLink";
    private static final String caMonitorLinkName = "org.epics.pvioc.caMonitorLink";
    private static final String caMonitorNotifyLinkName = "org.epics.pvioc.caMonitorNotifyLink";    
}
