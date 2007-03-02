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
public class BasePVLink extends AbstractPVField implements PVLink {
    private PVStructure pvStructure= null;
    
    /**
     * Constructor.
     * @param parent The parent interface.
     * @param field The introspection interface.
     */
    public BasePVLink(PVField parent,Field field) {
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
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        if(pvStructure==null) return "";
        return "isLink " + super.toString(indentLevel) + pvStructure.toString(indentLevel);
    }
}
