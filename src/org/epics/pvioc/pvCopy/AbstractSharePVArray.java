/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import java.nio.ByteBuffer;

import org.epics.pvdata.pv.DeserializableControl;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.SerializableControl;
import org.epics.pvioc.database.PVRecord;


/**
 * Base class for implementing PVBooleanArray.
 * @author mrk
 *
 */
public abstract class AbstractSharePVArray extends AbstractSharePVField implements PVArray
{
    private PVArray pvShare;
	/**
     * Constructor.
     * @param parent The parent.
     * @param array The Introspection interface.
     */
    protected AbstractSharePVArray(PVRecord pvRecord,PVStructure parent,PVArray pvShare)
    {
        super(pvRecord,parent,pvShare);
        this.pvShare = pvShare;
    }        
	/* (non-Javadoc)
     * @see org.epics.pvdata.pv.Serializable#serialize(java.nio.ByteBuffer, org.epics.pvdata.pv.SerializableControl)
     */
    @Override
    public void serialize(ByteBuffer buffer, SerializableControl flusher) {
        serialize(buffer, flusher, 0, -1);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#setLength(int)
     */
    @Override
    public void setLength(int len) {
        lockShare();
        try {
            pvShare.setLength(len);
        } finally {
            unlockShare();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#getCapacity()
     */
    @Override
    public int getCapacity() {
        return pvShare.getCapacity();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#getLength()
     */
    @Override
    public int getLength() {
        return pvShare.getLength();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#isCapacityMutable()
     */
    @Override
    public boolean isCapacityMutable() {
        return pvShare.isCapacityMutable();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#setCapacityMutable(boolean)
     */
    @Override
    public void setCapacityMutable(boolean isMutable) {
        lockShare();
        try {
            pvShare.setCapacityMutable(isMutable);
        } finally {
            unlockShare();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#setCapacity(int)
     */
    public void setCapacity(int len) {
        lockShare();
        try {
            pvShare.setCapacity(len);
        } finally {
            unlockShare();
        }
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
	    lockShare();
        try {
	        return pvShare.equals(obj);
        } finally {
            unlockShare();
        }
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    lockShare();
        try {
            return pvShare.hashCode();
        } finally {
            unlockShare();
        }
	}
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.SerializableArray#serialize(java.nio.ByteBuffer, org.epics.pvdata.pv.SerializableControl, int, int)
     */
    @Override
    public void serialize(ByteBuffer buffer, SerializableControl flusher, int offset, int count) {
        lockShare();		// TODO this can block !!!
        try {
            pvShare.serialize(buffer, flusher, offset, count);
        } finally {
            unlockShare();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.Serializable#deserialize(java.nio.ByteBuffer, org.epics.pvdata.pv.DeserializableControl)
     */
    @Override
    public void deserialize(ByteBuffer buffer, DeserializableControl control) {
        lockShare();	// TODO this can block !!!
        try {
        pvShare.deserialize(buffer, control);
        } finally {
            unlockShare();
        }
    }
}
