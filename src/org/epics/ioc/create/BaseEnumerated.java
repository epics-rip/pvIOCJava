/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.DBField;
import org.epics.ioc.pv.AbstractPVArray;
import org.epics.ioc.pv.AbstractPVField;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.util.MessageType;

/**
 * Base class for an enumerated structure, which is a structure that has an array of string choices
 * and an index field and a choice field.
 * The index an choice select one of the choices.
 * A put to the index field will also update the choice field and a put to the choice will update the index.
 * This class overrides the PVField implementation for all three fields.
 * @author mrk
 *
 */
public class BaseEnumerated implements Enumerated{
    private static Convert convert = ConvertFactory.getConvert();
    private int index;
    private String[] choices;
    private PVField pvField;
    private DBField dbIndex;
    private DBField dbChoice;
    private PVInt pvIndex;
    private PVString pvChoice;
    private PVStringArray pvChoices;

    /**
     * The constructor.
     * @param dbIndex The DBField for the index.
     * @param dbChoice The DBField for the choice.
     * @param dbChoices The DBField for the choices.
     */
    public BaseEnumerated(PVField pvField,DBField dbIndex, DBField dbChoice, DBField dbChoices) {
        this.pvField = pvField;
        this.dbIndex = dbIndex;
        this.dbChoice = dbChoice;            
        PVString pvChoice = (PVString)dbChoice.getPVField();
        PVStringArray pvChoices = (PVStringArray)dbChoices.getPVField();
        PVField pvParent = dbIndex.getParent().getPVField();
        PVInt pvNewIndex = new Index(pvParent,dbIndex.getPVField().getField());
        PVString pvNewChoice = new Choice(pvParent,pvChoice.getField());      
        PVStringArray pvNewChoices = new Choices(
                pvParent,pvChoices.getArray(),pvChoices.getCapacity(),pvChoices.isCapacityMutable());
        dbIndex.replacePVField(pvNewIndex);
        dbChoice.replacePVField(pvNewChoice);
        dbChoices.replacePVField(pvNewChoices);
        if(pvChoices.getLength()>0) {
            StringArrayData stringArrayData = new StringArrayData();
            int len = pvChoices.get(0,pvChoices.getLength(), stringArrayData);
            pvNewChoices.put(0, len, stringArrayData.data , 0);
        }        
        String choice = pvChoice.get();
        if(choice!=null) pvNewChoice.put(choice);
        this.pvIndex = pvNewIndex;
        this.pvChoice = pvNewChoice;
        this.pvChoices = pvNewChoices;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnumerated#getPVField()
     */
    public PVField getPVField() {
        return pvField;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.create.Enumerated#getChoiceField()
     */
    public PVString getChoiceField() {
        return pvChoice;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.create.Enumerated#getChoicesField()
     */
    public PVStringArray getChoicesField() {
        return pvChoices;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.create.Enumerated#getIndexField()
     */
    public PVInt getIndexField() {
        return pvIndex;
    }

    private class Index extends AbstractPVField implements PVInt {
        private Index(PVField parent,Field field) {
            super(parent,field);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            if(index>=choices.length) return -1;
            return index;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return;
            }
            if(value<0 || value>=choices.length) {
                super.message("illegal choice " + value + " num choices " + choices.length, MessageType.error);
                return;
            }
            if(index!=value) {
                index = value;
                dbChoice.postPut();
            }
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

    private class Choice extends AbstractPVField implements PVString {

        private Choice(PVField parent,Field field) {
            super(parent,field);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            if(index>=choices.length) return null;
            return choices[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(!super.isMutable()) {   
                super.message("not isMutable", MessageType.error);
            }
            for(int i=0; i<choices.length; i++) {
                if(value.equals(choices[i])) {
                    if(index!=i) {
                        index = i;
                        dbIndex.postPut();
                    }
                    return;
                }
            }
            super.message("illegal choice", MessageType.error);
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

    private class Choices extends AbstractPVArray implements PVStringArray
    {
        private Choices(PVField parent,Array array,
                int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            choices = new String[capacity];           
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                String[]newarray = new String[len];
                if(length>0) System.arraycopy(choices,0,newarray,0,length);
                choices = newarray;
                capacity = len;
                if(index>=capacity) index = 0;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#get(int, int, org.epics.ioc.pv.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                if(n<=0) return 0;
                data.data = choices;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }           
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#put(int, int, java.lang.String[], int)
         */
        public int put(int offset, int len, String[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,choices,offset,len);
                if(index>=length) {
                    index = 0;
                    dbIndex.postPut();
                    dbChoice.postPut();
                }
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }
    }
}
