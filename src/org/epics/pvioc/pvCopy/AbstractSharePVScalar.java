/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvioc.database.PVRecord;

/**
 * @author mrk
 *
 */
public abstract class AbstractSharePVScalar extends AbstractSharePVField implements PVScalar{
    /**
     * Constructor.
     * @param parent The parent.
     * @param scalar The ScalarType.
     */
    protected AbstractSharePVScalar(PVRecord pvRecord, PVStructure parent,PVScalar  pvShare) {
        super(pvRecord,parent,pvShare);
    }
	/* (non-Javadoc)
	 * @see org.epics.pvdata.pv.PVScalar#getScalar()
	 */
	@Override
	public Scalar getScalar() {
		return (Scalar)super.getField();
	}
}
