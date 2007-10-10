/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVArray;

/**
 * @author mrk
 *
 */
public class BaseCDArray extends BaseCDField implements CDArray {
    private PVArray pvArray;
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvArray The pvArray that this CDArray references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDArray(
        CDField parent,CDRecord cdRecord,
        PVArray pvArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvArray,supportAlso);
        this.pvArray = pvArray; 
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDArray#getPVArray()
     */
    public PVArray getPVArray() {
        return pvArray;
    }
}
