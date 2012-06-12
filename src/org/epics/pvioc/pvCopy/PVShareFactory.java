/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;
import java.nio.ByteBuffer;

import org.epics.pvdata.pv.BooleanArrayData;
import org.epics.pvdata.pv.ByteArrayData;
import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.FloatArrayData;
import org.epics.pvdata.pv.IntArrayData;
import org.epics.pvdata.pv.LongArrayData;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVByteArray;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVFloatArray;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVIntArray;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVLongArray;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVShortArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.SerializableControl;
import org.epics.pvdata.pv.ShortArrayData;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvioc.database.PVRecord;

/**
 * Create a PVField that shares the data from another PVField.
 * The original pvField is replaced by the newly created PVField.
 * @author mrk
 *
 */
public class PVShareFactory {
    /**
     * Replace pvNow with an implementation that shares the data from pvShare.
     * The original pvNow is replaced with the new implementation.
     * When a get of put is made to the new PVField the get or put method of the shared
     * PVField is called.
     * @param pvNow The original PVScalar to replace.
     * @param pvShare The field from which data will be shared.
     * @return The newly created PVScalar.
     */
    public static PVScalar replace(PVRecord pvRecord,PVScalar pvNow,PVScalar pvShare) {
        PVScalar newPVField = createScalar(pvRecord,pvNow.getParent(),(PVScalar)pvShare);
        pvNow.getParent().replacePVField(pvNow,newPVField);
        return newPVField;
    }
    /**
     * Replace pvNow with an implementation that shares the data from pvShare.
     * The original pvNow is replaced with the new implementation.
     * When a get of put is made to the new PVField the get or put method of the shared
     * PVField is called.
     * @param pvNow The original PVScalar to replace.
     * @param pvShare The field from which data will be shared.
     * @return The newly created PVScalar.
     */
    public static PVArray replace(PVRecord pvRecord,PVArray pvNow,PVArray pvShare) {
        PVArray newPVField = createArray(pvRecord,pvNow.getParent(),(PVArray)pvShare);
        pvNow.getParent().replacePVField(pvNow,newPVField);
        return newPVField;
    }
    
    public static PVStructureArray replace(PVRecord pvRecord,PVStructureArray pvNow,PVStructureArray pvShare) {
    	PVStructureArray pvStructureArray = new BaseSharePVStructureArray(pvRecord,pvNow.getParent(),pvShare);
    	pvNow.getParent().replacePVField(pvNow,pvStructureArray);
    	return pvStructureArray;
    }
    
    
    private static PVScalar createScalar(PVRecord pvRecord,PVStructure pvParent,PVScalar pvShare) {
        Scalar scalar = pvShare.getScalar();
        switch(scalar.getScalarType()) {
        case pvBoolean:
            return new SharePVBooleanImpl(pvRecord,pvParent,(PVBoolean)pvShare);
        case pvByte:
            return new SharePVByteImpl(pvRecord,pvParent,(PVByte)pvShare);
        case pvShort:
            return new SharePVShortImpl(pvRecord,pvParent,(PVShort)pvShare);
        case pvInt:
            return new SharePVIntImpl(pvRecord,pvParent,(PVInt)pvShare);
        case pvLong:
            return new SharePVLongImpl(pvRecord,pvParent,(PVLong)pvShare);
        case pvFloat:
            return new SharePVFloatImpl(pvRecord,pvParent,(PVFloat)pvShare);
        case pvDouble:
            return new SharePVDoubleImpl(pvRecord,pvParent,(PVDouble)pvShare);
        case pvString:
            return new SharePVStringImpl(pvRecord,pvParent,(PVString)pvShare);
        }
        return null;
    }
    
    private static PVArray createArray(PVRecord pvRecord,PVStructure pvParent,PVArray pvShare) {
    	PVScalarArray pvScalarArray = (PVScalarArray)pvShare;
        ScalarArray array = pvScalarArray.getScalarArray();
        switch(array.getElementType()) {
        case pvBoolean:
            return new SharePVBooleanArrayImpl(pvRecord,pvParent,(PVBooleanArray)pvShare);
        case pvByte:
            return new SharePVByteArrayImpl(pvRecord,pvParent,(PVByteArray)pvShare);
        case pvShort:
            return new SharePVShortArrayImpl(pvRecord,pvParent,(PVShortArray)pvShare);
        case pvInt:
            return new SharePVIntArrayImpl(pvRecord,pvParent,(PVIntArray)pvShare);
        case pvLong:
            return new SharePVLongArrayImpl(pvRecord,pvParent,(PVLongArray)pvShare);
        case pvFloat:
            return new SharePVFloatArrayImpl(pvRecord,pvParent,(PVFloatArray)pvShare);
        case pvDouble:
            return new SharePVDoubleArrayImpl(pvRecord,pvParent,(PVDoubleArray)pvShare);
        case pvString:
            return new SharePVStringArrayImpl(pvRecord,pvParent,(PVStringArray)pvShare);
        }
        return null;
    }
    
    private static class SharePVBooleanImpl extends AbstractSharePVScalar implements PVBoolean
    {
        private PVBoolean pvShare = null;
        
        private SharePVBooleanImpl(PVRecord pvRecord,PVStructure parent,PVBoolean pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVBoolean#get()
         */
        @Override
        public boolean get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVBoolean#put(boolean)
         */
        @Override
        public void put(boolean value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
       
    }
    
    private static class SharePVByteImpl extends AbstractSharePVScalar implements PVByte
    {
        private PVByte pvShare = null;
        
        private SharePVByteImpl(PVRecord pvRecord,PVStructure parent,PVByte pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVByte#get()
         */
        @Override
        public byte get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVByte#put(byte)
         */
        @Override
        public void put(byte value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVShortImpl extends AbstractSharePVScalar implements PVShort
    {
        private PVShort pvShare = null;
        
        private SharePVShortImpl(PVRecord pvRecord,PVStructure parent,PVShort pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVShort#get()
         */
        @Override
        public short get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVShort#put(short)
         */
        @Override
        public void put(short value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVIntImpl extends AbstractSharePVScalar implements PVInt
    {
        private PVInt pvShare = null;
        
        private SharePVIntImpl(PVRecord pvRecord,PVStructure parent,PVInt pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVInt#get()
         */
        @Override
        public int get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVInt#put(int)
         */
        @Override
        public void put(int value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVLongImpl extends AbstractSharePVScalar implements PVLong
    {
        private PVLong pvShare = null;
        
        private SharePVLongImpl(PVRecord pvRecord,PVStructure parent,PVLong pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVLong#get()
         */
        @Override
        public long get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVLong#put(long)
         */
        @Override
        public void put(long value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVFloatImpl extends AbstractSharePVScalar implements PVFloat
    {
        private PVFloat pvShare = null;
        
        private SharePVFloatImpl(PVRecord pvRecord,PVStructure parent,PVFloat pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVFloat#get()
         */
        @Override
        public float get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVFloat#put(float)
         */
        @Override
        public void put(float value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVDoubleImpl extends AbstractSharePVScalar implements PVDouble
    {
        private PVDouble pvShare = null;
        
        private SharePVDoubleImpl(PVRecord pvRecord,PVStructure parent,PVDouble pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDouble#get()
         */
        @Override
        public double get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDouble#put(double)
         */
        @Override
        public void put(double value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
    }
    
    private static class SharePVStringImpl extends AbstractSharePVScalar implements PVString
    {
        private PVString pvShare = null;
        
        private SharePVStringImpl(PVRecord pvRecord,PVStructure parent,PVString pvShare) {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVString#get()
         */
        @Override
        public String get() {
            super.lockShare();
            try {
                return pvShare.get();
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVString#put(String)
         */
        @Override
        public void put(String value) {
            super.lockShare();
            try {
                pvShare.put(value);
            } finally {
                super.unlockShare();
            }
            super.postPut();
        }      
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.SerializableArray#serialize(java.nio.ByteBuffer, org.epics.pvdata.pv.SerializableControl, int, int)
         */
        public void serialize(ByteBuffer buffer, SerializableControl flusher, int offset, int count) {
            lockShare();
            try {
                pvShare.serialize(buffer, flusher, offset, count);
            } finally {
                unlockShare();
            }
        }
    }
    
    
    
    private static class SharePVBooleanArrayImpl extends AbstractSharePVScalarArray implements PVBooleanArray
    {
        private PVBooleanArray pvShare;
        
        private SharePVBooleanArrayImpl(PVRecord pvRecord,PVStructure parent,PVBooleanArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVBooleanArray#get(int, int, org.epics.pvdata.pv.BooleanArrayData)
         */
        @Override
        public int get(int offset, int len, BooleanArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVBooleanArray#put(int, int, boolean[], int)
         */
        @Override
        public int put(int offset, int length, boolean[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVBooleanArray#shareData(boolean[])
         */
        @Override
        public void shareData(boolean[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVByteArrayImpl extends AbstractSharePVScalarArray implements PVByteArray
    {
        private PVByteArray pvShare;
        
        private SharePVByteArrayImpl(PVRecord pvRecord,PVStructure parent,PVByteArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVByteArray#get(int, int, org.epics.pvdata.pv.ByteArrayData)
         */
        @Override
        public int get(int offset, int len, ByteArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVByteArray#put(int, int, byte[], int)
         */
        @Override
        public int put(int offset, int length, byte[] from, int fromOffset) {
        	int len = 0;
            super.lockShare();
            try {
            	super.pvRecord.beginGroupPut();
                len = pvShare.put(offset, length, from, fromOffset);
                super.pvRecord.endGroupPut();
            } finally {
                super.unlockShare();
            }
            super.postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVByteArray#shareData(byte[])
         */
        @Override
        public void shareData(byte[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVShortArrayImpl extends AbstractSharePVScalarArray implements PVShortArray
    {
        private PVShortArray pvShare;
        
        private SharePVShortArrayImpl(PVRecord pvRecord,PVStructure parent,PVShortArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVShortArray#get(int, int, org.epics.pvdata.pv.ShortArrayData)
         */
        @Override
        public int get(int offset, int len, ShortArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVShortArray#put(int, int, short[], int)
         */
        @Override
        public int put(int offset, int length, short[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVShortArray#shareData(short[])
         */
        @Override
        public void shareData(short[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVIntArrayImpl extends AbstractSharePVScalarArray implements PVIntArray
    {
        private PVIntArray pvShare;
        
        private SharePVIntArrayImpl(PVRecord pvRecord,PVStructure parent,PVIntArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVIntArray#get(int, int, org.epics.pvdata.pv.IntArrayData)
         */
        @Override
        public int get(int offset, int len, IntArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVIntArray#put(int, int, int[], int)
         */
        @Override
        public int put(int offset, int length, int[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;   
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVIntArray#shareData(int[])
         */
        @Override
        public void shareData(int[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVLongArrayImpl extends AbstractSharePVScalarArray implements PVLongArray
    {
        private PVLongArray pvShare;
        
        private SharePVLongArrayImpl(PVRecord pvRecord,PVStructure parent,PVLongArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVLongArray#get(int, int, org.epics.pvdata.pv.LongArrayData)
         */
        @Override
        public int get(int offset, int len, LongArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVLongArray#put(int, int, long[], int)
         */
        @Override
        public int put(int offset, int length, long[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVLongArray#shareData(long[])
         */
        @Override
        public void shareData(long[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVFloatArrayImpl extends AbstractSharePVScalarArray implements PVFloatArray
    {
        private PVFloatArray pvShare;
        
        private SharePVFloatArrayImpl(PVRecord pvRecord,PVStructure parent,PVFloatArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVFloatArray#get(int, int, org.epics.pvdata.pv.FloatArrayData)
         */
        @Override
        public int get(int offset, int len, FloatArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVFloatArray#put(int, int, float[], int)
         */
        @Override
        public int put(int offset, int length, float[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVFloatArray#shareData(float[])
         */
        @Override
        public void shareData(float[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVDoubleArrayImpl extends AbstractSharePVScalarArray implements PVDoubleArray
    {
        private PVDoubleArray pvShare;
        
        private SharePVDoubleArrayImpl(PVRecord pvRecord,PVStructure parent,PVDoubleArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDoubleArray#get(int, int, org.epics.pvdata.pv.DoubleArrayData)
         */
        @Override
        public int get(int offset, int len, DoubleArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDoubleArray#put(int, int, double[], int)
         */
        @Override
        public int put(int offset, int length, double[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDoubleArray#shareData(double[])
         */
        @Override
        public void shareData(double[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}
        }
    }
    
    private static class SharePVStringArrayImpl extends AbstractSharePVScalarArray implements PVStringArray
    {
        private PVStringArray pvShare;
        
        private SharePVStringArrayImpl(PVRecord pvRecord,PVStructure parent,PVStringArray pvShare)
        {
            super(pvRecord,parent,pvShare);
            this.pvShare = pvShare;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVStringArray#get(int, int, org.epics.pvdata.pv.StringArrayData)
         */
        @Override
        public int get(int offset, int len, StringArrayData data) {
            super.lockShare();
            try {
                return pvShare.get(offset, len, data);
            } finally {
                super.unlockShare();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVStringArray#put(int, int, String[], int)
         */
        @Override
        public int put(int offset, int length, String[] from, int fromOffset) {
        	int len = 0;
        	super.lockShare();
        	try {
        		super.pvRecord.beginGroupPut();
        		len = pvShare.put(offset, length, from, fromOffset);
        		super.pvRecord.endGroupPut();
        	} finally {
        		super.unlockShare();
        	}
        	super.postPut();
        	return len;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVStringArray#shareData(java.lang.String[])
         */
        @Override
        public void shareData(String[] from) {
        	super.lockShare();
        	try {
        		pvShare.shareData(from);
        	} finally {
        		super.unlockShare();
        	}

        }
    }   
}
