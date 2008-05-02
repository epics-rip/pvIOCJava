/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Base class for creating a Field.
 * It can also be a complete implementation.
 * @author mrk
 *
 */
public class BaseField implements Field
{
    private String fieldName;
    private Type type;
    private String supportName = null;
    private String createName = null;
    private FieldAttribute fieldAttribute;
    private static Convert convert = ConvertFactory.getConvert();

    /**
     * Constructor for BaseField.
     * @param fieldName The field fieldName.
     * @param type The field type.
     * @param fieldAttribute The field attributes.
     * @throws IllegalArgumentException if type or fieldAttribute is null;
     */
    public BaseField(String fieldName, Type type,FieldAttribute fieldAttribute) {
        if(type==null) {
            throw new IllegalArgumentException("type is null");
        }
        if(fieldAttribute==null) fieldAttribute = new BaseFieldAttribute();
        this.fieldName = fieldName;
        this.type = type;
        this.fieldAttribute = fieldAttribute;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getFieldName()
     */
    public String getFieldName() {
        return(fieldName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getType()
     */
    public Type getType() {
        return type;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getSupportName()
     */
    public String getSupportName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#setSupportName(java.lang.String)
     */
    public void setSupportName(String name) {
        this.supportName = name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getCreateName()
     */
    public String getCreateName() {
        return createName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#setCreateName(java.lang.String)
     */
    public void setCreateName(String name) {
        createName = name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getFieldAttribute()
     */
    public FieldAttribute getFieldAttribute() {
        return fieldAttribute;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        convert.newLine(builder,indentLevel);
        builder.append(String.format("field %s type %s",
                fieldName,type.toString()));
        if(supportName!=null) {
            builder.append(" supportName " + supportName + " ");
        }
        if(createName!=null) {
            builder.append(" createName " + createName + " ");
        }
        builder.append(" " + fieldAttribute.toString(indentLevel));
        return builder.toString();
    }
}
