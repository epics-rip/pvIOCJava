/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * Factory to obtain the implementation of <i>Convert</i>
 * @author mrktestByteArrayCopy
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
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#getString(org.epics.ioc.pvAccess.PVData)
         */
        public String getString(PVData pv) {
            return ConvertToString(pv,0);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#getString(org.epics.ioc.pvAccess.PVData, int)
         */
        public String getString(PVData pv,int indentLevel) {
            return ConvertToString(pv,indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromString(org.epics.ioc.pvAccess.PVData, java.lang.String)
         */
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
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromStringArray(org.epics.ioc.pvAccess.PVArray, int, int, java.lang.String[], int)
         */
        public int fromStringArray(PVArray pv, int offset, int len,
            String[] from, int fromOffset)
        {
            return ConvertFromStringArray(pv,offset,len,from,fromOffset);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toStringArray(org.epics.ioc.pvAccess.PVArray, int, int, java.lang.String[], int)
         */
        public int toStringArray(PVArray pv, int offset, int len, String[] to, int toOffset) {
            return ConvertToStringArray(pv,offset,len,to,toOffset);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#isCopyScalarCompatible(org.epics.ioc.pvAccess.Field, org.epics.ioc.pvAccess.Field)
         */
        public boolean isCopyScalarCompatible(Field fromField, Field toField) {
            Type fromType = fromField.getType();
            Type toType = toField.getType();
            switch(fromType) {
            case pvBoolean:
                if(toType==Type.pvBoolean || toType==Type.pvString) return true;
                break;
            case pvString:
                if(toType.isScalar()) return true;
                break;
            default:
                if(fromType.isNumeric() && toType.isNumeric()) return true;
                if(fromType.isScalar() && toType==Type.pvString) return true;
                break;
            }
            return false;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#copyScalar(org.epics.ioc.pvAccess.PVData, org.epics.ioc.pvAccess.PVData)
         */
        public void copyScalar(PVData from, PVData to) {
            Field field = from.getField();
            Type type = field.getType();
            switch(type) {
            case pvBoolean: {
                    Type toType = to.getField().getType();
                    if(toType!=Type.pvBoolean) {
                        if(toType!=Type.pvString) break;
                    }
                    PVBoolean data = (PVBoolean)from;
                    boolean value = data.get();
                    if(toType==Type.pvString) {
                        PVString dataTo = (PVString)to;
                        dataTo.put(((Boolean)value).toString());
                    } else {
                        PVBoolean dataTo = (PVBoolean)to;
                        dataTo.put(value);
                    }
                    return;
                }
            case pvByte : {
                    PVByte data = (PVByte)from;
                    byte value = data.get();
                    fromByte(to,value);
                    return;
                }
            case pvShort : {
                    PVShort data = (PVShort)from;
                    short value = data.get();
                    fromShort(to,value);
                    return;
                } 
            case pvInt :{
                    PVInt data = (PVInt)from;
                    int value = data.get();
                    fromInt(to,value);
                    return;
                }    
            case pvLong : {
                    PVLong data = (PVLong)from;
                    long value = data.get();
                    fromLong(to,value);
                    return;
                }  
            case pvFloat : {
                    PVFloat data = (PVFloat)from;
                    float value = data.get();
                    fromFloat(to,value);
                    return;
                }     
            case pvDouble : {
                    PVDouble data = (PVDouble)from;
                    double value = data.get();
                    fromDouble(to,value);
                    return;
                }  
            case pvString: {
                    PVString data = (PVString)from;
                    String value = data.get();
                    fromString(to,value);
                    return;
                }
            default:
            }
            throw new IllegalArgumentException(
                    "Convert.copyScalar arguments are not compatible"
                  );
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#isCopyArrayCompatible(org.epics.ioc.pvAccess.Array, org.epics.ioc.pvAccess.Array)
         */
        public boolean isCopyArrayCompatible(Array fromArray, Array toArray) {
            Type fromElementType = fromArray.getElementType();
            Type toElementType = toArray.getElementType();
            if(toElementType.isNumeric() && fromElementType.isNumeric()) return true;
            if(toElementType==Type.pvBoolean && fromElementType==Type.pvBoolean) return true;
            if(toElementType==Type.pvString) return true;
            if(fromElementType==Type.pvString && toElementType.isScalar()) return true;
            if(fromElementType==Type.pvArray && toElementType==Type.pvArray) return true;
            if(fromElementType==Type.pvStructure && toElementType==Type.pvStructure) return true;
            return false;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#copyArray(org.epics.ioc.pvAccess.PVArray, int, org.epics.ioc.pvAccess.PVArray, int, int)
         */
        public int copyArray(PVArray from, int offset, PVArray to, int toOffset, int len)
        {
            Type fromElementType = ((Array)from.getField()).getElementType();
            Type toElementType = ((Array)to.getField()).getElementType();
            if(toElementType.isNumeric() && fromElementType.isNumeric())
                return CopyNumericArray(from,offset,to,toOffset,len);
            if(toElementType==Type.pvBoolean && fromElementType==Type.pvBoolean) {
                PVBooleanArray pvfrom = (PVBooleanArray)from;
                PVBooleanArray pvto = (PVBooleanArray)to;
                BooleanArrayData data = new BooleanArrayData();
                int ncopy = 0;
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = pvto.put(toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                return ncopy;
            }
            if(toElementType==Type.pvString) {
                PVStringArray pvto = (PVStringArray)to;
                int ncopy = from.getLength();
                if(ncopy>len) ncopy = len;
                int num = ncopy;
                String[] toData = new String[1];
                while(num>0) {
                    toStringArray(from,offset,1,toData,0);
                    pvto.put(toOffset,1,toData,0);
                    num--; offset++; toOffset++;
                }
                return ncopy;
            }
            if(fromElementType==Type.pvString && toElementType.isScalar()) {
                PVStringArray pvfrom = (PVStringArray)from;
                StringArrayData data = new StringArrayData();
                int ncopy = 0;
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromStringArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                return ncopy;
            }
            if(fromElementType==Type.pvArray && toElementType==Type.pvArray) {
                PVArrayArray pvfrom = (PVArrayArray)from;
                PVArrayArray pvto = (PVArrayArray)to;
                ArrayArrayData data = new ArrayArrayData();
                int ncopy = 0;
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = pvto.put(toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                return ncopy;
            }
            if(fromElementType==Type.pvStructure && toElementType==Type.pvStructure) {
                PVStructureArray pvfrom = (PVStructureArray)from;
                PVStructureArray pvto = (PVStructureArray)to;
                StructureArrayData data = new StructureArrayData();
                int ncopy = 0;
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = pvto.put(toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                return ncopy;
            }
            return 0;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#isCopyStructureCompatible(org.epics.ioc.pvAccess.Structure, org.epics.ioc.pvAccess.Structure)
         */
        public boolean isCopyStructureCompatible(Structure from, Structure to) {
            if(from==to) return true;
            return false;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#copyStructure(org.epics.ioc.pvAccess.PVStructure, org.epics.ioc.pvAccess.PVStructure)
         */
        public void copyStructure(PVStructure from, PVStructure to) {
            Structure fromStructure = (Structure)from.getField();
            Structure toStructure = (Structure)to.getField();
            if(fromStructure!=toStructure) {
                throw new IllegalArgumentException(
                    "Convert.copyStructure from and to are not the same type of structure");
            }
            PVData[] fromDatas = from.getFieldPVDatas();
            PVData[] toDatas = to.getFieldPVDatas();
            for(int i=0; i < fromDatas.length; i++) {
                PVData fromData = fromDatas[i];
                PVData toData = toDatas[i];
                Type type = fromData.getField().getType();
                if(type.isScalar()) {
                    copyScalar(fromData,toData);
                } else if(type==Type.pvUnknown) {
                    // do nothing
                } else if(type==Type.pvArray) {
                    Array fromElementArray = (Array)fromData.getField();
                    Array toElementArray = (Array)toData.getField();
                    if(isCopyArrayCompatible(fromElementArray,toElementArray)) {
                        int len = ((PVArray)fromData).getLength();
                        copyArray((PVArray)fromData,0,(PVArray)toData,0,len);
                    }
                } else if(type==Type.pvEnum) {
                    PVEnum fromEnum = (PVEnum)fromData;
                    PVEnum toEnum = (PVEnum)toData;
                    String[] choices = fromEnum.getChoices();
                    int index = fromEnum.getIndex();
                    toEnum.setChoices(choices);
                    toEnum.setIndex(index);
                } else if(type==Type.pvStructure) {
                    Structure fromElementStructure = (Structure)fromData.getField();
                    Structure toElementStructure = (Structure)toData.getField();
                    if(isCopyStructureCompatible(fromElementStructure,toElementStructure))
                        copyStructure((PVStructure)fromData,(PVStructure)toData);
                }
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromByte(org.epics.ioc.pvAccess.PVData, byte)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromByteArray(org.epics.ioc.pvAccess.PVData, int, int, byte[], int)
         */
        public int fromByteArray(PVData pv, int offset, int len,
            byte[] from, int fromOffset) {
            return ConvertByteArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromDouble(org.epics.ioc.pvAccess.PVData, double)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromDoubleArray(org.epics.ioc.pvAccess.PVData, int, int, double[], int)
         */
        public int fromDoubleArray(PVData pv, int offset, int len,
            double[] from, int fromOffset) {
            return ConvertDoubleArrayFrom(pv,offset,len,from,fromOffset);
        }

    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromFloat(org.epics.ioc.pvAccess.PVData, float)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromFloatArray(org.epics.ioc.pvAccess.PVData, int, int, float[], int)
         */
        public int fromFloatArray(PVData pv, int offset, int len,
            float[] from, int fromOffset) {
            return ConvertFloatArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromInt(org.epics.ioc.pvAccess.PVData, int)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromIntArray(org.epics.ioc.pvAccess.PVData, int, int, int[], int)
         */
        public int fromIntArray(PVData pv, int offset, int len,
            int[] from, int fromOffset) {
            return ConvertIntArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromLong(org.epics.ioc.pvAccess.PVData, long)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromLongArray(org.epics.ioc.pvAccess.PVData, int, int, long[], int)
         */
        public int fromLongArray(PVData pv, int offset, int len,
            long[] from, int fromOffset) {
            return ConvertLongArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromShort(org.epics.ioc.pvAccess.PVData, short)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#fromShortArray(org.epics.ioc.pvAccess.PVData, int, int, short[], int)
         */
        public int fromShortArray(PVData pv, int offset, int len,
            short[] from, int fromOffset) {
            return ConvertShortArrayFrom(pv,offset,len,from,fromOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toByte(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toByteArray(org.epics.ioc.pvAccess.PVData, int, int, byte[], int)
         */
        public int toByteArray(PVData pv, int offset, int len,
            byte[] to, int toOffset) {
        	return ConvertByteArrayTo(pv,offset,len,to,toOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toDouble(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toDoubleArray(org.epics.ioc.pvAccess.PVData, int, int, double[], int)
         */
        public int toDoubleArray(PVData pv, int offset, int len,
            double[] to, int toOffset) {
        	return ConvertDoubleArrayTo(pv,offset,len,to,toOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toFloat(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toFloatArray(org.epics.ioc.pvAccess.PVData, int, int, float[], int)
         */
        public int toFloatArray(PVData pv, int offset, int len,
            float[] to, int toOffset) {
        	return ConvertFloatArrayTo(pv,offset,len,to,toOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toInt(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toIntArray(org.epics.ioc.pvAccess.PVData, int, int, int[], int)
         */
        public int toIntArray(PVData pv, int offset, int len,
            int[] to, int toOffset) {
        	return ConvertIntArrayTo(pv,offset,len,to,toOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toLong(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toLongArray(org.epics.ioc.pvAccess.PVData, int, int, long[], int)
         */
        public int toLongArray(PVData pv, int offset, int len,
            long[] to, int toOffset) {
        	return ConvertLongArrayTo(pv,offset,len,to,toOffset);
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toShort(org.epics.ioc.pvAccess.PVData)
         */
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
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Convert#toShortArray(org.epics.ioc.pvAccess.PVData, int, int, short[], int)
         */
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n; ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (byte)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (byte)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (byte)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (byte)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (byte)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n;  ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (short)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (short)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (short)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (short)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (short)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n;  ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (int)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (int)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (int)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (int)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (int)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n;  ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (long)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (long)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (long)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (long)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (long)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n;  ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (float)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (float)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (float)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (float)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (float)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n;  ntransfered += n;
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
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        byte[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (double)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvShort : {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        short[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (double)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvInt : {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        int[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (double)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvLong : {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        long[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (double)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvFloat : {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        float[] dataArray = data.data;
                        int dataOffset = data.offset;
                        for(int i=0; i<num; i++)
                            to[i+toOffset] = (double)dataArray[i+dataOffset];
                        len -= num; offset += num; toOffset += num; ntransfered += num;
                    }
                    return ntransfered;
                } 
                case pvDouble : {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    while(len>0) {
                        int num = pvdata.get(offset,len,data);
                        if(num==0) break;
                        double[] dataArray = data.data;
                        int dataOffset = data.offset;
                        System.arraycopy(dataArray,dataOffset,to,toOffset,num);
                        len -= num; offset += num; toOffset += num; ntransfered += num;
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
        
        private int ConvertFromStringArray(PVArray pv, int offset, int len,
                String[]from, int fromOffset)
        {
            Array array = (Array)pv.getField();
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
                        int n = pvdata.put(offset,len,from,fromOffset);
                        if(n==0) break;
                        len -= n; offset += n; fromOffset += n; ntransfered += n;
                    }
                    return ntransfered;
                default:
                    throw new IllegalArgumentException(
                      "Illegal PVType. Must be scalar but it is "
                      + elemType.toString()
                    );
            }
        }
        
        private int ConvertToStringArray(PVArray pv, int offset, int len,
                String[]to, int toOffset)
        {
            Array array = (Array)pv.getField();
            Type elementType = array.getElementType();
            int ncopy = pv.getLength();
            if(ncopy>len) ncopy = len;
            int num = ncopy;
            switch(elementType) {
            case pvUnknown: {
                    for(int i=0; i<num; i++) to[toOffset+i] = "unknown";
                }
                break;
            case pvBoolean: {
                    PVBooleanArray pvdata = (PVBooleanArray)pv;
                    BooleanArrayData data = new BooleanArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            boolean[] dataArray = data.data;
                            Boolean value = new Boolean(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvByte: {
                    PVByteArray pvdata = (PVByteArray)pv;
                    ByteArrayData data = new ByteArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            byte[] dataArray = data.data;
                            Byte value = new Byte(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvShort: {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            short[] dataArray = data.data;
                            Short value = new Short(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvInt: {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            int[] dataArray = data.data;
                            Integer value = new Integer(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvLong: {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            long[] dataArray = data.data;
                            Long value = new Long(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvFloat: {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            float[] dataArray = data.data;
                            Float value = new Float(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvDouble: {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            double[] dataArray = data.data;
                            Double value = new Double(dataArray[data.offset]);
                            to[toOffset+i] = value.toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvString: {
                    PVStringArray pvdata = (PVStringArray)pv;
                    StringArrayData data = new StringArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            String[] dataArray = data.data;
                            to[toOffset+i] = dataArray[data.offset];
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvEnum: {
                    PVEnumArray pvdata = (PVEnumArray)pv;
                    EnumArrayData data = new EnumArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            PVEnum[] dataArray = data.data;
                            to[toOffset+i] = dataArray[data.offset].toString();
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvStructure: {
                    PVStructureArray pvdata = (PVStructureArray)pv;
                    StructureArrayData data = new StructureArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            PVStructure[] dataArray = data.data;
                            if(dataArray==null
                            || (dataArray.length<=data.offset)
                            || dataArray[data.offset]==null) {
                                to[toOffset+i] = "null";
                            } else {
                                to[toOffset+i] = dataArray[data.offset].toString();
                            }
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            case pvArray: {
                    PVArrayArray pvdata = (PVArrayArray)pv;
                    ArrayArrayData data = new ArrayArrayData();
                    for(int i=0; i<num; i++) {
                        if(pvdata.get(offset+i,1,data)==1) {
                            PVArray[] dataArray = data.data;
                            if(dataArray==null
                            || (dataArray.length<=data.offset)
                            || dataArray[data.offset]==null) {
                                to[toOffset+i] = "null";
                            } else {
                                to[toOffset+i] = dataArray[data.offset].toString();
                            }
                        } else {
                            to[toOffset+i] = "bad pv";
                        }
                    }
                }
                break;
            default:    
                throw new IllegalArgumentException(
                        "Illegal PVType. Must be scalar but it is "
                        + elementType.toString()
                      );
            }
            return ncopy;
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
            if(fieldsData!=null) for(PVData fieldData : fieldsData) {
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
                    PVBooleanArray pvdata = (PVBooleanArray)pv;
                    BooleanArrayData data = new BooleanArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             boolean[] value = data.data;
                             if(value[data.offset]) {
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
                    PVByteArray pvdata = (PVByteArray)pv;
                    ByteArrayData data = new ByteArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             byte[] value = data.data;
                             builder.append(String.format("%d ",value[data.offset]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvShort: {
                    PVShortArray pvdata = (PVShortArray)pv;
                    ShortArrayData data = new ShortArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             short[] value = data.data;
                             builder.append(String.format("%d ",value[data.offset]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvInt: {
                    PVIntArray pvdata = (PVIntArray)pv;
                    IntArrayData data = new IntArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             int[] value = data.data;
                             builder.append(String.format("%d ",value[data.offset]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvLong: {
                    PVLongArray pvdata = (PVLongArray)pv;
                    LongArrayData data = new LongArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             long[] value = data.data;
                             builder.append(String.format("%d ",value[data.offset]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvFloat: {
                    PVFloatArray pvdata = (PVFloatArray)pv;
                    FloatArrayData data = new FloatArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             float[] value = data.data;
                             builder.append(String.format("%f ",value[data.offset]));
                        } else {
                             builder.append(indentString + "???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvDouble: {
                    PVDoubleArray pvdata = (PVDoubleArray)pv;
                    DoubleArrayData data = new DoubleArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        if(num==1) {
                             double[] value = data.data;
                             builder.append(String.format("%f ",value[data.offset]));
                        } else {
                             builder.append("???? ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvString: {
                    PVStringArray pvdata = (PVStringArray)pv;
                    StringArrayData data = new StringArrayData();
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        String[] value = data.data;
                        if(num==1 && value[data.offset]!=null) {
                            builder.append("\"");
                            builder.append(value[data.offset]);
                            builder.append("\" ");
                        } else {
                             builder.append("null ");
                        }
                    }
                    builder.append("}");
                    break;
                }
            case pvEnum: {
                    PVEnumArray pvdata = (PVEnumArray)pv;
                    EnumArrayData data = new EnumArrayData();
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        newLine(builder,indentLevel);
                        builder.append(indentString);
                        int num = pvdata.get(i,1,data);
                        PVEnum[] value = data.data;
                        if(num==1 && value[data.offset]!=null) {
                            builder.append(convertEnum(value[data.offset]));
                        } else {
                             builder.append("{} ");
                        }
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                    break;
                }
            case pvStructure: {
                    PVStructureArray pvdata = (PVStructureArray)pv;
                    StructureArrayData data = new StructureArrayData();
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        int num = pvdata.get(i,1,data);
                        PVStructure[] value = data.data;
                        if(num==1 && value[data.offset]!=null) {
                            builder.append(convertStructure(value[data.offset],
                                indentLevel + 1));
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
                    PVArrayArray pvdata = (PVArrayArray)pv;
                    ArrayArrayData data = new ArrayArrayData();
                    newLine(builder,indentLevel);
                    builder.append("{");
                    for(int i=0; i < pvdata.getLength(); i++) {
                        newLine(builder,indentLevel);
                        builder.append(indentString);
                        int num = pvdata.get(i,1,data);
                        PVArray[] value = data.data;
                        if(num==1 && value[data.offset]!=null) {
                            builder.append(convertArray(value[data.offset],
                                indentLevel + 1));
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

        private int CopyNumericArray(PVArray from, int offset, PVArray to, int toOffset, int len)
        {
            Type fromElementType = ((Array)from.getField()).getElementType();
            int ncopy = 0;
            switch(fromElementType) {
            case pvByte: {
                    PVByteArray pvfrom = (PVByteArray)from;
                    ByteArrayData data = new ByteArrayData();
                    while(len>0) {
                        int num = pvfrom.get(offset,len,data);
                        if(num<=0) break;
                        while(num>0) {
                            int n = fromByteArray(to,toOffset,num,data.data,data.offset);
                            if(n<=0) break;
                            len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                        }
                    }
                    break;
                }
            case pvShort: {
                PVShortArray pvfrom = (PVShortArray)from;
                ShortArrayData data = new ShortArrayData();
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromShortArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                break;
                
                }
            case pvInt: {
                PVIntArray pvfrom = (PVIntArray)from;
                IntArrayData data = new IntArrayData();
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromIntArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                break;
                
                }
            case pvLong: {
                PVLongArray pvfrom = (PVLongArray)from;
                LongArrayData data = new LongArrayData();
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromLongArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                break;
                
                }
            case pvFloat: {
                PVFloatArray pvfrom = (PVFloatArray)from;
                FloatArrayData data = new FloatArrayData();
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromFloatArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                break;
                
                }
            case pvDouble: {
                PVDoubleArray pvfrom = (PVDoubleArray)from;
                DoubleArrayData data = new DoubleArrayData();
                while(len>0) {
                    int num = pvfrom.get(offset,len,data);
                    if(num<=0) break;
                    while(num>0) {
                        int n = fromDoubleArray(to,toOffset,num,data.data,data.offset);
                        if(n<=0) break;
                        len -= n; num -= n; ncopy+=n; offset += n; toOffset += n; 
                    }
                }
                break;
                }
            }
            return ncopy;
        }
        
    }  
}
