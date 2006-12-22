/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;

/**
 * Abstract base class for DBEnum
 * @author mrk
 *
 */
public abstract class AbstractDBEnum extends AbstractDBData implements PVEnum {
    private int index;
    private String[]choice;

    private final static String[] EMPTY_STRING_ARRAY = new String[0];
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * constructor that derived classes must call.
     * @param parent The parent interface.
     * @param dbdEnumField the reflection interface for the DBEnum data.
     * @param choice the choices for the enum.
     */
    protected AbstractDBEnum(DBData parent,Enum enumField, String[]choice) {
        super(parent,enumField);
        index = 0;
        if(choice==null) choice = EMPTY_STRING_ARRAY;
        this.choice = choice;
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
    public boolean setChoices(String[] choice) {
        if(super.getField().isMutable()) {
            this.choice = choice;
            AbstractDBData dbData = this;
            while(dbData!=null) {
                Iterator<RecordListener> iter = dbData.listenerList.iterator();
                while(iter.hasNext()) {
                    RecordListener listener = iter.next();
                    DBListener dbListener = listener.getDBListener();
                    Type type = dbData.getField().getType();
                    if(type==Type.pvEnum) {
                        dbListener.enumChoicesPut(this);
                    } else if(type==Type.pvStructure) {
                        dbListener.structurePut(dbData.thisStructure, this);
                    } else {
                        throw new IllegalStateException("Logic error");
                    }
                }
                if(dbData.parent==dbData) {
                    System.err.printf("setChoices parent = this Why???%n");
                } else {
                    dbData = (AbstractDBData)dbData.parent;
                }
            }
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
            AbstractDBData dbData = this;
            while(dbData!=null) {
                Iterator<RecordListener> iter = dbData.listenerList.iterator();
                while(iter.hasNext()) {
                    RecordListener listener = iter.next();
                    DBListener dbListener = listener.getDBListener();
                    Type type = dbData.getField().getType();
                    if(type==Type.pvEnum || type ==Type.pvMenu) {
                        dbListener.enumIndexPut(this);
                    } else if(type==Type.pvStructure) {
                        dbListener.structurePut(dbData.thisStructure, this);
                    } else {
                        throw new IllegalStateException("Logic error");
                    }
                }
                if(dbData.parent==dbData) {
                    System.err.printf("setChoices parent = this Why???%n");
                } else {
                    dbData = (AbstractDBData)dbData.parent;
                }
            }
            return;
        }
        throw new IllegalStateException("PVData.isMutable is false");
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#toString(int)
     */
    public String toString(int indentLevel) {
        return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
    }
}
