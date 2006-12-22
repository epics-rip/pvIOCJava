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
public class FieldBase implements Field
{
    private static String indentString = "    ";
    private Field parent;
    private boolean isMutable;
    private String name;
    private Property[] property;
    private Type type;
    private String supportName = null;
    private FieldAttribute fieldAttribute;

    /**
     * Constructor for FieldBase.
     * @param name The field name.
     * @param type The field type.
     * @param property An array of properties for the field.
     * @param fieldAttribute The field attributes.
     */
    public FieldBase(String name, Type type,Property[] property,FieldAttribute fieldAttribute) {
        this.name = name;
        this.type = type;
        if(property==null) property = new Property[0];
        this.property = property;
        this.fieldAttribute = fieldAttribute;
        isMutable = true;
    }   
    /**
     * Generate a newLine and spaces at the beginning of the new line.
     * @param builder The StringBuilder to add the new line.
     * @param indentLevel Indentation level. Ecah level is four spaces.
     */
    public static void newLine(StringBuilder builder, int indentLevel) {
        builder.append(String.format("%n"));
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getName()
     */
    public String getFieldName() {
        return(name);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#getParent()
     */
    public Field getParent() {
        return parent;
    }
    /**
     * Set the parent field.
     * @param field The parent.
     */
    protected void setParent(Field field) {
        parent = field;
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
            if(property[i].getName().equals(propertyName)) return property[i];
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
     * @see org.epics.ioc.pv.Field#isMutable()
     */
    public boolean isMutable() {
        return(isMutable);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#setMutable(boolean)
     */
    public void setMutable(boolean value) {
        isMutable = value;
        
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
        newLine(builder,indentLevel);
        builder.append(String.format("field %s type %s isMutable %b ",
                name,type.toString(),isMutable));
        if(parent!=null) {
            builder.append(String.format("parent %s ", parent.getFieldName()));
        }
        if(supportName!=null) {
            builder.append("supportName " + supportName + " ");
        }
        builder.append(" " + fieldAttribute.toString(indentLevel));
        if(property.length>0) {
            newLine(builder,indentLevel);
            builder.append("property{");
            for(Property prop : property) {
                builder.append(prop.toString(indentLevel + 1));
            }
            newLine(builder,indentLevel);
            builder.append("}");
        }
        return builder.toString();
    }
}
