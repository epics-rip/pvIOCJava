/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Base class for DBMenu.
 * @author mrk
 *
 */
public class BaseDBMenu extends BaseDBEnum implements DBMenu
{ 
    /**
     * Constructor for BasePVMenu
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvField The reflection interface.
     */
    public BaseDBMenu(DBField parent,DBRecord record, PVField pvField) {
        super(parent,record,pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBMenu#getPVMenu()
     */
    public PVMenu getPVMenu() {
        return (PVMenu)super.getPVField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBMenu#replacePVMenu()
     */
    public void replacePVMenu() {
        super.replacePVEnum();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        throw new UnsupportedOperationException(
            "Menu choices can not be modified");
    }
}
