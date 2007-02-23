/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class BaseCDBLink extends BaseCDBData implements CDBLink {
    private PVLink pvLink;
    private int numConfigurationStructurePuts;
    
    public BaseCDBLink(
        CDBData parent,CDBRecord cdbRecord,PVData pvData)
    {
        super(parent,cdbRecord,pvData);
        pvLink = (PVLink)pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLink#configurationStructurePut(org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVLink targetPVLink) {
        pvLink.setConfigurationStructure(targetPVLink.getConfigurationStructure());
        numConfigurationStructurePuts++;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLink#getNumConfigurationStructurePuts()
     */
    public int getNumConfigurationStructurePuts() {
        int num = numConfigurationStructurePuts;
        numConfigurationStructurePuts = 0;
        return num;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLink#getPVLink()
     */
    public PVLink getPVLink() {
        return pvLink;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDBData#dataPut(org.epics.ioc.pv.PVData)
     */
    public void dataPut(PVData targetPVData) {
        configurationStructurePut((PVLink)targetPVData);
    }
}
