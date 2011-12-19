/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvCopy;

import org.epics.ioc.database.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.PVStructureArray;
import org.epics.pvData.pv.StructureArray;
import org.epics.pvData.pv.StructureArrayData;


/**
 * Base class for implementing PVBooleanArray.
 * @author mrk
 *
 */
/**
 * @author mrk
 *
 */
public class BaseSharePVStructureArray extends AbstractSharePVArray implements PVStructureArray
{
	private PVStructureArray pvShare;
    /**
     * Constructor.
     * @param parent The parent.
     * @param array The Introspection interface.
     */
    protected BaseSharePVStructureArray(PVRecord pvRecord,PVStructure parent,PVStructureArray pvShare)
    {
        super(pvRecord,parent,pvShare);
        this.pvShare = pvShare;
    }
	@Override
	public int get(int offset, int length, StructureArrayData data) {
		super.lockShare();
		try {
			return pvShare.get(offset, length, data);
		} finally {
			super.unlockShare();
		}
	}
	@Override
	public StructureArray getStructureArray() {
		return pvShare.getStructureArray();
	}
	@Override
	public int put(int offset, int length, PVStructure[] from, int fromOffset) {
		super.lockShare();
		try {
			return pvShare.put(offset, length, from, fromOffset);
		} finally {
			super.unlockShare();
		}
	}
	@Override
	public void shareData(PVStructure[] from) {
		throw new IllegalStateException("shareData not legal in this context");
	}
}