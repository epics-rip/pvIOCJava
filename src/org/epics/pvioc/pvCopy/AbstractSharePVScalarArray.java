/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvioc.database.PVRecord;


/**
 * Base class for implementing PVBooleanArray.
 * @author mrk
 *
 */
/**
 * @author mrk
 *
 */
public abstract class AbstractSharePVScalarArray extends AbstractSharePVArray implements PVScalarArray
{
    /**
     * Constructor.
     * @param pvRecord The record.
     * @param parent The parent.
     * @param pvShare The field.
     */
    protected AbstractSharePVScalarArray(PVRecord pvRecord,PVStructure parent,PVArray pvShare)
    {
        super(pvRecord,parent,pvShare);
    }
	@Override
	public ScalarArray getScalarArray() {
		return (ScalarArray)super.getField();
	}        
    
}
