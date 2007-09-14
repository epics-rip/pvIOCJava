/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;
import org.epics.ioc.util.*;

/**
 * Base class for a PVStructure.
 * @author mrk
 *
 */
public class BasePVStructure extends AbstractPVField implements PVStructure
{
    static private Pattern periodPattern = Pattern.compile("[.]");
    private static Convert convert = ConvertFactory.getConvert();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private PVField[] pvFields;
    
    /**
     * Constructor.
     * @param parent The parent interface.
     * @param structure the reflection interface for the PVStructure data.
     */
    public BasePVStructure(PVField parent, Structure structure) {
        super(parent,structure);
        String structureName = structure.getStructureName();
        if(structureName==null) {
            pvFields = new PVField[0];
            return;
        }
        Field[] fields = structure.getFields();
        pvFields = new PVField[fields.length];
        for(int i=0; i < pvFields.length; i++) {
            pvFields[i] = pvDataCreate.createPVField(this,fields[i]);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getStructure()
     */
    public Structure getStructure() {
        return (Structure)getField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#setRecord(org.epics.ioc.pv.PVRecord)
     */
    public void setRecord(PVRecord record) {
        super.setRecord(record);
        for(PVField pvField : pvFields) {
            AbstractPVField abstractPVField = (AbstractPVField)pvField;
            abstractPVField.setRecord(record);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVField)
     */
    public boolean replaceStructureField(String fieldName, Structure structure) {
        Structure oldStructure = (Structure)super.getField();
        int index = oldStructure.getFieldIndex(fieldName);
        Structure fieldStructure = fieldCreate.createStructure(
            fieldName, structure.getStructureName(), structure.getFields());
        PVField newField = pvDataCreate.createPVField(this, fieldStructure);
        pvFields[index] = newField;
        // Must create and replace the Structure for this structure.
        Field[] oldFields = oldStructure.getFields();
        int length = oldFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) newFields[i] = oldFields[i];
        newFields[index] = fieldStructure;
        Structure newStructure = fieldCreate.createStructure(
            oldStructure.getFieldName(),
            oldStructure.getStructureName(),
            newFields,
            oldStructure.getFieldAttribute());
        newStructure.setSupportName(oldStructure.getSupportName());
        super.replaceField(newStructure);
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getFieldPVFields()
     */
    public PVField[] getFieldPVFields() {
        return pvFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getBooleanField(java.lang.String)
     */
    public PVBoolean getBooleanField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvBoolean) {
            super.message(
                "fieldName " + fieldName + " does not have type boolean ",
                MessageType.error);
            return null;
        }
        return (PVBoolean)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getByteField(java.lang.String)
     */
    public PVByte getByteField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvByte) {
            super.message(
                "fieldName " + fieldName + " does not have type byte ",
                MessageType.error);
            return null;
        }
        return (PVByte)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getShortField(java.lang.String)
     */
    public PVShort getShortField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvShort) {
            super.message(
                "fieldName " + fieldName + " does not have type short ",
                MessageType.error);
            return null;
        }
        return (PVShort)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getIntField(java.lang.String)
     */
    public PVInt getIntField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvInt) {
            super.message(
                "fieldName " + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVInt)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getLongField(java.lang.String)
     */
    public PVLong getLongField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvLong) {
            super.message(
                "fieldName " + fieldName + " does not have type long",
                MessageType.error);
            return null;
        }
        return (PVLong)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getFloatField(java.lang.String)
     */
    public PVFloat getFloatField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvFloat) {
            super.message(
                "fieldName " + fieldName + " does not have type float",
                MessageType.error);
            return null;
        }
        return (PVFloat)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getDoubleField(java.lang.String)
     */
    public PVDouble getDoubleField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvDouble) {
            super.message(
                "fieldName " + fieldName + " does not have type double",
                MessageType.error);
            return null;
        }
        return (PVDouble)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getStringField(java.lang.String)
     */
    public PVString getStringField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()!=Type.pvString) {
            super.message(
                "fieldName " + fieldName + " does not have type string",
                MessageType.error);
            return null;
        }
        return (PVString)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getStructureField(java.lang.String, java.lang.String)
     */
    public PVStructure getStructureField(String fieldName, String structureName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.pvStructure) {
            super.message(
                "fieldName " + fieldName + " does not have type structure ",
                MessageType.error);
            return null;
        }
        Structure fieldStructure = (Structure)field;
        if(!fieldStructure.getStructureName().equals(structureName)) {
            super.message(
                    "fieldName " + fieldName + " is not a structure " + structureName,
                    MessageType.error);
                return null;
        }
        return (PVStructure)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getArrayField(java.lang.String, org.epics.ioc.pv.Type)
     */
    public PVArray getArrayField(String fieldName, Type elementType) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.pvArray) {
            super.message(
                "fieldName " + fieldName + " does not have type array ",
                MessageType.error);
            return null;
        }
        Array array = (Array)field;
        if(array.getElementType()!=elementType) {
            super.message(
                    "fieldName "
                    + fieldName + " is array but does not have elementType " + elementType.toString(),
                    MessageType.error);
                return null;
        }
        return (PVArray)pvField;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }       
    /**
     * Called by BasePVRecord.
     * @param prefix A prefix for the generated stting.
     * @param indentLevel The indentation level.
     * @return String showing the PVStructure.
     */
    protected String toString(String prefix,int indentLevel) {
        return getString(prefix,indentLevel);
    }
    
    private PVField findSubField(String fieldName,PVStructure pvStructure) {
        String[] names = periodPattern.split(fieldName,2);
        PVField[] subFields = pvStructure.getFieldPVFields();
        Structure structure = (Structure)pvStructure.getField();
        int index = structure.getFieldIndex(names[0]);
        if(index<0) {
            pvStructure.message(" fieldName " + fieldName + " not found", MessageType.error);
            return null;
        }
        PVField pvField = subFields[index];
        if(names.length==1) return pvField;
        if(pvField.getField().getType()!=Type.pvStructure) return null;
        return findSubField(names[1],(PVStructure)pvField);
    }
    private String getString(String prefix,int indentLevel) {
        StringBuilder builder = new StringBuilder();
        convert.newLine(builder,indentLevel);
        Structure structure = (Structure)super.getField();
        builder.append(prefix + " " + structure.getStructureName());
        builder.append(super.toString(indentLevel));
        convert.newLine(builder,indentLevel);
        builder.append("{");
        for(int i=0, n= pvFields.length; i < n; i++) {
            convert.newLine(builder,indentLevel + 1);
            Field field = pvFields[i].getField();
            builder.append(field.getFieldName() + " = ");
            builder.append(pvFields[i].toString(indentLevel + 2));            
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
