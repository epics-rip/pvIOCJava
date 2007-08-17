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
    private boolean isMutable;
    private String fieldName;
    private Property[] property;
    private Type type;
    private String supportName = null;
    private String createName = null;
    private FieldAttribute fieldAttribute;
    private static Convert convert = ConvertFactory.getConvert();

    /**
     * Constructor for BaseField.
     * @param fieldName The field fieldName.
     * @param type The field type.
     * @param property An array of properties for the field.
     * If the argument is null then a null array of properties is created.
     * @param fieldAttribute The field attributes.
     * @throws IllegalArgumentException if type or fieldAttribute is null;
     */
    public BaseField(String fieldName, Type type,Property[] property,FieldAttribute fieldAttribute) {
        if(type==null) {
            throw new IllegalArgumentException("type is null");
        }
        if(fieldAttribute==null) {
            throw new IllegalArgumentException("fieldAttribute is null");
        }
        this.fieldName = fieldName;
        this.type = type;
        if(property==null) property = new Property[0];
        this.property = property;
        this.fieldAttribute = fieldAttribute;
        isMutable = true;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getName()
     */
    public String getFieldName() {
        return(fieldName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getPropertys()
     */
    public Property[] getPropertys() {
        return property;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getProperty(java.lang.String)
     */
    public Property getProperty(String propertyName) {
        for(int i=0; i<property.length; i++) {
            if(property[i].getPropertyName().equals(propertyName)) return property[i];
        }
        return null;
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
        builder.append(String.format("field %s type %s isMutable %b ",
                fieldName,type.toString(),isMutable));
        if(supportName!=null) {
            builder.append("supportName " + supportName + " ");
        }
        if(createName!=null) {
            builder.append("createName " + createName + " ");
        }
        builder.append(" " + fieldAttribute.toString(indentLevel));
        if(property.length>0) {
            convert.newLine(builder,indentLevel);
            builder.append("property{");
            for(Property prop : property) {
                builder.append(prop.toString(indentLevel + 1));
            }
            convert.newLine(builder,indentLevel);
            builder.append("}");
        }
        return builder.toString();
    }
}
