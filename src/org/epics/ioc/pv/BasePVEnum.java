/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;


/**
 * Abstract base class for PVEnum
 * @author mrk
 *
 */
public class BasePVEnum extends AbstractPVField implements PVEnum {
    private int index;
    private String[]choice;

    private final static String[] EMPTY_STRING_ARRAY = new String[0];
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * Constructor.
     * @param parent The parent interface.
     * @param enumField the reflection interface for the PVEnum data.
     */
    public BasePVEnum(PVField parent,Enum enumField) {
        super(parent,enumField);
        index = 0;
        choice = EMPTY_STRING_ARRAY;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getEnum()
     */
    public Enum getEnum() {
        return (Enum)getField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getChoices()
     */
    public String[] getChoices() {
        return choice;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getIndex()
     */
    public int getIndex() {
        return index;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choices) {
        if(super.getField().isMutable()) {
            this.choice = choices;
            return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#setIndex(int)
     */
    public void setIndex(int index) {
        if(super.getField().isMutable()) {
            this.index = index;       
            return;
        }
        throw new IllegalStateException("PVField.isMutable is false");
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
    }
}
