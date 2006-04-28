/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Factory to obtain the implementation of <i>Convert</i>
 * @author mrk
 *
 */
public final class ConvertFactory {
    /**
     * Implements <i>Convert</i>.
     * The implementation ensures that a single instance is created.
     * @return the implementation of <i>Convert</i>
     */
    public static Convert getConvert()
    {
    	return ImplementConvert.getConvert();
    }

    private static final class ImplementConvert implements Convert{
        private static ImplementConvert singleImplementation = null;
    
    
        private static synchronized ImplementConvert getConvert() {
                if (singleImplementation==null) {
                    singleImplementation = new ImplementConvert();
                }
                return singleImplementation;
        }

        // Guarantee that ImplementConvert can only be created via getConvert
        private ImplementConvert() {}
       
        public void fromByte(PVData pv, byte from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int fromByteArray(PVData pv, int offset, int len,
            byte[] from, int fromOffset) {
            return ConvertByteArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public void fromDouble(PVData pv, double from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int fromDoubleArray(PVData pv, int offset, int len,
            double[] from, int fromOffset) {
            return ConvertDoubleArrayFrom(pv,offset,len,from,fromOffset);
        }

        public int fromStringArray(PVData pv, int offset, int len,
            String[] from, int fromOffset) {
            return ConvertStringArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public void fromFloat(PVData pv, float from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int fromFloatArray(PVData pv, int offset, int len,
            float[] from, int fromOffset) {
            return ConvertFloatArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public void fromInt(PVData pv, int from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int fromIntArray(PVData pv, int offset, int len,
            int[] from, int fromOffset) {
            return ConvertIntArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public void fromLong(PVData pv, long from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int fromLongArray(PVData pv, int offset, int len,
            long[] from, int fromOffset) {
            return ConvertLongArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public void fromShort(PVData pv, short from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; value.put((byte)from); return;}
                case pvShort :
                    {PVShort value = (PVShort)pv; value.put((short)from); return;}
                case pvInt :
                    {PVInt value = (PVInt)pv; value.put((int)from); return;}
                case pvLong :
                    {PVLong value = (PVLong)pv; value.put((long)from); return;}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; value.put((float)from); return;}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; value.put((double)from); return;}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }

        public void fromString(PVData pv, String from) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvBoolean: {
                        PVBoolean value = (PVBoolean)pv;
                        value.put(Boolean.parseBoolean(from));
                        return;
                    }
                case pvByte : {
                        PVByte value = (PVByte)pv;
                        value.put(Byte.decode(from));
                        return;
                    }
                case pvShort : {
                        PVShort value = (PVShort)pv;
                        value.put(Short.decode(from));
                        return;
                    }
                case pvInt : {
                        PVInt value = (PVInt)pv;
                        value.put(Integer.decode(from));
                        return;
                    }
                case pvLong : {
                        PVLong value = (PVLong)pv;
                        value.put(Long.decode(from));
                        return;
                    }
                case pvFloat : {
                        PVFloat value = (PVFloat)pv;
                        value.put(Float.valueOf(from));
                        return;
                    }
                case pvDouble : {
                        PVDouble value = (PVDouble)pv;
                        value.put(Double.valueOf(from));
                        return;
                    }
                case pvString: {
                        PVString value = (PVString)pv;
                        value.put(from);
                        return;
                    }
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
            
        }
    
        public int fromShortArray(PVData pv, int offset, int len,
            short[] from, int fromOffset) {
            return ConvertShortArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        public String getString(PVData pv) {
            return ConvertToString(pv,0);
        }
    
        public String getString(PVData pv,int indentLevel) {
            return ConvertToString(pv,indentLevel);
        }
    
        public byte toByte(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (byte)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (byte)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (byte)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (byte)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (byte)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (byte)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toByteArray(PVData pv, int offset, int len,
            byte[] to, int toOffset) {
        	return ConvertByteArrayTo(pv,offset,len,to,toOffset);
        }
    
        public double toDouble(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (double)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (double)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (double)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (double)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (double)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (double)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toDoubleArray(PVData pv, int offset, int len,
            double[] to, int toOffset) {
        	return ConvertDoubleArrayTo(pv,offset,len,to,toOffset);
        }
    
        public float toFloat(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (float)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (float)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (float)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (float)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (float)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (float)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toFloatArray(PVData pv, int offset, int len,
            float[] to, int toOffset) {
        	return ConvertFloatArrayTo(pv,offset,len,to,toOffset);
        }
    
        public int toInt(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (int)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (int)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (int)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (int)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (int)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (int)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toIntArray(PVData pv, int offset, int len,
            int[] to, int toOffset) {
        	return ConvertIntArrayTo(pv,offset,len,to,toOffset);
        }
    
        public long toLong(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (long)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (long)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (long)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (long)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (long)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (long)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toLongArray(PVData pv, int offset, int len,
            long[] to, int toOffset) {
        	return ConvertLongArrayTo(pv,offset,len,to,toOffset);
        }
    
        public short toShort(PVData pv) {
            Field field = pv.getField();
            Type type = field.getType();
            switch(type) {
                case pvByte :
                    {PVByte value = (PVByte)pv; return (short)value.get();}
                case pvShort :
                    {PVShort value = (PVShort)pv; return (short)value.get();}
                case pvInt :
                    {PVInt value = (PVInt)pv; return (short)value.get();}
                case pvLong :
                    {PVLong value = (PVLong)pv; return (short)value.get();}
                case pvFloat :
                    {PVFloat value = (PVFloat)pv; return (short)value.get();}
                case pvDouble :
                    {PVDouble value = (PVDouble)pv; return (short)value.get();}
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        public int toShortArray(PVData pv, int offset, int len,
            short[] to, int toOffset) {
        	return ConvertShortArrayTo(pv,offset,len,to,toOffset);
        }
    
    
        private int ConvertByteArrayFrom(PVData pv, int offset, int len,
        byte[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        data[0] = (short)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        data[0] = (int)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        data[0] = (long)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        data[0] = (float)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        data[0] = (double)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertByteArrayTo(PVData pv, int offset, int len,
        byte[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertShortArrayFrom(PVData pv, int offset, int len,
        short[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        data[0] = (byte)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        data[0] = (int)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        data[0] = (long)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        data[0] = (float)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        data[0] = (double)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertShortArrayTo(PVData pv, int offset, int len,
        short[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertIntArrayFrom(PVData pv, int offset, int len,
            int[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        data[0] = (byte)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        data[0] = (short)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        data[0] = (long)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        data[0] = (float)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        data[0] = (double)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertIntArrayTo(PVData pv, int offset, int len,
            int[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertLongArrayFrom(PVData pv, int offset, int len,
        long[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        data[0] = (byte)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        data[0] = (short)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        data[0] = (int)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        data[0] = (float)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        data[0] = (double)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertLongArrayTo(PVData pv, int offset, int len,
        long[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertFloatArrayFrom(PVData pv, int offset, int len,
        float[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        data[0] = (byte)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        data[0] = (short)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        data[0] = (int)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        data[0] = (long)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        data[0] = (double)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertFloatArrayTo(PVData pv, int offset, int len,
        float[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    double[] data = new double[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private int ConvertDoubleArrayFrom(PVData pv, int offset, int len,
        double[]from, int fromOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        data[0] = (byte)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        data[0] = (short)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        data[0] = (int)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        data[0] = (long)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        data[0] = (float)from[fromOffset];
                        if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                        --len; ++ntransfered; ++offset; ++fromOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    while(len>0) {
                        int num = pvdata.put(offset,len,from,fromOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; fromOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
        
        private int ConvertStringArrayFrom(PVData pv, int offset, int len,
                String[]from, int fromOffset)
                {
                    Field field = pv.getField();
                    Type type = field.getType();
                    if(type!=Type.pvArray) throw new IllegalArgumentException(
                        "Illegal PVType. Must be array but it is " + type.toString());
                    Array array = (Array)field;
                    Type elemType = array.getElementType();
                    int ntransfered = 0;
                    switch(elemType) {
                        case pvBoolean: {
                            PVBooleanArray pvdata = (PVBooleanArray)pv;
                            boolean[] data = new boolean[1];
                            while(len>0) {
                                data[0] = Boolean.parseBoolean(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        }
                        case pvByte : {
                            PVByteArray pvdata = (PVByteArray)pv;
                            byte[] data = new byte[1];
                            while(len>0) {
                                data[0] = Byte.decode(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvShort : {
                            PVShortArray pvdata = (PVShortArray)pv;
                            short[] data = new short[1];
                            while(len>0) {
                                data[0] = Short.decode(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvInt : {
                            PVIntArray pvdata = (PVIntArray)pv;
                            int[] data = new int[1];
                            while(len>0) {
                                data[0] = Integer.decode(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvLong : {
                            PVLongArray pvdata = (PVLongArray)pv;
                            long[] data = new long[1];
                            while(len>0) {
                                data[0] = Long.decode(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvFloat : {
                            PVFloatArray pvdata = (PVFloatArray)pv;
                            float[] data = new float[1];
                            while(len>0) {
                                data[0] = Float.valueOf(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvDouble : {
                            PVDoubleArray pvdata = (PVDoubleArray)pv;
                            double[]data = new double[1];
                            while(len>0) {
                                data[0] = Double.valueOf(from[fromOffset]);
                                if(pvdata.put(offset,1,data,0)==0) return ntransfered;
                                --len; ++ntransfered; ++offset; ++fromOffset;
                            }
                            return ntransfered;
                        } 
                        case pvString:
                            PVStringArray pvdata = (PVStringArray)pv;
                            while(len>0) {
                                int num = pvdata.put(offset,len,from,fromOffset);
                                if(num==0) break;
                                len -= num;
                                offset += num; fromOffset += num; ntransfered += num;
                            }
                            return ntransfered;
                        default:
                            throw new IllegalArgumentException(
                              "Illegal PVType. Must be scalar but it is "
                              + type.toString()
                            );
                    }
                }
    
        private int ConvertDoubleArrayTo(PVData pv, int offset, int len,
        double[]to, int toOffset)
        {
            Field field = pv.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) throw new IllegalArgumentException(
                "Illegal PVType. Must be array but it is " + type.toString());
            Array array = (Array)field;
            Type elemType = array.getElementType();
            int ntransfered = 0;
            switch(elemType) {
                case pvByte : {
                    PVByteArray pvdata = (PVByteArray)pv;
                    byte[] data = new byte[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    short[] data = new short[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    int[] data = new int[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    long[] data = new long[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    float[] data = new float[1];
                    while(len>0) {
                        if(pvdata.get(offset,1,data,0)==0) return ntransfered;
                        to[toOffset] = (byte)data[0];
                        --len; ++ntransfered; ++offset; ++toOffset;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    while(len>0) {
                        int num = pvdata.get(offset,len,to,toOffset);
                        if(num==0) break;
                        len -= num;
                        offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be numeric but it is "
                      + type.toString()
                    );
            }
        }
    
        private static void newLine(StringBuilder builder, int indentLevel) {
            builder.append("\n");
            for (int i=0; i <indentLevel; i++) builder.append(indentString);
        }
        private static String indentString = "    ";

        private String ConvertToString(PVData pv,int indentLevel) {
            Field field = pv.getField();
            switch(field.getType()) {
            case pvUnknown:{
                    return "unknown type";
                }
            case pvBoolean: {
                    PVBoolean data = (PVBoolean)pv;
                    boolean value = data.get();
                    if(value) {
                        return "true";
                    } else {
                        return "false";
                    }
                }
            case pvByte: {
                    PVByte data = (PVByte)pv;
                    return String.format("%d",data.get());
                }
            case pvShort: {
                    PVShort data = (PVShort)pv;
                    return String.format("%d",data.get());
                }
            case pvInt: {
                    PVInt data = (PVInt)pv;
                    return String.format("%d",data.get());
                }
            case pvLong: {
                    PVLong data = (PVLong)pv;
                    return String.format("%d",data.get());
                }
            case pvFloat: {
                    PVFloat data = (PVFloat)pv;
                    return String.format("%f",data.get());
                }
            case pvDouble: {
                    PVDouble data = (PVDouble)pv;
                    return String.format("%f",data.get());
                }
            case pvString: {
                    PVString data = (PVString)pv;
                    return data.get();
                }
            case pvEnum: return convertEnum(pv);
            case pvStructure: return convertStructure(pv,indentLevel);
            case pvArray: return convertArray(pv,indentLevel);
            default:
                return "unknown PVType";
            }
        }
    
        private String convertEnum(PVData pv) {
            PVEnum data = (PVEnum)pv;
            StringBuilder builder = new StringBuilder();
            int index = data.getIndex();
            String[] choices = data.getChoices();
            builder.append(String.format(
                "{index = %d choices = {",index));
            if(choices!=null) for(String choice : choices) {
                 builder.append("\"");
                 if(choice!=null) builder.append(choice);
                 builder.append("\" ");
            }
            builder.append( "}}");
            return builder.toString();
        }
    
        private String convertStructure(PVData pv,int indentLevel) {
            PVStructure data = (PVStructure)pv;
            Structure structure = (Structure)pv.getField();
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format("structure %s{",
                structure.getStructureName()));
            PVData[] fieldsData = data.getFieldPVDatas();
            for(PVData fieldData : fieldsData) {
                Field fieldnow = fieldData.getField();
                newLine(builder,indentLevel+1);
                builder.append(String.format("%s = ", fieldnow.getName()));
                builder.append(ConvertToString(fieldData,indentLevel+1));
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
    
        private String convertArray(PVData pv,int indentLevel) {
            Array array = (Array)pv.getField();
            Type type = array.getElementType();
            StringBuilder builder = new StringBuilder();
            switch(type) {
            case pvUnknown:{
                    builder.append( "unknown type");
                    break;
                }
            case pvBoolean: {
                    PVBooleanArray data = (PVBooleanArray)pv;
                    boolean[] value = new boolean[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             if(value[0]) {
                                 builder.append("true ");
                             } else {
                                 builder.append("false ");
                             }
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvByte: {
                    PVByteArray data = (PVByteArray)pv;
                    byte[] value = new byte[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%d ",value[0]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvShort: {
                    PVShortArray data = (PVShortArray)pv;
                    short[] value = new short[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%d ",value[0]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvInt: {
                    PVIntArray data = (PVIntArray)pv;
                    int[] value = new int[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%d ",value[0]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvLong: {
                    PVLongArray data = (PVLongArray)pv;
                    long[] value = new long[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%d ",value[0]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvFloat: {
                    PVFloatArray data = (PVFloatArray)pv;
                    float[] value = new float[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%f ",value[0]));
                        } else {
                             builder.append(indentString + "???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvDouble: {
                    PVDoubleArray data = (PVDoubleArray)pv;
                    double[] value = new double[1];
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        int num = data.get(i,1,value,0);
                        if(num==1) {
                             builder.append(String.format("%f ",value[0]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvString: {
                    PVStringArray data = (PVStringArray)pv;
                    String[] value = new String[1]; 
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        value[0] = null; data.get(i,1,value,0);
                        if(value[0]!=null) {
                            builder.append("\"");
                            builder.append(value[0]);
                            builder.append("\" ");
                        } else {
                             builder.append("null ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvEnum: {
                    PVEnumArray data = (PVEnumArray)pv;
                    PVEnum[] value = new PVEnum[1]; 
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        newLine(builder,indentLevel);
                        builder.append(indentString);
                        value[0] = null; data.get(i,1,value,0);
                        if(value[0]!=null) {
                            builder.append(convertEnum(value[0]));
                        } else {
                             builder.append("{} ");
                        }
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                    break;
                }
            case pvStructure: {
                    PVStructureArray data = (PVStructureArray)pv;
                    PVStructure[] value = new PVStructure[1]; 
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        value[0] = null; data.get(i,1,value,0);
                        if(value[0]!=null) {
                            builder.append(convertStructure(value[0],indentLevel + 1));
                        } else {
                             newLine(builder,indentLevel + 1);
                             builder.append("null ");
                        }
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                    break;
                }
            case pvArray: {
                    PVArrayArray data = (PVArrayArray)pv;
                    PVArray[] value = new PVArray[1]; 
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < data.getLength(); i++) {
                        newLine(builder,indentLevel);
                        builder.append(indentString);
                        value[0] = null; data.get(i,1,value,0);
                        if(value[0]!=null) {
                            builder.append(convertArray(value[0],indentLevel + 1));
                        } else {
                             builder.append("{}");
                        }
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                    break;
                }
            default:
                builder.append(" array element is unknown PVType");
            }
            return builder.toString();
        }
    }
}
