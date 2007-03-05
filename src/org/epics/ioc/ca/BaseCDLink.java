/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Base class for a CDLink (Channel Data Link).
 * @author mrk
 *
 */
public class BaseCDLink extends BaseCDField implements CDLink {
    private PVLink pvLink;
    private int numConfigurationStructurePuts;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvField The pvField that this CDField references.
     */
    public BaseCDLink(
        CDField parent,CDRecord cdRecord,PVField pvField)
    {
        super(parent,cdRecord,pvField);
        pvLink = (PVLink)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        numConfigurationStructurePuts = 0;
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDLink#configurationStructurePut(org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVLink targetPVLink) {
        pvLink.setConfigurationStructure(targetPVLink.getConfigurationStructure());
        numConfigurationStructurePuts++;
        super.setMaxNumPuts(numConfigurationStructurePuts);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDLink#getNumConfigurationStructurePuts()
     */
    public int getNumConfigurationStructurePuts() {
        int num = numConfigurationStructurePuts;
        numConfigurationStructurePuts = 0;
        return num;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDLink#getPVLink()
     */
    public PVLink getPVLink() {
        return pvLink;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        configurationStructurePut((PVLink)targetPVField);
        numConfigurationStructurePuts++;
        super.incrementNumPuts();
    }
}
