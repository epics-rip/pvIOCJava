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
 * @author mrk
 *
 */
public class CDRecordPrint {
    private CDRecord cdRecord;
    private Text text;
    
    public CDRecordPrint(CDRecord cdRecord,Text text) {
        this.cdRecord = cdRecord;
        this.text = text;
    }
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
        text.append(String.format(
            " supportName(%s)",
            cdField.getPVField().getSupportName()));
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
        CDField[] cdFields = cdStructure.getFieldCDFields();
        for(CDField cdField : cdFields) {
            int maxNumPuts = cdField.getMaxNumPuts();
            if(maxNumPuts==0 && !printAll) continue;
            PVField pvField = cdField.getPVField();
            Field field = pvField.getField();
            newLine(indentLevel);
            text.append(field.getFieldName());
            if(maxNumPuts>1) {
                text.append(String.format(" maxNumPuts %d",maxNumPuts));
            }
            checkNumSupportNamePuts(cdField);
            switch(field.getType()) {
            case pvEnum: printEnum((CDEnum)cdField,indentLevel+1,printAll); break;
            case pvMenu: printMenu((CDMenu)cdField,indentLevel+1,printAll); break;
            case pvLink: printLink((CDLink)cdField,indentLevel+1,printAll); break;
            case pvArray: printArray(cdField,indentLevel+1,printAll); break;
            case pvStructure: printStructure((CDStructure)cdField,indentLevel+1,printAll); break;
            default: printScalar(cdField,indentLevel+1,printAll); break;
            }
        }
    }
    private void printEnum(CDEnum cdEnum, int indentLevel,boolean printAll) {
        if(checkNumPuts(cdEnum)) printAll = true;
        PVEnum pvEnum = cdEnum.getPVEnum();
        String[] choices = pvEnum.getChoices();
        int index = pvEnum.getIndex();
        int numChoicesPut = cdEnum.getNumChoicesPut();
        if(numChoicesPut>0||printAll) {
            if(numChoicesPut>1) text.append(String.format(" numChoicesPuts %d", numChoicesPut));
            text.append(" choices = {");
            for(String choice : choices) {
                newLine(indentLevel+1);
                text.append(choice);
            }
            text.append(" }");
        }
        int numIndexPuts = cdEnum.getNumIndexPuts();
        if(numIndexPuts>0||printAll) {
            if(numIndexPuts>1) text.append(String.format(" numIndexPuts %d", numIndexPuts));
            text.append(String.format(" %s",choices[index]));
        }
    }
    private void printMenu(CDMenu cdMenu, int indentLevel,boolean printAll) {
        if(checkNumPuts(cdMenu)) printAll = true;
        PVMenu pvMenu = cdMenu.getPVMenu();
        String[] choices = pvMenu.getChoices();
        int index = pvMenu.getIndex();
        int numIndexPuts = cdMenu.getNumIndexPuts();
        if(numIndexPuts>0||printAll) {
            if(numIndexPuts>1) text.append(String.format(" numIndexPuts %d", numIndexPuts));
            text.append(String.format(" %s",choices[index]));
        }
    }
    private void printLink(CDLink cdLink, int indentLevel,boolean printAll) {
        if(checkNumPuts(cdLink)) printAll = true;
        int numConfigurationStructurePuts = cdLink.getNumConfigurationStructurePuts();
        if(numConfigurationStructurePuts==0 && !printAll) return;
        if(numConfigurationStructurePuts>1) {
            text.append(String.format(" numConfigurationStructurePuts %d",numConfigurationStructurePuts));
        }
        PVStructure pvStructure = cdLink.getPVLink().getConfigurationStructure();
        if(pvStructure==null) {
            text.append(" no configurationStructure");
            return;
        }
        text.append(" configurationStructure " + pvStructure.toString(indentLevel + 1));
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
        CDNonScalarArray cdNonScalarArray = (CDNonScalarArray)cdField;
        CDField[] cdFields = cdNonScalarArray.getElementCDFields();
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
            case pvEnum: printEnum((CDEnum)elementCDField,indentLevel+1,printAll); break;
            case pvMenu: printMenu((CDMenu)elementCDField,indentLevel+1,printAll); break;
            case pvLink: printLink((CDLink)elementCDField,indentLevel+1,printAll); break;
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
