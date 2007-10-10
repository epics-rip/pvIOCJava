/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Date;

import org.epics.ioc.pv.*;
import org.epics.ioc.ca.*;
import org.epics.ioc.util.*;

import org.eclipse.swt.widgets.*;

/**
 * Display all modified field in a CDRecord.
 * @author mrk
 *
 */
public class CDRecordPrint {
    private CDRecord cdRecord;
    private Text text;
    
    /**
     * Constructor.
     * @param cdRecord - The CDRecord to display.
     * @param text The SWT Text widget.
     */
    public CDRecordPrint(CDRecord cdRecord,Text text) {
        this.cdRecord = cdRecord;
        this.text = text;
    }
    /**
     * Print on the text widget all the fields of CDRecord that have been modified.
     */
    public void print() {
        text.append(cdRecord.getPVRecord().getRecordName());
        CDStructure cdStructure = cdRecord.getCDStructure();
        int maxNumPuts = cdStructure.getMaxNumPuts();
        if(maxNumPuts==0) {
            text.append(" no changes");
            return;
        }
        if(maxNumPuts>1) {
            text.append(String.format(" maxNumPuts %d",maxNumPuts));
        }
        printStructure(cdStructure,1,false);
        newLine(0);
    }
    
    private void checkNumSupportNamePuts(CDField cdField) {
        int numSupportNamePuts = cdField.getNumSupportNamePuts();
        if(numSupportNamePuts==0) return;
        if(numSupportNamePuts>1) {
                text.append(String.format(" numSupportNamePuts %d",numSupportNamePuts));
        }
        String supportName = cdField.getPVField().getSupportName();
        if(supportName==null) return;
        text.append(" supportName " + supportName);
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
            int maxNumPuts = cdField.getMaxNumPuts();
            if(maxNumPuts==0 && !printAll) continue;
            PVField pvField = cdField.getPVField();
            Field field = pvField.getField();
            newLine(indentLevel);
            text.append(field.getFieldName());
            switch(field.getType()) {
            case pvArray: printArray(cdField,indentLevel+1,printAll); break;
            case pvStructure: printStructure((CDStructure)cdField,indentLevel+1,printAll); break;
            default: printScalar(cdField,indentLevel+1,printAll); break;
            }
            checkNumSupportNamePuts(cdField);
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
        CDStructureArray cdStructureArray = (CDStructureArray)cdField;
        CDStructure[] cdFields = cdStructureArray.getElementCDStructures();
        for(CDField elementCDField : cdFields) {
            if(elementCDField==null) continue;
            int maxNumPuts = elementCDField.getMaxNumPuts();
            if(maxNumPuts==0 && !printAll) continue;
            PVField pvField = elementCDField.getPVField();
            Field field = pvField.getField();
            newLine(indentLevel);
            text.append(field.getFieldName());
            if(maxNumPuts>1) {
                text.append(String.format(" maxNumPuts %d",maxNumPuts));
            }
            switch(elementType) {
            case pvArray: printArray(elementCDField,indentLevel+1,printAll); break;
            case pvStructure: printStructure((CDStructure)elementCDField,indentLevel+1,printAll); break;
            default:
                throw new IllegalStateException("Logic error.");
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
