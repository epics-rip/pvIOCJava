package org.epics.ioc.pvAccess;

class ConvertByte {
     static byte to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, byte data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertShort {
     static short to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, short data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertInt {
     static int to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, int data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertLong {
     static long to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, long data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertFloat {
     static float to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, float data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertDouble {
     static double to(PVData pv) {
        Field field = pv.getField();
        PVType type = field.getPVType();
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
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static void from(PVData pv, double data) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        switch(type) {
            case pvByte :
                {PVByte value = (PVByte)pv; value.put((byte)data); return;}
            case pvShort :
                {PVShort value = (PVShort)pv; value.put((short)data); return;}
            case pvInt :
                {PVInt value = (PVInt)pv; value.put((int)data); return;}
            case pvLong :
                {PVLong value = (PVLong)pv; value.put((long)data); return;}
            case pvFloat :
                {PVFloat value = (PVFloat)pv; value.put((float)data); return;}
            case pvDouble :
                {PVDouble value = (PVDouble)pv; value.put((double)data); return;}
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertByteArray {
     static int from(PVData pv, int offset, int len, byte[]from, int fromOffset) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        if(type!=PVType.pvArray) throw new Error(
            "Illegal PVType. Must be array but it is " + type.toString());
        Array array = (Array)field;
        PVType elemType = array.getElementType();
        int ntransfered = 0;
        switch(elemType) {
            case pvByte : {
                PVByteArray fromArray = (PVByteArray)pv;
                byte[] data = new byte[1];
                while(len>0) {
                    data[0] = (byte)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            case pvShort : {
                PVShortArray fromArray = (PVShortArray)pv;
                short[] data = new short[1];
                while(len>0) {
                    data[0] = (short)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            case pvInt : {
                PVIntArray fromArray = (PVIntArray)pv;
                int[] data = new int[1];
                while(len>0) {
                    data[0] = (int)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            case pvLong : {
                PVLongArray fromArray = (PVLongArray)pv;
                long[] data = new long[1];
                while(len>0) {
                    data[0] = (long)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            case pvFloat : {
                PVFloatArray fromArray = (PVFloatArray)pv;
                float[] data = new float[1];
                while(len>0) {
                    data[0] = (float)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            case pvDouble : {
                PVDoubleArray fromArray = (PVDoubleArray)pv;
                double[] data = new double[1];
                while(len>0) {
                    data[0] = (double)from[fromOffset];
                    if(fromArray.put(offset,1,data,0)==0) return ntransfered;
                    --len; ++ntransfered; ++offset; ++fromOffset;
                }
                return ntransfered;
            } 
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }

    static int to(PVData pv, int offset, int len, byte[]to, int toOffset) {
        Field field = pv.getField();
        PVType type = field.getPVType();
        if(type!=PVType.pvArray) throw new Error(
            "Illegal PVType. Must be array but it is " + type.toString());
        Array array = (Array)field;
        PVType elemType = array.getElementType();
        int ntransfered = 0;
        switch(elemType) {
            case pvByte : {
                PVByteArray toArray = (PVByteArray)pv;
                byte[] data = new byte[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            case pvShort : {
                PVShortArray toArray = (PVShortArray)pv;
                short[] data = new short[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            case pvInt : {
                PVIntArray toArray = (PVIntArray)pv;
                int[] data = new int[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            case pvLong : {
                PVLongArray toArray = (PVLongArray)pv;
                long[] data = new long[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            case pvFloat : {
                PVFloatArray toArray = (PVFloatArray)pv;
                float[] data = new float[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            case pvDouble : {
                PVDoubleArray toArray = (PVDoubleArray)pv;
                double[] data = new double[1];
                while(len>0) {
                    if(toArray.get(offset,1,data,0)==0) return ntransfered;
                    to[toOffset] = (byte)data[0];
                    --len; ++ntransfered; ++offset; ++toOffset;
                }
                return ntransfered;
            } 
            default:
                throw new Error(
                  "Illegal PVType. Must be numeric but it is "
                  + type.toString()
                );
        }
    }
}

class ConvertShortArray {
     static int to(PVData pv, int offset, int len, short[]to, int toOffset) {
     return 0;}

    static int from(PVData pv, int offset, int len, short[]from, int fromOffset) {
    return 0;}
}

class ConvertIntArray {
     static int to(PVData pv, int offset, int len, int[]to, int toOffset) {
     return 0;}

    static int from(PVData pv, int offset, int len, int[]from, int fromOffset) {
    return 0;}
}

class ConvertLongArray {
     static int to(PVData pv, int offset, int len, long[]to, int toOffset) {
     return 0;}

    static int from(PVData pv, int offset, int len, long[]from, int fromOffset) {
    return 0;}
}

class ConvertFloatArray {
     static int to(PVData pv, int offset, int len, float[]to, int toOffset) {
     return 0;}

    static int from(PVData pv, int offset, int len, float[]from, int fromOffset) {
    return 0;}
}

class ConvertDoubleArray {
     static int to(PVData pv, int offset, int len, double[]to, int toOffset) {
     return 0;}

    static int from(PVData pv, int offset, int len, double[]from, int fromOffset) {
    return 0;}
}

public class Convert implements PVConvert{
    private static Convert singleImplementation = null;

    private Convert() {
    }

    public static Convert getConvert() {
            if (singleImplementation==null) {
                singleImplementation = new Convert();
            }
            return singleImplementation;
    }
   
    public void fromByte(PVData pv, byte from) {
        ConvertByte.from(pv,from);
    }

    public int fromByteArray(PVData pv, int offset, int len,
        byte[] from, int fromOffset) {
        return ConvertByteArray.from(pv,offset,len,from,fromOffset);
    }

    public void fromDouble(PVData pv, double from) {
        ConvertDouble.from(pv,from);
    }

    public int fromDoubleArray(PVData pv, int offset, int len,
        double[] from, int fromOffset) {
        return ConvertDoubleArray.from(pv,offset,len,from,fromOffset);
    }

    public void fromFloat(PVData pv, float from) {
        ConvertFloat.from(pv,from);
    }

    public int fromFloatArray(PVData pv, int offset, int len,
        float[] from, int fromOffset) {
        return ConvertFloatArray.from(pv,offset,len,from,fromOffset);
    }

    public void fromInt(PVData pv, int from) {
        ConvertInt.from(pv,from);
    }

    public int fromIntArray(PVData pv, int offset, int len,
        int[] from, int fromOffset) {
        return ConvertIntArray.from(pv,offset,len,from,fromOffset);
    }

    public void fromLong(PVData pv, long from) {
        ConvertLong.from(pv,from);
    }

    public int fromLongArray(PVData pv, int offset, int len,
        long[] from, int fromOffset) {
        return ConvertLongArray.from(pv,offset,len,from,fromOffset);
    }

    public void fromShort(PVData pv, short from) {
        ConvertShort.from(pv,from);
    }

    public int fromShortArray(PVData pv, int offset, int len,
        short[] from, int fromOffset) {
        return ConvertShortArray.from(pv,offset,len,from,fromOffset);
    }

    public String getString(PVData pv) {
        // TODO Auto-generated method stub
        return null;
    }

    public byte toByte(PVData pv) {
        return ConvertByte.to(pv);
    }

    public int toByteArray(PVData pv, int offset, int len,
        byte[] to, int toOffset) {
    	return ConvertByteArray.to(pv,offset,len,to,toOffset);
    }

    public double toDouble(PVData pv) {
        return ConvertDouble.to(pv);
    }

    public int toDoubleArray(PVData pv, int offset, int len,
        double[] to, int toOffset) {
    	return ConvertDoubleArray.to(pv,offset,len,to,toOffset);
    }

    public float toFloat(PVData pv) {
        return ConvertFloat.to(pv);
    }

    public int toFloatArray(PVData pv, int offset, int len,
        float[] to, int toOffset) {
    	return ConvertFloatArray.to(pv,offset,len,to,toOffset);
    }

    public int toInt(PVData pv) {
        return ConvertInt.to(pv);
    }

    public int toIntArray(PVData pv, int offset, int len,
        int[] to, int toOffset) {
    	return ConvertIntArray.to(pv,offset,len,to,toOffset);
    }

    public long toLong(PVData pv) {
        return ConvertLong.to(pv);
    }

    public int toLongArray(PVData pv, int offset, int len,
        long[] to, int toOffset) {
    	return ConvertLongArray.to(pv,offset,len,to,toOffset);
    }

    public short toShort(PVData pv) {
        return ConvertShort.to(pv);
    }

    public int toShortArray(PVData pv, int offset, int len,
        short[] to, int toOffset) {
    	return ConvertShortArray.to(pv,offset,len,to,toOffset);
    }
}
