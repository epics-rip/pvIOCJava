/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public class AsynLink implements LinkSupport {
    private static String supportName = "AsynLink";
    private DBLink dbLink = null;
    
    public AsynLink(DBLink dbLink) {
        this.dbLink = dbLink;
    }

    public String getName() {
        return supportName;
    }

    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    public void initialize() {
        // TODO Auto-generated method stub
        
    }

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }
    public LinkReturn process(RecordProcess recordProcess, RecordSupport recordSupport) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean setField(PVData field) {
        // TODO Auto-generated method stub
        return false;
    }

    
}
