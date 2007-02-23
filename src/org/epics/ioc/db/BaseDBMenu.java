/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Abstract base class for DBMenu.
 * @author mrk
 *
 */
public class BaseDBMenu extends BaseDBEnum implements DBMenu
{ 
    /**
     * Constructor for BasePVMenu
     * @param parent The parent.
     * @param menu The introspection interface.
     * @param choice The array of choices.
     */
    public BaseDBMenu(DBData parent,DBRecord record, PVData pvData) {
        super(parent,record,pvData);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        throw new UnsupportedOperationException(
            "Menu choices can not be modified");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBMenu#getPVMenu()
     */
    public PVMenu getPVMenu() {
        return (PVMenu)super.getPVData();
    }
}
