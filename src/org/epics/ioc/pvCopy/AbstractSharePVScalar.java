/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvCopy;

import org.epics.ioc.database.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;

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
	 * @see org.epics.pvData.pv.PVScalar#getScalar()
	 */
	@Override
	public Scalar getScalar() {
		return (Scalar)super.getField();
	}
}
