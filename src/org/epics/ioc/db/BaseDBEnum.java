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
public class BaseDBEnum extends BaseDBField implements DBEnum {
    private PVEnum pvEnum;
   
    /**
     * constructor that derived classes must call.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvField The reflection interface.
     */
    public BaseDBEnum(DBField parent,DBRecord record, PVField pvField) {
        super(parent,record,pvField);
        pvEnum = (PVEnum)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#getPVEnum()
     */
    public PVEnum getPVEnum() {
        return pvEnum;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#replacePVEnum()
     */
    public void replacePVEnum() {
        pvEnum = (PVEnum)super.getPVField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#getChoices()
     */
    public String[] getChoices() {
        return pvEnum.getChoices();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#getIndex()
     */
    public int getIndex() {
        return pvEnum.getIndex();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        if(((Enum)pvEnum.getField()).isChoicesMutable()) {
            pvEnum.setChoices(choice);
            Iterator<RecordListener> iter;
            iter = super.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.enumChoicesPut(this);
            }
            DBField parent = super.getParent();
            while(parent!=null) {
                iter = parent.getRecordListenerList().iterator();
                while(iter.hasNext()) {
                    RecordListener listener = iter.next();
                    DBListener dbListener = listener.getDBListener();
                    dbListener.enumChoicesPut(parent, this);
                }
                parent = parent.getParent();
            }
            return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#setIndex(int)
     */
    public void setIndex(int index) {
        if(pvEnum.isMutable()) {
            pvEnum.setIndex(index);
            Iterator<RecordListener> iter;
            iter = super.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.enumIndexPut(this);
            }
            DBField parent = super.getParent();
            while(parent!=null) {
                iter = parent.getRecordListenerList().iterator();
                while(iter.hasNext()) {
                    RecordListener listener = iter.next();
                    DBListener dbListener = listener.getDBListener();
                    dbListener.enumIndexPut(parent, this);
                }
                parent = parent.getParent();
            }
            return;
        }
        throw new IllegalStateException("PVField.isMutable is false");
    }
}
