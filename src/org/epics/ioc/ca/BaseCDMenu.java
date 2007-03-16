/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Base class for a CDMenu (Channel Data Menu).
 * @author mrk
 *
 */
public class BaseCDMenu extends BaseCDEnum implements CDMenu{
    private PVMenu pvMenu;

    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvField The pvField that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDMenu(
        CDField parent,CDRecord cdRecord,PVField pvField,boolean supportAlso)
    {
        super(parent,cdRecord,pvField,supportAlso);
        pvMenu= (PVMenu)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMenu#getPVMenu()
     */
    public PVMenu getPVMenu() {
        return pvMenu;
    }
}
