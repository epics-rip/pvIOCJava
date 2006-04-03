/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for DBEnum
 * @author mrk
 *
 */
public abstract class AbstractDBEnum extends AbstractDBData implements DBEnum {

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#getChoices()
     */
    public String[] getChoices() {
        return choice;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#getIndex()
     */
    public int getIndex() {
        return index;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        if(super.getField().isMutable()) {
            this.choice = choice;
            return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#setIndex(int)
     */
    public void setIndex(int index) {
        if(super.getField().isMutable()) {
            this.index = index;
            return;
        }
        throw new IllegalStateException("PVData.isMutable is false");
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return convert.getString(this);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return convert.getString(this, indentLevel);
    }
    
    /**
     * @param dbdField
     * @param choice
     */
    AbstractDBEnum(DBDField dbdField, String[]choice) {
        super(dbdField);
        index = 0;
        if(choice==null) choice = EMPTY_STRING_ARRAY;
        this.choice = choice;
    }
    
    /**
     * tndex of current choice
     */
    protected int index;
    /**
     * array of choices
     */
    protected String[]choice;

    private final static String[] EMPTY_STRING_ARRAY = new String[0];
    private static Convert convert = ConvertFactory.getPVConvert();

}
