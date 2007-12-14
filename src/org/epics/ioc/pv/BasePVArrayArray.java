/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.MessageType;

/**
 * Base class for a PVStructure.
 * @author mrk
 *
 */
public class BasePVArrayArray extends AbstractPVArray implements PVArrayArray
{
    private Convert convert = ConvertFactory.getConvert();
    private PVArray[] value;
    
    /**
     * Constructor
     * @param parent The parent
     * @param array The Array interface.
     * @param capacity The initial capacity.
     * @param capacityMutable Is the capacity mutable.
     */
    public BasePVArrayArray(PVField parent,Array array,
        int capacity,boolean capacityMutable)
    {
        super(parent,array,capacity,capacityMutable);
        value = new PVArray[capacity];
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#getSubField(java.lang.String)
     */
    @Override
    public PVField getSubField(String fieldName) {
        for(PVArray pvArray : value) {
            if(pvArray.getField().getFieldName().equals(fieldName)) return pvArray;
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel)
        + super.toString(indentLevel);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
     */
    public void setCapacity(int len) {
        if(!capacityMutable) {
            super.message("not capacityMutable", MessageType.error);
            return;
        }
        super.asynAccessCallListener(true);
        try {
            if(length>len) length = len;
            PVArray[]newarray = new PVArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        } finally {
            super.asynAccessCallListener(false);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArrayArray#get(int, int, org.epics.ioc.pv.ArrayArrayData)
     */
    public int get(int offset, int len, ArrayArrayData data) {
        super.asynAccessCallListener(true);
        try {
            int n = len;
            if(offset+len > length) n = length - offset;
            data.data = value;
            data.offset = offset;
            return n;
        } finally {
            super.asynAccessCallListener(false);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArrayArray#put(int, int, org.epics.ioc.pv.PVArray[], int)
     */
    public int put(int offset, int len, PVArray[]from, int fromOffset) {
        if(!super.isMutable()) {
            super.message("not isMutable", MessageType.error);
            return 0;
        }
        super.asynAccessCallListener(true);
        try {
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) {
                    setCapacity(newlength);
                    newlength = capacity;
                    len = newlength - offset;
                    if(len<=0) return 0;
                }
                length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        } finally {
            super.asynAccessCallListener(false);
        }
    }
    
    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        convert.newLine(builder,indentLevel);
        builder.append("{");
        for(int i=0; i < length; i++) {
            convert.newLine(builder,indentLevel + 1);
            if(value[i]==null) {
                builder.append("{}");
            } else {
                builder.append(value[i].toString(indentLevel+1));
            }
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
