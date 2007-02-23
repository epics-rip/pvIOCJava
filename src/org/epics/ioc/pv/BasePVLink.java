/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;


/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class BasePVLink extends AbstractPVData implements PVLink {
    private PVStructure pvStructure= null;
    
    public BasePVLink(PVData parent,Field field) {
        super(parent,field);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#getConfigurationStructure()
     */
    public PVStructure getConfigurationStructure() {
        return pvStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#setConfigurationStructure(org.epics.ioc.pv.PVStructure)
     */
    public boolean setConfigurationStructure(PVStructure pvStructure) {
        this.pvStructure = pvStructure;
        return true;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractPVData#toString()
     */
    public String toString() {
        return toString(0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractPVData#toString(int)
     */
    public String toString(int indentLevel) {
        if(pvStructure==null) return "";
        return "isLink " + super.toString(indentLevel) + pvStructure.toString(indentLevel);
    }
}
