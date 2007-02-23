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
public class BaseDBEnum extends BaseDBData implements DBEnum {
    private PVEnum pvEnum;
   
    /**
     * constructor that derived classes must call.
     * @param parent The parent interface.
     * @param enumField the reflection interface for the DBEnum data.
     * @param choice the choices for the enum.
     */
    public BaseDBEnum(DBData parent,DBRecord record, PVData pvData) {
        super(parent,record,pvData);
        pvEnum = (PVEnum)pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getChoices()
     */
    public String[] getChoices() {
        return pvEnum.getChoices();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getIndex()
     */
    public int getIndex() {
        return pvEnum.getIndex();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#setChoices(java.lang.String[])
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
            DBData parent = super.getParent();
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
     * @see org.epics.ioc.pv.PVEnum#setIndex(int)
     */
    public void setIndex(int index) {
        if(pvEnum.getField().isMutable()) {
            pvEnum.setIndex(index);
            Iterator<RecordListener> iter;
            iter = super.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.enumIndexPut(this);
            }
            DBData parent = super.getParent();
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
        throw new IllegalStateException("PVData.isMutable is false");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBEnum#getPVEnum()
     */
    public PVEnum getPVEnum() {
        return (PVEnum)super.getPVData();
    }
}
