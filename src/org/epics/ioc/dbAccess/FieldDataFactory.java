/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Factory to create default implementations for field data
 * @author mrk
 *
 */
public class FieldDataFactory {
   
    /**
     * create implementation for all scalar fields that are of type dbPvData
     * @param dbdField the reflection interface for the field
     * @return the DBData implementation
     */
    public static DBData createScalarData(DBDField dbdField)
    {
        DBType dbType = dbdField.getDBType();
        if(dbType!=DBType.dbPvType) 
            throw new IllegalArgumentException("DBType must be dbPvType");
        Type type = dbdField.getType();
        switch(type) {
        case pvUnknown: return new UnknownData(dbdField);
        case pvBoolean: return new BooleanData(dbdField);
        case pvByte:    return new ByteData(dbdField);
        case pvShort:   return new ShortData(dbdField);
        case pvInt:     return new IntData(dbdField);
        case pvLong:    return new LongData(dbdField);
        case pvFloat:   return new FloatData(dbdField);
        case pvDouble:  return new DoubleData(dbdField);
        case pvString:  return new StringData(dbdField);
        case pvEnum:    break; // illegal
        }
        throw new IllegalArgumentException(
            "Illegal Type. Must be pvUnknown,...,pvString");
    }

    /**
     * create an implementation for an enumerated field
     * @param dbdField the reflection interface for the field
     * @param choice the enum choices
     * @return the DBData implementation
     */
    public static DBData createEnumData(DBDField dbdField, String[] choice)
    {
        return new EnumData(dbdField,choice);
    }

    /**
     * create an implementation for a menu field
     * @param dbdMenuField the reflection interface for the field
     * @return the DBMenu implementation
     */
    public static DBMenu createMenuData(DBDMenuField dbdMenuField)
    {
        return new MenuData(dbdMenuField);
    }

    /**
     * create an implementation for a structure field
     * @param dbdStructureField the reflection interface for the field
     * @return the DBStructure implementation
     */
    public static DBStructure createStructureData(
        DBDStructureField dbdStructureField)
    {
        return new StructureData(dbdStructureField);
    }

    /**
     * create an implementation for a link field
     * @param dbdField the reflection interface for the field
     * @return the DBLink implementation
     */
    public static DBLink createLinkData(DBDField dbdField)
    {
        DBDStructureField dbdStructureField = DBDCreateFactory.createDBDStructureField(
                dbdField.getName(),dbdLinkStructure,dbdField.getPropertys());
        return new LinkData(dbdStructureField);
    }
    
    private static DBDStructure dbdLinkStructure;
    {
        DBDField dbdConfigStructName = DBDCreateFactory.createDBDField(
                "configStructName",Type.pvString,DBType.dbPvType,null);
        DBDField dbdLinkSupportName = DBDCreateFactory.createDBDField(
                "linkSupportName",Type.pvString,DBType.dbPvType,null);
        DBDField[] dbdField = new DBDField[]{dbdConfigStructName,dbdLinkSupportName};
        dbdLinkStructure = DBDCreateFactory.createDBDStructure("link",dbdField,null);
    }

    /**
     * create an implementation for an array field
     * @param dbdArrayField the reflection interface for the field
     * @param capacity the default capacity for the field
     * @param capacityMutable can the capacity be changed after initialization
     * @return the DBArray implementation
     */
    public static DBArray createArrayData(
            DBDArrayField dbdArrayField,int capacity,boolean capacityMutable)
    {
        DBType dbType= dbdArrayField.getDBType();
        switch(dbType) {
        case dbPvType: {
                Type elementType = dbdArrayField.getElementType();
                switch(elementType) {
                case pvBoolean: return new ArrayBooleanData(
                    dbdArrayField, capacity, capacityMutable);
                case pvByte:    return new ArrayByteData(
                    dbdArrayField, capacity, capacityMutable);
                case pvShort:   return new ArrayShortData(
                    dbdArrayField, capacity, capacityMutable);
                case pvInt:     return new ArrayIntData(
                    dbdArrayField, capacity, capacityMutable);
                case pvLong:    return new ArrayLongData(
                    dbdArrayField, capacity, capacityMutable);
                case pvFloat:   return new ArrayFloatData(
                    dbdArrayField, capacity, capacityMutable);
                case pvDouble:  return new ArrayDoubleData(
                    dbdArrayField, capacity, capacityMutable);
                case pvString:  return new ArrayStringData(
                    dbdArrayField, capacity, capacityMutable);
                case pvEnum:    return new ArrayEnumData(
                    dbdArrayField, capacity, capacityMutable);
                }
                throw new IllegalArgumentException(
                    "Illegal Type. Logic error");
            }
        case dbMenu:
            return new ArrayMenuData(
                 dbdArrayField, capacity, capacityMutable);
        case dbStructure:
            return new ArrayStructureData(
                 dbdArrayField, capacity, capacityMutable);
        case dbArray:
            return new ArrayArrayData(
                 dbdArrayField, capacity, capacityMutable);
        case dbLink:
            return new ArrayLinkData(
                 dbdArrayField, capacity, capacityMutable);
        }
        throw new IllegalArgumentException("Illegal Type. Logic error");
    }
    
    private static Convert convert = ConvertFactory.getPVConvert();

    private static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    private static String indentString = "    ";

    private static class UnknownData extends AbstractDBData {

        public String toString() {
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        UnknownData(DBDField dbdField) {
            super(dbdField);
        }

    }

    private static class BooleanData extends AbstractDBData
        implements DBBoolean
    {

        public boolean get() {
            return value;
        }

        public void put(boolean value) {
            if(super.getField().isMutable()) { this.value = value; return ;}
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        BooleanData(DBDField dbdField) {
            super(dbdField);
            value = false;
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractDBData implements DBByte {

        public byte get() {
            return value;
        }

        public void put(byte value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ByteData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractDBData implements DBShort {

        public short get() {
            return value;
        }

        public void put(short value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ShortData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private short value;

    }

    private static class IntData extends AbstractDBData implements DBInt {

        public int get() {
            return value;
        }

        public void put(int value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        IntData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private int value;

    }

    private static class LongData extends AbstractDBData implements DBLong {

        public long get() {
            return value;
        }

        public void put(long value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        LongData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private long value;

    }

    private static class FloatData extends AbstractDBData implements DBFloat {

        public float get() {
            return value;
        }

        public void put(float value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        FloatData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractDBData implements DBDouble {

        public double get() {
            return value;
        }

        public void put(double value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        DoubleData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private double value;

    }

    private static class StringData extends AbstractDBData implements DBString {

        public String get() {
            return value;
        }

        public void put(String value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(DBDField dbdField) {
            super(dbdField);
            value = null;
        }
        
        private String value;

    }

    private static class EnumData extends AbstractDBData implements DBEnum {

        public String[] getChoices() {
            return choice;
        }
        
        public int getIndex() {
            return index;
        }
        public boolean setChoices(String[] choice) {
            if(super.getField().isMutable()) {
                this.choice = choice;
                return true;
            }
            return false;
        }
        
        public void setIndex(int index) {
            if(super.getField().isMutable()) {
                this.index = index;
                return;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        
        EnumData(DBDField dbdField, String[]choice) {
            super(dbdField);
            index = 0;
            if(choice==null) choice = new String[0];
            this.choice = choice;
        }
        
        private int index;
        private String[]choice;

    }

    private static class MenuData extends AbstractDBData implements DBMenu
    {

        public String getMenuName() {
            return menuName;
        }

        public String[] getChoices() {
            return choice;
        }
        
        public int getIndex() {
            return index;
        }
        public boolean setChoices(String[] choice) {
            throw new UnsupportedOperationException(
                "Menu choices can not be modified");
        }
        
        public void setIndex(int index) {
            if(super.getField().isMutable()) { this.index = index; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() { return getString(0);}
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(convert.getString(this,indentLevel));
            newLine(builder,indentLevel + 1);
            builder.append(String.format("DBType %s ",
                    this.getDBType().toString()));
            return builder.toString();
        }
        
        MenuData(DBDMenuField dbdMenuField) {
            super(dbdMenuField);
            index = 0;
            DBDMenu dbdMenu = dbdMenuField.getDBDMenu();
            this.choice = dbdMenu.getChoices();
            this.menuName = dbdMenu.getName();
        }
        
        private int index;
        private String[]choice;
        private String menuName;

    }

    private static class StructureData extends AbstractDBData
        implements DBStructure
    {

        public PVData[] getFieldPVDatas() {
            return pvData;
        }

        public DBData[] getFieldDBDatas() {
            return dbData;
        }

        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            Structure structure = (Structure)this.getField();
            builder.append("structure " + structure.getStructureName() + "{");
            for(int i=0, n= dbData.length; i < n; i++) {
                newLine(builder,indentLevel + 1);
                Field field = pvData[i].getField();
                builder.append(field.getName() + " = ");
                DBDField dbdField = dbData[i].getDBDField();
                switch(dbdField.getDBType()) {
                case dbPvType:
                    builder.append(convert.getString(
                        dbData[i],indentLevel + 1));
                    break;
                case dbMenu:
                    builder.append(((MenuData)dbData[i]).toString(
                        indentLevel + 1));
                    break;
                case dbStructure:
                    builder.append(((StructureData)dbData[i]).toString(
                        indentLevel + 1));
                    break;
                case dbArray:
                    builder.append(((AbstractDBArray)dbData[i]).toString(
                        indentLevel + 1));
                    break;
                case dbLink:builder.append(((LinkData)dbData[i]).toString(
                        indentLevel + 1));
                     break;
                }
                
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        private DBData constructPv(DBDField dbdField) {
           Type type =  dbdField.getType();
           if(type!=Type.pvEnum) return createScalarData(dbdField);
           String[] choice = new String[0];
           return createEnumData(dbdField,choice);
        }
        
        StructureData(DBDStructureField dbdStructureField) {
            super(dbdStructureField);
            DBDStructure dbdStructure = dbdStructureField.getDBDStructure();
            DBDField[] dbdField = dbdStructure.getDBDFields();
            dbData = new DBData[dbdField.length];
            pvData = new PVData[dbData.length];
            for(int i=0; i < dbData.length; i++) {
                DBDField field = dbdField[i];
                switch(field.getDBType()) {
                case dbPvType:
                    dbData[i] = constructPv(field);
                    break;
                case dbMenu:
                    dbData[i] = createMenuData((DBDMenuField)field);
                    break;
                case dbStructure:
                    dbData[i] = createStructureData((DBDStructureField)field);
                    break;
                case dbArray:
                    dbData[i] = createArrayData((DBDArrayField)field,0,true);
                case dbLink:
                    dbData[i] = createLinkData(field);
                    break;
                default:
                }
                pvData[i] = dbData[i];
            }
            
        }
        private PVData[] pvData;
        private DBData[] dbData;
        
    }

    private static class LinkData extends StructureData implements DBLink
    {
        
        public String getConfigStructureFieldName() {
            return configStructFieldName;
        }

        public void putConfigStructureFieldName(DBD dbd, String name) {
            this.dbd = dbd;
            configStructFieldName = name;
        }

        public DBData[] getFieldDBDatas() {
            if(dbConfigStructure==null) return null;
            return(dbConfigStructure.getFieldDBDatas());
        }

        public PVData[] getFieldPVDatas() {
            if(dbConfigStructure==null) return null;
            return(dbConfigStructure.getFieldPVDatas());
        }

        public String toString() {
            return super.toString();
        }

        public String toString(int indentLevel) {
            return super.toString(indentLevel);
        }

        public DBStructure getConfigStructure() {
            return dbConfigStructure;
        }

        public String getLinkSupportName() {
            return dbLink.get();
        }

        public void putLinkSupportName(String name) {
            if(dbd==null)
                throw new IllegalStateException("DBD not specified");
            DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(name);
            if(dbdLinkSupport==null) 
                throw new IllegalArgumentException("no link support");
            String linkSupportName = dbdLinkSupport.getLinkSupportName();
            dbLink.put(linkSupportName);
            String configStructName = dbdLinkSupport.getConfigStructName();
            if(configStructName!= null && configStructName.length()>0) {
                if(configStructFieldName==null)
                    throw new IllegalStateException(
                    "configStructFieldName not specified");
                DBDStructure dbdConfig = dbd.getDBDStructure(
                   configStructFieldName);
                if(dbdConfig==null)
                    throw new IllegalArgumentException(
                        "configStruct" + configStructName + "does not exist");
                DBDStructureField dbdConfigField = DBDCreateFactory.
                    createDBDStructureField(configStructFieldName,dbdConfig,null);
                dbConfigStructure = new StructureData(dbdConfigField);
                dbConfig.put(configStructName);
            } else {
                dbConfigStructure = null;
                dbConfig.put("");
            }
        }

        LinkData(DBDStructureField dbdStructureField)
        {
            super(dbdStructureField);
            dbd = null;
            configStructFieldName = null;
            PVData[] pvData = super.getFieldPVDatas();
            dbConfig = null;
            dbLink = null;
            for(PVData data : pvData) {
                Field field = data.getField();
                if(field.getName().equals("configStructName"))
                    dbConfig = (PVString)data;
                if(field.getName().equals("linkSupportName"))
                    dbConfig = (PVString)data;
            }
            if(dbConfig==null)
                throw new IllegalStateException("Logic error configStructName");
            if(dbLink==null)
                throw new IllegalStateException("Logic error linkSupportName");
            dbConfigStructure = null;
        }

        private DBD dbd;
        private String configStructFieldName;
        private PVString dbConfig;
        private PVString dbLink;
        private DBStructure dbConfigStructure;
    }

    private static class ArrayBooleanData
        extends AbstractDBArray implements DBBooleanArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, boolean[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, boolean[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            boolean[]newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayBooleanData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
        }
        
        private boolean[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayByteData
        extends AbstractDBArray implements DBByteArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, byte[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, byte[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            byte[]newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayByteData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
        }
        
        private byte[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayShortData
        extends AbstractDBArray implements DBShortArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, short[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, short[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            short[]newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayShortData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
        }
        
        private short[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayIntData
        extends AbstractDBArray implements DBIntArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, int[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, int[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            int[]newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayIntData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
        }
        
        private int[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayLongData
        extends AbstractDBArray implements DBLongArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, long[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, long[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            long[]newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayLongData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
        }
        
        private long[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayFloatData
        extends AbstractDBArray implements DBFloatArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, float[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, float[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            float[]newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayFloatData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
        }
        
        private float[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayDoubleData
        extends AbstractDBArray implements DBDoubleArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, double[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, double[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            double[]newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayDoubleData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
        }
        
        private double[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayStringData
        extends AbstractDBArray implements DBStringArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, String[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, String[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            String[]newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayStringData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
        }
        
        private String[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayEnumData
        extends AbstractDBArray implements DBEnumArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, PVEnum[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVEnum[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBEnum[]newarray = new DBEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayEnumData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBEnum[capacity];
        }
        
        private DBEnum[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayMenuData
        extends AbstractDBArray implements DBMenuArray
    {
        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            for(int i=0; i < length; i++) {
                newLine(builder,indentLevel + 1);
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    value[i].toString(indentLevel);
                }
            }
            return builder.toString();
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBMenu[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBMenu[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBMenu[]newarray = new DBMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayMenuData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBMenu[capacity];
        }
        
        private DBMenu[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayStructureData
        extends AbstractDBArray implements DBStructureArray
    {
        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            Structure structure = (Structure)this.getField();
            builder.append("structure " + structure.getStructureName() + "{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    newLine(builder,indentLevel + 1);
                    builder.append("{}");
                } else {
                    value[i].toString(indentLevel);
                }
            }
            return builder.toString();
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBStructure[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBStructure[]newarray = new DBStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayStructureData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBStructure[capacity];
        }
        
        private DBStructure[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayArrayData
        extends AbstractDBArray implements DBArrayArray
    {
 
        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    newLine(builder,indentLevel + 1);
                    builder.append("{}");
                } else {
                    value[i].toString(indentLevel);
                }
            }
            return builder.toString();
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBArray[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBArray[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBArray[]newarray = new DBArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayArrayData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBArray[capacity];
        }
        
        private DBArray[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayLinkData
        extends AbstractDBArray implements DBLinkArray
    {
 
        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    newLine(builder,indentLevel + 1);
                    builder.append("{}");
                } else {
                    value[i].toString(indentLevel);
                }
            }
            return builder.toString();
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBLink[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBLink[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBLink[]newarray = new DBLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayLinkData(DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBLink[capacity];
        }
        
        private DBLink[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

}
