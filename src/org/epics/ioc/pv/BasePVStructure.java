/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * @author mrk
 *
 */
public class BasePVStructure extends AbstractPVData implements PVStructure
{
    private static Convert convert = ConvertFactory.getConvert();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private PVData[] pvDatas;
    
    /**
     * Constructor.
     * @param parent The parent interface.
     * @param structure the reflection interface for the PVStructure data.
     */
    public BasePVStructure(PVData parent, Structure structure) {
        super(parent,structure);
        String structureName = structure.getStructureName();
        if(structureName==null) {
            pvDatas = new PVData[0];
            return;
        }
        Field[] fields = structure.getFields();
        pvDatas = new PVData[fields.length];
        for(int i=0; i < pvDatas.length; i++) {
            pvDatas[i] = pvDataCreate.createData(this,fields[i]);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVData#setRecord(org.epics.ioc.pv.PVRecord)
     */
    @Override
    public void setRecord(PVRecord record) {
        super.setRecord(record);
        for(PVData pvData : pvDatas) {
            AbstractPVData abstractPVData = (AbstractPVData)pvData;
            abstractPVData.setRecord(record);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
     */
    public boolean replaceStructureField(String fieldName, Structure structure) {
        Structure oldStructure = (Structure)super.getField();
        int index = oldStructure.getFieldIndex(fieldName);
        PVData newData = pvDataCreate.createData(this, structure);
        pvDatas[index] = newData;
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
     * @see org.epics.ioc.pv.PVStructure#getFieldPVDatas()
     */
    public PVData[] getFieldPVDatas() {
        return pvDatas;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVData#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }    
    /**
     * Convert to string with a prefix.
     * @param prefix The prefix.
     * @param indentLevel The indentation level.
     * @return The string showing the structure.
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
        for(int i=0, n= pvDatas.length; i < n; i++) {
            convert.newLine(builder,indentLevel + 1);
            Field field = pvDatas[i].getField();
            builder.append(field.getFieldName() + " = ");
            builder.append(pvDatas[i].toString(indentLevel + 2));            
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
