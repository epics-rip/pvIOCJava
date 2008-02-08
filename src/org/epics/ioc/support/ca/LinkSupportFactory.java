 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.db.DBStructure;
import org.epics.ioc.support.Support;
import org.epics.ioc.util.MessageType;

/**
 * Factory to create support for Channel Access links.
 * @author mrk
 *
 */
public class LinkSupportFactory {
    /**
     * Create link support for Channel Access links.
     * @param dbStructure The field for which to create support.
     * @return A Support interface or null if the support is not found.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        if(supportName.equals(processSupportName)) {
            return new ProcessSupportBase(processSupportName,dbStructure);
        } else if(supportName.equals(inputSupportName)) {
            return new InputSupportBase(inputSupportName,dbStructure);
        } else if(supportName.equals(outputSupportName)) {
            return new OutputSupportBase(outputSupportName,dbStructure);
        } else if(supportName.equals(monitorSupportName)) {
            return new MonitorSupportBase(monitorSupportName,dbStructure);
        } else if(supportName.equals(monitorNotifySupportName)) {
            return new MonitorNotifySupportBase(monitorNotifySupportName,dbStructure);
        }
        dbStructure.getPVStructure().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processSupportName = "processSupport";
    private static final String inputSupportName = "inputSupport";
    private static final String outputSupportName = "outputSupport";
    private static final String monitorSupportName = "monitorSupport";
    private static final String monitorNotifySupportName = "monitorNotifySupport";    
}
