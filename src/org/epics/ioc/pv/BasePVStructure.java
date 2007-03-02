/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Base class for a PVStructure.
 * @author mrk
 *
 */
public class BasePVStructure extends AbstractPVField implements PVStructure
{
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
     * @see org.epics.ioc.pv.AbstractPVField#setRecord(org.epics.ioc.pv.PVRecord)
     */
    @Override
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
        PVField newField = pvDataCreate.createPVField(this, structure);
        pvFields[index] = newField;
        // Must create and replace the Structure for this structure.
        Field[] oldFields = oldStructure.getFields();
        int length = oldFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) newFields[i] = oldFields[i];
        newFields[index] = structure;
        Structure newStructure = fieldCreate.createStructure(
            oldStructure.getFieldName(),
            oldStructure.getStructureName(),
            newFields,
            oldStructure.getPropertys(),
            oldStructure.getFieldAttribute());
        super.replaceField(newStructure);
        super.setSupportName(oldStructure.getSupportName());
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getFieldPVFields()
     */
    public PVField[] getFieldPVFields() {
        return pvFields;
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
