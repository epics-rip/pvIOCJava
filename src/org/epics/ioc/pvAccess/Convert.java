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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
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
                  "Illegal PVType. Must be numeric bit it is "
                  + type.toString()
                );
        }
    }
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

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        super.finalize();
    }

    public void fromByte(PVData pv, byte from) {
        ConvertByte.from(pv,from);
    }

    public int fromByteArray(PVData pv, int offset, int len, byte[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void fromDouble(PVData pv, double from) {
        ConvertDouble.from(pv,from);
    }

    public int fromDoubleArray(PVData pv, int offset, int len, double[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void fromFloat(PVData pv, float from) {
        ConvertFloat.from(pv,from);
    }

    public int fromFloatArray(PVData pv, int offset, int len, float[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void fromInt(PVData pv, int from) {
        ConvertInt.from(pv,from);
    }

    public int fromIntArray(PVData pv, int offset, int len, int[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void fromLong(PVData pv, long from) {
        ConvertLong.from(pv,from);
    }

    public int fromLongArray(PVData pv, int offset, int len, long[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void fromShort(PVData pv, short from) {
        ConvertShort.from(pv,from);
    }

    public int fromShortArray(PVData pv, int offset, int len, short[] from) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getString(PVData pv) {
        // TODO Auto-generated method stub
        return null;
    }

    public byte toByte(PVData pv) {
        return ConvertByte.to(pv);
    }

    public int toByteArray(PVData pv, int offset, int len, byte[] to) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double toDouble(PVData pv) {
        return ConvertDouble.to(pv);
    }

    public int toDoubleArray(PVData pv, int offset, int len, double[] to) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float toFloat(PVData pv) {
        return ConvertFloat.to(pv);
    }

    public int toFloatArray(PVData pv, int offset, int len, float[] to) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int toInt(PVData pv) {
        return ConvertInt.to(pv);
    }

    public int toIntArray(PVData pv, int offset, int len, int[] to) {
        // TODO Auto-generated method stub
        return 0;
    }

    public long toLong(PVData pv) {
        return ConvertLong.to(pv);
    }

    public int toLongArray(PVData pv, int offset, int len, long[] to) {
        // TODO Auto-generated method stub
        return 0;
    }

    public short toShort(PVData pv) {
        return ConvertShort.to(pv);
    }

    public int toShortArray(PVData pv, int offset, int len, short[] to) {
        // TODO Auto-generated method stub
        return 0;
    }
}
