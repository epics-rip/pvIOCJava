/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.util.*;


/**
 * Extends AbstractSupport to implement getConfigStructure.
 * @author mrk
 *
 */
public class AbstractLinkSupport extends AbstractSupport implements LinkSupport{
    
    /**
     * Constructor.
     * This must be called by any class that extends AbstractSupport.
     * @param name The support name.
     * @param dbField The DBdata which is supported.
     * This can be a record or any field in a record.
     */
    protected AbstractLinkSupport(String name,DBField dbField) {
        super(name,dbField);
    }     
    /* (non-Javadoc)
     * @see org.epics.ioc.support.LinkSupport#getConfigStructure(java.lang.String, boolean)
     */
    public PVStructure getConfigStructure(String structureName, boolean reportFailure) {
        PVField pvField = super.getDBField().getPVField();
        if(pvField.getField().getType()!=Type.pvLink) {
            pvField.message("field is not a link", MessageType.fatalError);
            return null;
        }
        PVLink pvLink = (PVLink)pvField;
        PVStructure configStructure = pvLink.getConfigurationStructure();
        if(configStructure==null) {
            if(reportFailure) {
                pvField.message("no configuration structure", MessageType.fatalError);
            }
            return null;
        }
        Structure structure = (Structure)configStructure.getField();
        String configStructureName = structure.getStructureName();
        if(!configStructureName.equals(structureName)) {
            pvField.message(
                    "configurationStructure name is " + configStructureName
                    + " but expecting " + structureName,
                MessageType.fatalError);
            return null;
        }
        return configStructure;
    }
}
