/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import java.util.Date;

import org.eclipse.swt.widgets.Text;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.property.PVAlarm;
import org.epics.pvdata.property.PVAlarmFactory;
import org.epics.pvdata.property.PVEnumerated;
import org.epics.pvdata.property.PVEnumeratedFactory;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;


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
        private static final Convert convert = ConvertFactory.getConvert();
        private String structureName;
        private PVStructure pvStructure;
        private BitSet changeBitSet;
        private BitSet overrunBitSet;
        private Text text;
        private StringBuilder builder = new StringBuilder();
        private TimeStamp timeStamp = TimeStampFactory.create();
        private PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        private Alarm alarm = new Alarm();
        private PVAlarm pvAlarm = PVAlarmFactory.create();
        private PVEnumerated pvEnumerated = PVEnumeratedFactory.create();
        
        private PrintModifiedImpl(String structureName,PVStructure pvStructure,BitSet changeBitSet,BitSet overrunBitSet,Text text) {
            this.structureName = structureName;
            this.pvStructure = pvStructure;
            this.changeBitSet = changeBitSet;
            this.overrunBitSet = overrunBitSet;
            this.text = text;
            if(overrunBitSet==null) {
                this.overrunBitSet = new BitSet(changeBitSet.size());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.swtshell.PrintModified#print()
         */
       @Override
        public void print() {
            builder.setLength(0);
            builder.append("record ");
            builder.append(structureName); 
            int offset = changeBitSet.nextSetBit(0);
            if(offset<0) {
                builder.append(" no changes");
            } else {
                printStructure(pvStructure,0,((offset==0) ? true : false));
            }
            convert.newLine(builder, 0);
            text.append(builder.toString());
        }
        
       
        private void printStructure(PVStructure pvStructure,int indentLevel,boolean printAll) {
            int offset = pvStructure.getFieldOffset();
            if(changeBitSet.get(offset)) printAll = true;
            String fieldName = pvStructure.getFieldName();
            if(fieldName!=null && fieldName.equals("timeStamp") && pvTimeStamp.attach(pvStructure)) {
                convert.newLine(builder, indentLevel);
                pvTimeStamp.get(timeStamp);
                long milliPastEpoch = timeStamp.getMilliSeconds();
                int userTag = timeStamp.getUserTag();
                Date date = new Date(milliPastEpoch);
                builder.append(String.format("timeStamp %tF %tT.%tL userTag %d", date,date,date,userTag));
                if(overrunBitSet.get(offset)) {
                    builder.append(" overrun");
                }
                return;
            }
            String extendsName = pvStructure.getExtendsStructureName();
            if(indentLevel>0) {
                convert.newLine(builder, indentLevel);
                if(extendsName==null || extendsName.length()<1) {
                    builder.append("structure ");
                } else {
                    builder.append(extendsName);
                    builder.append(" ");
                }
                builder.append(fieldName);
            }
            if(fieldName!=null && pvStructure.getFieldName().equals("alarm") && pvAlarm.attach(pvStructure)) {
                pvAlarm.get(alarm);
                PVField[] pvFields = pvStructure.getPVFields();
                if(printAll || changeBitSet.get(pvFields[0].getFieldOffset())) {
                    convert.newLine(builder, indentLevel+1);
                    builder.append("severity ");
                    AlarmSeverity severity = alarm.getSeverity();
                    builder.append(severity.toString());
                    builder.append(" status ");
                    AlarmStatus status = alarm.getStatus();
                    builder.append(status.toString());
                    if(overrunBitSet.get(pvFields[0].getFieldOffset())) {
                        builder.append(" overrun");
                    }
                }
                if(printAll || changeBitSet.get(pvFields[0].getFieldOffset())) {
                    convert.newLine(builder, indentLevel+1);
                    builder.append("message ");
                    String message = alarm.getMessage();
                    builder.append(message);
                    if(overrunBitSet.get(pvFields[1].getFieldOffset())) {
                        builder.append(" overrun");
                    }
                }
                return;
            }
            PVField[] pvFields = pvStructure.getPVFields();
            for(PVField pvField : pvFields) {
                offset = pvField.getFieldOffset();
                if(pvField.getField().getType()==Type.structure) {
                    boolean printIt = false;
                    int nextSet = changeBitSet.nextSetBit(offset);
                    if(nextSet>=0 && (nextSet<pvField.getNextFieldOffset())) printIt = true;
                    if(printAll || printIt) {
                        printStructure((PVStructure)pvField,indentLevel+1,printAll);
                    }
                    continue;
                }
                if(!printAll && !changeBitSet.get(offset)) continue;
                convert.newLine(builder, indentLevel+1);
                pvField.toString(builder, indentLevel+1);
                if(pvField.getFieldName().equals("index") && pvEnumerated.attach(pvField.getParent())) {
                    builder.append(" choice ");
                    builder.append(pvEnumerated.getChoice());
                }
                if(overrunBitSet.get(offset)) {
                    builder.append(" overrun");
                }
            }
        }
    }
}
