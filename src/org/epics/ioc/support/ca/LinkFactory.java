 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

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
     * Create link support for Channel Access links.
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
        if(supportName.equals(caProcessLinkName + "Factory")) {
            return new ProcessLinkBase(caProcessLinkName,pvField);
        } else if(supportName.equals(caInputLinkName + "Factory")) {
            return new InputLinkBase(caInputLinkName,pvField);
        } else if(supportName.equals(caOutputLinkName + "Factory")) {
            return new OutputLinkBase(caOutputLinkName,pvField);
        } else if(supportName.equals(caMonitorLinkName + "Factory")) {
            return new MonitorLinkBase(caMonitorLinkName,pvField);
        } else if(supportName.equals(caMonitorNotifyLinkName + "Factory")) {
            return new MonitorNotifyLinkBase(caMonitorNotifyLinkName,pvField);
        }
        pvField.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String caProcessLinkName = "org.epics.ioc.caProcessLink";
    private static final String caInputLinkName = "org.epics.ioc.caInputLink";
    private static final String caOutputLinkName = "org.epics.ioc.caOutputLink";
    private static final String caMonitorLinkName = "org.epics.ioc.caMonitorLink";
    private static final String caMonitorNotifyLinkName = "org.epics.ioc.caMonitorNotifyLink";    
}
