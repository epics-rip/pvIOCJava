/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import java.nio.ByteBuffer;

import org.epics.pvdata.factory.AbstractPVField;
import org.epics.pvdata.pv.DeserializableControl;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.SerializableControl;
import org.epics.pvioc.database.PVRecord;

/**
 * @author mrk
 *
 */
public abstract class AbstractSharePVField extends AbstractPVField {
	protected PVRecord pvRecord = null;
    private PVField pvShare = null;
    /**
     * Constructor.
     * @param parent The parent.
     * @param scalar The ScalarType.
     */
    protected AbstractSharePVField(PVRecord pvRecord,PVStructure parent, PVField  pvShare) {
        super(parent,pvShare.getField());
        this.pvRecord = pvRecord;
        this.pvShare = pvShare;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVField#toString(java.lang.StringBuilder, int)
     */
    @Override
    public void toString(StringBuilder buf, int indentLevel) {
        convert.getString(buf, this, indentLevel);
    }
    /**
     * Lock the shared record.
     */
    protected void lockShare() { pvRecord.lock(); }
    /**
     * Unlock the shared record
     */
    protected void unlockShare() { pvRecord.unlock(); }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVField#setImmutable()
     */
    @Override
    public void setImmutable() {
        lockShare();
        try {
            pvShare.setImmutable();
        } finally {
            unlockShare();
        }
        super.setImmutable();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) { return pvShare.equals(obj); }
       
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {return pvShare.hashCode();}

    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.Serializable#deserialize(java.nio.ByteBuffer, org.epics.pvdata.pv.DeserializableControl)
     */
    @Override
    public void deserialize(ByteBuffer buffer, DeserializableControl control) {
        lockShare();
        try {
        pvShare.deserialize(buffer, control);
        } finally {
            unlockShare();
        }
    }

    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.Serializable#serialize(java.nio.ByteBuffer, org.epics.pvdata.pv.SerializableControl)
     */
    @Override
    public void serialize(ByteBuffer buffer, SerializableControl flusher) {
        lockShare();
        try {
            pvShare.serialize(buffer, flusher);
        } finally {
            unlockShare();
        }
    }
}
