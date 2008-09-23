/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Date;

import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.CDArray;
import org.epics.ioc.ca.CDArrayArray;
import org.epics.ioc.ca.CDField;
import org.epics.ioc.ca.CDRecord;
import org.epics.ioc.ca.CDStructure;
import org.epics.ioc.ca.CDStructureArray;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.PVTimeStamp;
import org.epics.ioc.util.TimeStamp;

/**
 * Factory which implememnts CDPrint.
 * @author mrk
 *
 */
public class CDPrintFactory {
    /**
     * Create a CDPrint.
     * @param cdRecord The cdRecord to print.
     * @param text The text widget in which the results are written.
     * @return The CDPrint interface.
     */
    public static CDPrint create(CDRecord cdRecord,Text text) {
        return new CDPrintImpl(cdRecord,text);
    }
    
    private static class CDPrintImpl implements CDPrint{
        private CDRecord cdRecord;
        private Text text;
        
        private CDPrintImpl(CDRecord cdRecord,Text text) {
            this.cdRecord = cdRecord;
            this.text = text;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.CDPrint#print()
         */
        public void print() {
            text.append(cdRecord.getPVRecord().getRecordName());
            CDStructure cdStructure = cdRecord.getCDStructure();
            int maxNumPuts = cdStructure.getMaxNumPuts();
            if(maxNumPuts==0) {
                text.append(" no changes");
                newLine(0);
                return;
            }
            if(maxNumPuts>1) {
                text.append(String.format(" maxNumPuts %d",maxNumPuts));
            }
            printStructure(cdStructure,1,false);
            newLine(0);
        }
        
        //return (false,true) if (0, not 0)
        private boolean checkNumPuts(CDField cdField) {
            int numPuts = cdField.getNumPuts();
            if(numPuts==0) return false;
            if(numPuts>1) {
                    text.append(String.format(" numPuts %d",numPuts));
            }
            return true;
        }
        
        private void printStructure(CDStructure cdStructure,int indentLevel,boolean printAll) {
            if(checkNumPuts(cdStructure)) printAll = true;
            PVTimeStamp pvTimeStamp = PVTimeStamp.create(cdStructure.getPVField());
            if(pvTimeStamp!=null) {
                TimeStamp timeStamp = new TimeStamp();
                pvTimeStamp.get(timeStamp);
                long secondPastEpochs = timeStamp.secondsPastEpoch;
                int nano = timeStamp.nanoSeconds;
                long milliPastEpoch = nano/1000000 + secondPastEpochs*1000;
                Date date = new Date(milliPastEpoch);
                text.append(String.format(" = %tF %tT.%tL", date,date,date));
                return;
            }
            CDField[] cdFields = cdStructure.getCDFields();
            for(CDField cdField : cdFields) {
                if(cdField==null)continue;
                int maxNumPuts = cdField.getMaxNumPuts();
                if(maxNumPuts==0 && !printAll) continue;
                PVField pvField = cdField.getPVField();
                Field field = pvField.getField();
                String fieldName = pvField.getField().getFieldName();
                if(fieldName!=null && fieldName.length()>0) {
                    newLine(indentLevel);
                    text.append(fieldName);
                }
                switch(field.getType()) {
                case pvArray: printArray(cdField,indentLevel+1,printAll); break;
                case pvStructure: printStructure((CDStructure)cdField,indentLevel+1,printAll); break;
                default: printScalar(cdField,indentLevel+1,printAll); break;
                }
            }
        }

        
        private void printArray(CDField cdField, int indentLevel,boolean printAll) {
            if(checkNumPuts(cdField)) printAll = true;
            PVArray pvArray = (PVArray)cdField.getPVField();
            
            Array array = (Array)pvArray.getField();
            Type elementType = array.getElementType();
            if(elementType.isScalar()) {
                text.append(String.format(
                        " = %s",
                        pvArray.toString(indentLevel+1)));
                return;
            }
            if(elementType==Type.pvArray) {
                CDArrayArray cdArrayArray = (CDArrayArray)cdField;
                CDArray[] cdFields = cdArrayArray.getElementCDArrays();
                for(CDArray elementCDField : cdFields) {
                    if(elementCDField==null) continue;
                    int maxNumPuts = elementCDField.getMaxNumPuts();
                    if(maxNumPuts==0 && !printAll) continue;
                    newLine(indentLevel);
                    text.append(elementCDField.getPVField().getField().getFieldName());
                    printArray(elementCDField,indentLevel+1,printAll);
                }
            } else if(elementType==Type.pvStructure) {
                CDStructureArray cdStructureArray = (CDStructureArray)cdField;
                CDStructure[] cdFields = cdStructureArray.getElementCDStructures();
                for(CDStructure elementCDField : cdFields) {
                    if(elementCDField==null) continue;
                    int maxNumPuts = elementCDField.getMaxNumPuts();
                    if(maxNumPuts==0 && !printAll) continue;
                    newLine(indentLevel);
                    text.append(elementCDField.getPVField().getField().getFieldName());
                    printStructure(elementCDField,indentLevel+1,printAll);
                }
            }
        }
        
        private void printScalar(CDField cdField, int indentLevel,boolean printAll) {
            if(checkNumPuts(cdField)) printAll = true;
            if(!printAll) return;
            text.append(String.format(
                " = %s",
                cdField.getPVField().toString(indentLevel+1)));
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
