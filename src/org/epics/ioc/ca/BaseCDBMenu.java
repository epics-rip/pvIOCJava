/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVData;
import org.epics.ioc.pv.PVEnum;
import org.epics.ioc.pv.PVMenu;

/**
 * @author mrk
 *
 */
public class BaseCDBMenu extends BaseCDBEnum implements CDBMenu{
    private PVMenu pvMenu;

    public BaseCDBMenu(
        CDBData parent,CDBRecord cdbRecord,PVData pvData)
    {
        super(parent,cdbRecord,pvData);
        pvMenu= (PVMenu)pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBMenu#getPVMenu()
     */
    public PVMenu getPVMenu() {
        return pvMenu;
    }
}
