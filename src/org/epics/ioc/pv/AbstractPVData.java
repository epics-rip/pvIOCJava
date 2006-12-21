/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.MessageType;

/**
 * Abstract base class for a PVData.
 * @author mrk
 *
 */
public abstract class AbstractPVData implements PVData{
    private static String indentString = "    "; 
    private Field field;
    private PVData parent;
    private String supportName = null;
       
    protected AbstractPVData(PVData parent, Field field) {
        this.field = field;
        this.parent = parent;
    }
    /**
     * Called by derived classes to replace a field.
     * @param field The new field.
     */
    protected void replaceField(Field field) {
        this.field = field;
    }
    /**
     * used by toString to start a new line.
     * @param builder the stringBuilder to which output is added.
     * @param indentLevel indentation level.
     */
    protected static void newLine(StringBuilder builder, int indentLevel) {
        builder.append(String.format("%n"));
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getFullFieldName()
     */
    public abstract String getFullFieldName();
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public abstract void message(String message, MessageType messageType);
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getField()
     */
    public Field getField() {
        return field;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getParent()
     */
    public PVData getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getSupportName()
     */
    public String getSupportName() {
        return supportName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        supportName = name;
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        if(supportName!=null) {
            return " supportName " + supportName;
        }
        return "";
    }

}
