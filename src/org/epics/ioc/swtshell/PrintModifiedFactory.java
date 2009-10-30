/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Date;

import org.eclipse.swt.widgets.Text;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;


/**
 * Factory which implements PrintModified.
 * @author mrk
 *
 */
public class PrintModifiedFactory {
   
    /**
     * Create a PrintModified.
     * @param structureName The structure name.
     * @param pvStructure The structure holding data to print.
     * @param changeBitSet The bitset that shows which fields have been changed.
     * @param overrunBitSet The bitset that shows which fields have been changed multiple times.
     * @param text The text widget in which the output will be printed.
     * @return The PrintModified interface.
     */
    public static PrintModified create(String structureName,PVStructure pvStructure,BitSet changeBitSet,BitSet overrunBitSet,Text text) {
        return new PrintModifiedImpl(structureName,pvStructure,changeBitSet,overrunBitSet,text);
    }
    
    private static class PrintModifiedImpl implements PrintModified{
        private String structureName;
        private PVStructure pvStructure;
        private BitSet changeBitSet;
        private BitSet overrunBitSet;
        private Text text;
        
        private PrintModifiedImpl(String structureName,PVStructure pvStructure,BitSet changeBitSet,BitSet overrunBitSet,Text text) {
            this.structureName = structureName;
            this.pvStructure = pvStructure;
            this.changeBitSet = changeBitSet;
            this.overrunBitSet = overrunBitSet;
            this.text = text;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.PrintModified#print()
         */
       @Override
        public void print() {
            newLine(0);
            if(structureName!=null) text.append(structureName);
            int offset = changeBitSet.nextSetBit(0);
            if(offset<0) {
                text.append(" no changes");
                newLine(0);
                return;
            }
            // following code allows timeStamp to be displayed properly.
            if(offset==0) {
                boolean setOverrun = false;
                if(overrunBitSet!=null) {
                    setOverrun = (overrunBitSet.nextSetBit(0)>=0);
                }
                if(setOverrun) {
                    overrunBitSet.clear();
                }
                PVField[] pvFields = pvStructure.getPVFields();
                changeBitSet.clear();
                for(int i=0; i<pvFields.length; i++) {
                    offset = pvFields[i].getFieldOffset();
                    changeBitSet.set(offset);
                    if(setOverrun) {
                        overrunBitSet.set(offset);
                    }
                }
            }
            printStructure(pvStructure,0);
            this.structureName = null;
            this.pvStructure = null;
            this.changeBitSet = null;
            this.overrunBitSet = null;
            this.text = null;
        }
        
        private void printStructure(PVStructure pvStructure,int indentLevel) {
            int offset = pvStructure.getFieldOffset();
            int numberFields = pvStructure.getNumberFields();
            int next = changeBitSet.nextSetBit(offset);
            if(next<0) return;
            if(next>=offset+numberFields) return;
            String fieldName = pvStructure.getField().getFieldName();
            TimeStamp timeStamp = TimeStampFactory.getTimeStamp(pvStructure);
            if(timeStamp!=null) {
                long milliPastEpoch = timeStamp.getMilliSeconds();
                Date date = new Date(milliPastEpoch);
                text.append(String.format(" = %tF %tT.%tL", date,date,date));
                return;
            }
            if(offset==next) {
                text.append(" = " + pvStructure.toString(indentLevel));
                return;
            } 
            PVField[] pvFields = pvStructure.getPVFields();
            for(PVField pvField : pvFields) {
                offset = pvField.getFieldOffset();
                numberFields = pvField.getNumberFields();
                next = changeBitSet.nextSetBit(offset);
                if(next<0) return;
                if(next>=offset+numberFields) continue;
                fieldName = pvField.getField().getFieldName();
                newLine(indentLevel+1);
                if(overrunBitSet!=null) {
                    boolean isSet = overrunBitSet.get(offset);
                    if(isSet) text.append("(overrun) ");
                }
                text.append(fieldName);
                if(numberFields==1) {
                    text.append(" = " + pvField.toString(indentLevel+1));
                } else {
                    printStructure((PVStructure)pvField,indentLevel+1);
                }
            }
            
        }
 
        final static String indentString = "    ";
        
        private void newLine(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%n"));
            for (int i=0; i <indentLevel; i++) builder.append(indentString);
            text.append(builder.toString());
        }
    }

}
