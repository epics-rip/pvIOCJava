 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.support.Support;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;

/**
 * Factory to create support for Channel Access links.
 * @author mrk
 *
 */
public class LinkSupportFactory {
    /**
     * Create link support for Channel Access links.
     * @param pvStructure The field for which to create support.
     * @return A Support interface or null if the support is not found.
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
        if(supportName.equals(processSupportName + "Factory")) {
            return new ProcessSupportBase(processSupportName,pvStructure);
        } else if(supportName.equals(inputSupportName + "Factory")) {
            return new InputSupportBase(inputSupportName,pvStructure);
        } else if(supportName.equals(outputSupportName + "Factory")) {
            return new OutputSupportBase(outputSupportName,pvStructure);
        } else if(supportName.equals(monitorSupportName + "Factory")) {
            return new MonitorSupportBase(monitorSupportName,pvStructure);
        } else if(supportName.equals(monitorNotifySupportName + "Factory")) {
            return new MonitorNotifySupportBase(monitorNotifySupportName,pvStructure);
        }
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processSupportName = "processSupport";
    private static final String inputSupportName = "inputSupport";
    private static final String outputSupportName = "outputSupport";
    private static final String monitorSupportName = "monitorSupport";
    private static final String monitorNotifySupportName = "monitorNotifySupport";    
}
