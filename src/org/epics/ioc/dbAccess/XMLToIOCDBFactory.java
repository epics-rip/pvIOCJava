/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.*;
import java.util.regex.Pattern;
import java.net.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;


/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToIOCDBFactory {
//  for use by private classes
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");
    private static Pattern stringPattern = Pattern.compile("\\s*,\\s*");
    private static DBD dbd;
    private static IOCDB iocdb;
    private static IOCXMLReader errorHandler;
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbdin the reflection database.
     * @param iocdbin IOC database.
     * @param fileName The name of the file containing xml record instance definitions.
     * @throws MalformedURLException If SAX throws it.
     * @throws IllegalStateException If any errors were detected.
     */
    public static void convert(DBD dbdin, IOCDB iocdbin, String fileName)
        throws IllegalStateException
    {
        dbd = dbdin;
        iocdb = iocdbin;
        IOCXMLListener listener = new Listener();
        errorHandler = IOCXMLReaderFactory.getReader();
        IOCXMLReaderFactory.create("IOCDatabase",fileName,listener);
        
    }
    
    private static class Listener implements IOCXMLListener
    {
        private enum State {
            idle,
            record,
        } 
        private State state = State.idle;
        private RecordHandler  recordHandler = null;
 
        public void endDocument() {}       

        public void startElement(String qName,Map<String,String> attributes)
        {
            switch(state) {
            case idle:
                if(qName.equals("record")) {
                    recordHandler = new RecordHandler(qName,attributes);
                    state = State.record;
                } else {
                    errorHandler.errorMessage(
                        "startElement " + qName + " not understood");
                }
                break;
            case record: 
                recordHandler.startElement(qName,attributes);
                break;
            }
        }
        
        public void endElement(String qName)
        {
            switch(state) {
            case idle:
                errorHandler.errorMessage(
                            "endElement element " + qName + " not understood");
                break;
            case record: 
                if(qName.equals("record")) {
                    state = State.idle;
                } else {
                    recordHandler.endElement(qName);
                }
                break;
            }
        }
        
        public void characters(char[] ch, int start, int length)
        {
            switch(state) {
            case idle:
                break;
            case record: 
                recordHandler.characters(ch,start,length);
                break;
            }
        }
        
    }

    private static class RecordHandler
    {
        enum State {idle, structure, field, enumerated, array}
        private State state;
        private StringBuilder stringBuilder = new StringBuilder();
        private DBRecord dbRecord = null;
        
        private static class IdleState {
            private State prevState = null;
        }
        private IdleState idleState = new IdleState();
        private Stack<IdleState> idleStack = new Stack<IdleState>();
        
        private static class FieldState {
            private State prevState = null;
            private DBData dbData = null;
        }
        private FieldState fieldState = new FieldState();
        private Stack<FieldState> fieldStack = new Stack<FieldState>();
        
        private static class StructureState {
            private State prevState = null;
            private String fieldName = null;
            private DBStructure dbStructure = null;
        }
        private StructureState structureState = new StructureState();
        private Stack<StructureState> structureStack = new Stack<StructureState>();
        
        private static class EnumState {
            private State prevState = null;
            private DBEnum dbEnum = null;
            private String fieldName = null;
            private LinkedList<String> enumChoiceList = new LinkedList<String>();
            private String value = "";
        }
        private EnumState enumState = new EnumState();
        private Stack<EnumState> enumStack = new Stack<EnumState>();
        
        private static class ArrayState {
            private State prevState = null;
            private String fieldName = null;
            private DBArray dbArray = null;
            private Type arrayElementType = null;
            private DBType arrayElementDBType = null;
            private int arrayOffset = 0;
            
            private DBEnum[] enumData = null;
            private DBEnumArray dbEnumArray = null;
            
            private DBMenu[] menuData = null;
            private DBMenuArray dbMenuArray = null;

            private DBStructure[] structureData = null;
            private DBStructureArray dbStructureArray = null;

            private DBArray[] arrayData = null;
            private DBArrayArray dbArrayArray = null;

            private DBLink[] linkData = null;
            private DBLinkArray dbLinkArray = null;
        }
        private ArrayState arrayState = new ArrayState();
        private Stack<ArrayState> arrayStack = new Stack<ArrayState>();
        
        RecordHandler(String qName, Map<String,String> attributes) {
            String recordName = attributes.get("name");
            String recordTypeName = attributes.get("type");
            if(recordName==null) {
                errorHandler.errorMessage(
                    "attribute name not specified");
                state = State.idle;
                return;
            }
            if(recordTypeName==null) {
                errorHandler.errorMessage(
                    "attribute type not specified");
                state = State.idle;
                return;
            }
            DBDRecordType dbdRecordType = dbd.getRecordType(recordTypeName);
            if(dbdRecordType==null) {
                errorHandler.warningMessage(
                    "record type " + recordTypeName + " does not exist.");
                state = State.idle;
                return;
            }
            dbRecord = iocdb.findRecord(recordName);
            if(dbRecord==null) {
                boolean result = iocdb.createRecord(recordName,dbdRecordType);
                if(!result) {
                    errorHandler.warningMessage(
                            "failed to create record " + recordTypeName);
                    state = State.idle;
                    return;
                }
                dbRecord = iocdb.findRecord(recordName);
                dbRecord.setDBD(dbd);
            }
            String supportName = attributes.get("supportName");
            if(supportName==null) {
                supportName = dbRecord.getSupportName();
            }
            if(supportName==null) {
                supportName = dbdRecordType.getSupportName();
            }
            if(supportName==null) {
                errorHandler.errorMessage(
                        "record  " + recordName + " has no record support");
            } else {
                String error = dbRecord.setSupportName(supportName);
                if(error!=null) {
                    errorHandler.errorMessage(
                            "record  " + recordName
                            + " " + error);
                }
            }
            structureState.dbStructure = dbRecord;
            state = State.structure;
        }
        
        void startElement(String qName, Map<String,String> attributes) {
            switch(state) {
            case idle: 
                idleState.prevState = state;
                state = State.idle;
                idleStack.push(idleState);
                idleState = new IdleState();
                break;
            case structure:
                if(qName.equals("configure")) {
                    supportStart(structureState.dbStructure,attributes);
                    break;
                }
                startStructureElement(qName,attributes);
                break;
            case field:
                if(qName.equals("configure")) {
                    supportStart(fieldState.dbData,attributes);
                    break;
                }
                // just ignore any additional xml elements
                idleState.prevState = state;
                state = State.idle;
                break;
            case enumerated:
                if(qName.equals("configure")) {
                    supportStart(enumState.dbEnum,attributes);
                    break;
                }
                startEnumElement(qName,attributes);
                break;
            case array:
                if(qName.equals("configure")) {
                    supportStart(arrayState.dbArray,attributes);
                    break;
                }
                startArrayElement(qName,attributes);
                break;
            }
        }
        
        void characters(char[] ch, int start, int length)  {
            switch(state) {
            case idle: break;
            case structure: break;
            case array:
            case enumerated:
            case field:
                while(start<ch.length && length>0
                && Character.isWhitespace(ch[start])) {
                    start++; length--;
                }
                while(length>0 && Character.isWhitespace(ch[start+ length-1])) {
                    length--;
                }
                if(length<=0) break;
                stringBuilder.append(ch,start,length);
                break;
            }
        }
        
        void endElement(String qName)  {
            switch(state) {
            case idle: 
                state = idleState.prevState;
                if(state==State.idle) {
                    idleState = idleStack.pop();
                }
                break;
            case structure: 
                if(!qName.equals(structureState.fieldName)) {
                    errorHandler.errorMessage(
                            "Logic error: qName " + qName
                            + " but expected " + structureState.fieldName);
                }
                if(qName.equals("configure")) {
                    supportEnd();
                    break;
                }
                state = structureState.prevState;
                if(state==State.structure) {
                    structureState = structureStack.pop();
                }
                if(state==State.array) {
                    endArrayElement(qName);
                }
                break;
            case field:
                endField();
                state = fieldState.prevState;
                if(state==State.array) {
                    endArrayElement(qName);
                }
                break;
            case enumerated:
                if(qName.equals(enumState.fieldName)) {
                    endEnum();
                    state = enumState.prevState;
                } else {
                    endEnumElement(qName);
                }
                break;
            case array:
                if(qName.equals(arrayState.fieldName)) {
                    state = arrayState.prevState;
                    if(state==State.array) {
                        arrayState = arrayStack.pop();
                    }
                } else {
                    endArrayElement(qName);
                }
                break;
            }
        }
        
        private void startStructureElement(String qName, Map<String,String> attributes) {
            DBStructure dbStructure = structureState.dbStructure;
            int dbDataIndex = dbStructure.getFieldDBDataIndex(qName);
            if(dbDataIndex<0) {
                errorHandler.errorMessage(
                    "fieldName " + qName + " not found");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBData dbData = dbStructure.getFieldDBDatas()[dbDataIndex];
            String supportName = attributes.get("supportName");
            if(supportName==null) {
                DBDField dbdField = dbData.getDBDField();
                supportName = dbdField.getSupportName();
            }
            if(supportName!=null) {
                String error = dbData.setSupportName(supportName);
                if(error!=null) {
                    errorHandler.errorMessage(
                            "fieldName " + qName + " setSupportName failed "
                            + error);
                }
            }
            DBDField dbdField = dbData.getDBDField();
            DBType dbType = dbdField.getDBType();
            switch(dbType) {
            case dbPvType: {
                    Type type= dbdField.getType();
                    if(type.isScalar()) {
                        fieldState.prevState = state;
                        fieldState.dbData = dbData;
                        state = State.field;
                        stringBuilder.setLength(0);
                        return;
                    }
                    if(type!=Type.pvEnum) {
                        errorHandler.errorMessage(
                                "fieldName " + qName + " illegal type ???");
                        idleState.prevState = state;
                        state = State.idle;
                        return;
                    }
                    enumState.prevState = state;
                    enumState.dbEnum = (DBEnum)dbData;
                    enumState.fieldName = qName;
                    enumState.value = "";
                    enumState.enumChoiceList.clear();
                    state = State.enumerated;
                    return;
                }
            case dbMenu:
                fieldState.prevState = state;
                fieldState.dbData = dbData;
                state = State.field;
                stringBuilder.setLength(0);
                return;
            case dbStructure: {
                    String structureName = attributes.get("structureName");
                    if(structureName!=null) {
                        DBDStructure dbdStructure = dbd.getStructure(structureName);
                        if(dbdStructure==null) {
                            errorHandler.errorMessage(
                                    "structureName " + structureName + " not defined");
                            idleState.prevState = state;
                            state = State.idle;
                            return;
                        }
                        DBStructure fieldStructure = (DBStructure)dbData;
                        if(!fieldStructure.createFields(dbdStructure)) {
                            errorHandler.warningMessage(
                                "structureName " + structureName
                                + " not used because a structure was already defined");
                        } 
                    }
                }
                structureStack.push(structureState);
                structureState = new StructureState();
                structureState.prevState = state;
                structureState.dbStructure = (DBStructure)dbData;
                structureState.fieldName = qName;
                state = State.structure;
                supportName = dbData.getSupportName();
                if(supportName==null) {
                    supportName = structureState.dbStructure.getDBDStructure().getSupportName();
                    if(supportName!=null) {
                        String error = dbData.setSupportName(supportName);
                        if(error!=null) {
                            errorHandler.errorMessage(
                                    "fieldName " + qName + " setSupportName failed "
                                    + error);
                        }
                    }
                }
                return;
            case dbArray:
                arrayState.dbArray = (DBArray)dbData;
                arrayState.prevState = state;
                arrayState.fieldName = qName;
                state = State.array;
                arrayStart(attributes);
                return;
            case dbLink:
                fieldState.prevState = state;
                fieldState.dbData = dbData;
                state = State.field;
                return;
            }
        }
        private void endField()  {
            String value = stringBuilder.toString();
            stringBuilder.setLength(0);
            DBData dbData = fieldState.dbData;
            DBDField dbdField = dbData.getDBDField();
            DBType dbType = dbdField.getDBType();
            if(dbType==DBType.dbPvType) {
                Type type= dbdField.getType();
                if(type.isScalar()) {
                    if(value!=null) {
                        try {
                            convert.fromString(dbData, value);
                        } catch (NumberFormatException e) {
                            errorHandler.warningMessage(e.toString());
                        }
                    }
                    return;
                } else {
                    errorHandler.errorMessage(
                            " Logic Error endField illegal type ???");
                }
                return;
            } else if(dbType==DBType.dbMenu) {
                DBMenu menu = (DBMenu)dbData;
                String[] choice = menu.getChoices();
                for(int i=0; i<choice.length; i++) {
                    if(value.equals(choice[i])) {
                        menu.setIndex(i);
                        return;
                    }
                }
                errorHandler.errorMessage(
                    "menu value " + value + " is not a valid choice");
                return;
            } else if(dbType==DBType.dbLink) {
                return;
            }
            errorHandler.errorMessage(
                "Logic error in endField");
        }
        
        private void startEnumElement(String qName, Map<String,String> attributes) {
            if(!qName.equals("choice")) {
                errorHandler.errorMessage(
                    qName + " illegal. Only choice is valid.");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            if(stringBuilder.length()>0) {
                enumState.value +=  stringBuilder.toString();
                stringBuilder.setLength(0);
            }
        }
        private void endEnum() {
            LinkedList<String> enumChoiceList = enumState.enumChoiceList;
            DBEnum dbEnum = enumState.dbEnum;
            String value = enumState.value;
            if(enumChoiceList.size()==0) return;
            String[] choice = new String[enumChoiceList.size()];
            for(int i=0; i< choice.length; i++) {
                choice[i] = enumChoiceList.removeFirst();
            }
            dbEnum.setChoices(choice);
            if(stringBuilder.length()>0) {
                value = value + stringBuilder.toString();
                stringBuilder.setLength(0);
            }
            if(value==null) return;
            for(int i=0; i< choice.length; i++) {
                if(value.equals(choice[i])) {
                    dbEnum.setIndex(i);
                    return;
                }
            }
            if(value!=null) {
                errorHandler.warningMessage(
                    value + " is not a valid choice");
            }
            return;
        }
        
        private void endEnumElement(String qName) {
            enumState.enumChoiceList.add(stringBuilder.toString());
            stringBuilder.setLength(0);
        }
        
        private void supportStart(DBData dbData, Map<String,String> attributes) {
            String supportName = dbData.getSupportName();
            if(supportName==null) {
                errorHandler.errorMessage(
                        "no support is defined");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBDSupport support = dbd.getSupport(supportName);
            if(support==null) {
                errorHandler.errorMessage(
                        "support " + supportName + " not defined");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String configurationStructureName = support.getConfigurationStructureName();
            if(configurationStructureName==null) {
                errorHandler.errorMessage(
                        "support " + supportName + " does not define a configurationStructureName");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String structureName = attributes.get("structureName");
            if(structureName==null) {
                errorHandler.errorMessage(
                        "structureName was not specified");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            if(!structureName.equals(configurationStructureName)) {
                errorHandler.errorMessage(
                        "structureName was not specified");
                idleState.prevState = state;
                state = State.idle;
                return; 
            }
            String error = dbData.setSupportName(supportName);
            if(error!=null) {
                errorHandler.errorMessage(error);
                return;
            }
            DBStructure dbStructure = dbData.getConfigurationStructure();
            if(dbStructure==null) {
                errorHandler.errorMessage(
                        "support " + supportName + " does not use a configuration structure");
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            supportName = null;
            switch(state) {
            case field:
                fieldStack.push(fieldState);
                fieldState = new FieldState();
                fieldState.prevState = state;
                break;
            case enumerated:
                enumStack.push(enumState);
                enumState = new EnumState();
                enumState.prevState = state;
                break;
            case array:
                arrayStack.push(arrayState);
                arrayState = new ArrayState();
                arrayState.prevState = state;
                break;
            default:
                break;
            }
            structureStack.push(structureState);
            structureState = new StructureState();
            structureState.prevState = state;
            structureState.dbStructure = dbStructure;
            state = State.structure;
            structureState.fieldName = "configure";
        }
        private void supportEnd() {
            State prevState = structureState.prevState;
            structureState = structureStack.pop();
            state = prevState;
            switch(state) {
            case field:
                fieldState = fieldStack.pop();
                break;
            case enumerated:
                enumState = enumStack.pop();
                break;
            case array:
                arrayState = arrayStack.pop();
                break;
            default:
                break;
            }
        }
        private void arrayStart(Map<String,String> attributes)  {
            DBArray dbArray= arrayState.dbArray;
            DBDArrayField dbdArrayField = (DBDArrayField)dbArray.getDBDField();
            Type arrayElementType = dbdArrayField.getElementType();
            DBType arrayElementDBType = dbdArrayField.getElementDBType();
            arrayState.arrayOffset = 0;
            arrayState.arrayElementType = arrayElementType;
            arrayState.arrayElementDBType = arrayElementDBType;
            String supportName = attributes.get("supportName");
            if(supportName!=null) {
                String error = dbArray.setSupportName(supportName);
                if(error!=null) {
                    errorHandler.errorMessage(error);
                }
                
            }
            String value = attributes.get("capacity");
            if(value!=null) dbArray.setCapacity(Integer.parseInt(value));
            value = attributes.get("length");
            if(value!=null) dbArray.setLength(Integer.parseInt(value));
            switch (arrayElementDBType) {
            case dbPvType:
                if (arrayElementType.isScalar()) {
                } else if (arrayElementType == Type.pvEnum) {
                    arrayState.enumData = new DBEnum[1];
                    arrayState.dbEnumArray = (DBEnumArray)dbArray;
                } else {
                    errorHandler.errorMessage(
                            " Logic error ArrayHandler");
                }
                break;
            case dbMenu:
                arrayState.menuData = new DBMenu[1];
                arrayState.dbMenuArray = (DBMenuArray)dbArray;
                break;
            case dbStructure:
                arrayState.structureData = new DBStructure[1];
                arrayState.dbStructureArray = (DBStructureArray)dbArray;
                break;
            case dbArray:
                arrayState.arrayData = new DBArray[1];
                arrayState.dbArrayArray = (DBArrayArray)dbArray;
                break;
            case dbLink:
                arrayState.linkData = new DBLink[1];
                arrayState.dbLinkArray = (DBLinkArray)dbArray;
                break;
            }
        }
        
        private void startArrayElement(String qName, Map<String,String> attributes)  {
            if(!qName.equals("value")) {
                errorHandler.errorMessage(
                        "arrayStartElement Logic error: expected value");
            }
            String offset = attributes.get("offset");
            if(offset!=null) arrayState.arrayOffset = Integer.parseInt(offset);
            int arrayOffset = arrayState.arrayOffset;
            DBArray dbArray = arrayState.dbArray;
            String fieldName = "value";
            String actualFieldName = "[" + String.format("%d",arrayOffset) + "]";
            DBDAttribute dbdAttribute;
            DBDField dbdField;
            switch(arrayState.arrayElementDBType) {
            case dbPvType:{
                    Type arrayElementType = arrayState.arrayElementType;
                    if(arrayElementType.isScalar()) {
                        stringBuilder.setLength(0);
                        return;
                    }
                    if(arrayElementType!=Type.pvEnum) {
                        errorHandler.errorMessage(
                                "fieldName " + qName + " illegal type ???");
                        idleState.prevState = state;
                        state = State.idle;
                        return;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new EnumDBDAttributeValues(actualFieldName);
                    dbdAttribute = DBDAttributeFactory.create(dbd,dbdAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBEnumArray dbEnumArray = arrayState.dbEnumArray;
                    DBEnum[] enumData = arrayState.enumData;
                    enumData[0] = (DBEnum)FieldDataFactory.createEnumData(
                        dbArray,dbdField,null);
                    dbEnumArray.put(arrayOffset,1,enumData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = enumData[0].setSupportName(supportName);
                        if(error!=null) {
                            errorHandler.errorMessage(error);
                        }
                    }
                    enumState.prevState = state;
                    enumState.dbEnum = enumData[0];
                    enumState.fieldName = fieldName;
                    state = State.enumerated;
                    return;
                }
            case dbMenu: {
                    String menuName = attributes.get("menuName");
                    if(menuName==null) {
                        errorHandler.warningMessage(
                                "menuName not given");
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new MenuDBDAttributeValues(menuName,actualFieldName);
                    dbdAttribute = DBDAttributeFactory.create(
                        dbd,dbdAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBMenuArray dbMenuArray = arrayState.dbMenuArray;
                    DBMenu[] menuData = arrayState.menuData;
                    menuData[0] = (DBMenu)FieldDataFactory.createData(dbArray,dbdField);
                    dbMenuArray.put(arrayOffset,1,menuData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = menuData[0].setSupportName(supportName);
                        if(error!=null) {
                            errorHandler.errorMessage(
                                    error);
                        }
                    }
                    fieldState.prevState = state;
                    fieldState.dbData = menuData[0];
                    state = State.field;
                    stringBuilder.setLength(0);
                    return;
                }
            case dbStructure: {
                    String structureName = attributes.get("structureName");
                    if(structureName==null) {
                        errorHandler.warningMessage(
                                "structureName not given");
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDStructure structure = dbd.getStructure(structureName);
                    if(structure==null) {
                        errorHandler.warningMessage(
                                "structureName not found");
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new StructureDBDAttributeValues(structureName,actualFieldName);
                    dbdAttribute = DBDAttributeFactory.create(
                        dbd,dbdAttributeValues);
                    Property[] property = structure.getPropertys();
                    dbdField = DBDCreateFactory.createField(dbdAttribute,property);
                    DBStructureArray dbStructureArray = arrayState.dbStructureArray;
                    DBStructure[] structureData = arrayState.structureData;
                    structureData[0] = (DBStructure)FieldDataFactory.createData(dbArray,dbdField);
                    dbStructureArray.put(arrayOffset,1,structureData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = structureData[0].setSupportName(supportName);
                        if(error!=null) {
                            errorHandler.errorMessage(
                                    error);
                        }
                    }
                    structureStack.push(structureState);
                    structureState = new StructureState();
                    structureState.prevState = state;
                    structureState.dbStructure = structureData[0];
                    structureState.fieldName = fieldName;
                    state = State.structure;
                    return;
                }
            case dbArray: {
                    String elementType = attributes.get("elementType");
                    if(elementType==null) {
                        errorHandler.warningMessage(
                                "elementType not given");
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new ArrayDBDAttributeValues(elementType,actualFieldName);
                    dbdAttribute = DBDAttributeFactory.create(dbd,dbdAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBArrayArray dbArrayArray = arrayState.dbArrayArray;
                    DBArray[] arrayData = arrayState.arrayData;
                    arrayData[0] = (DBArray)FieldDataFactory.createData(dbArray,dbdField);
                    dbArrayArray.put(arrayOffset,1,arrayData,0);
                    String value = attributes.get("capacity");
                    if(value!=null) arrayData[0].setCapacity(Integer.parseInt(value));
                    value = attributes.get("length");
                    if(value!=null) arrayData[0].setLength(Integer.parseInt(value));
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = arrayData[0].setSupportName(supportName);
                        if(error!=null) {
                            errorHandler.errorMessage(
                                    error);
                        }
                    }
                    arrayStack.push(arrayState);
                    arrayState = new ArrayState();
                    arrayState.prevState = state;
                    arrayState.dbArray = arrayData[0];
                    arrayState.fieldName = fieldName;
                    state = State.array;
                    arrayStart(attributes);
                    return;
                }
            case dbLink: {
                       DBDAttributeValues dbdAttributeValues =
                           new LinkDBDAttributeValues(actualFieldName);
                       dbdAttribute = DBDAttributeFactory.create(dbd,dbdAttributeValues);
                       dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                       DBLinkArray dbLinkArray = arrayState.dbLinkArray;
                       DBLink[] linkData = arrayState.linkData;
                       linkData[0] = (DBLink)FieldDataFactory.createData(
                           dbArray,dbdField);
                       dbLinkArray.put(arrayOffset,1,linkData,0);
                       String supportName = attributes.get("supportName");
                       if(supportName!=null) {
                           String error = linkData[0].setSupportName(supportName);
                           if(error!=null) {
                               errorHandler.errorMessage(
                                       error);
                           }
                       }
                       fieldState.prevState = state;
                       fieldState.dbData = linkData[0];
                       state = State.field;
                       return;
                }
            }
        }
        
        private void endArrayElement(String qName)  {
            if(!qName.equals("value")) {
                errorHandler.errorMessage(
                        "arrayEndElement Logic error: expected value");
            }
            int arrayOffset = arrayState.arrayOffset;
            DBArray dbArray = arrayState.dbArray;
            switch(arrayState.arrayElementDBType) {
            case dbPvType:
                String value = stringBuilder.toString();
                String[] values = null;
                Type arrayElementType = arrayState.arrayElementType;
                if(arrayElementType.isPrimitive()) {
                    values = primitivePattern.split(value);
                } else if(arrayElementType==Type.pvString) {
                    // ignore blanks , is separator
                    values = stringPattern.split(value);
                } else {
                    errorHandler.warningMessage(
                        "illegal array element type "
                        + arrayElementType.toString());
                    return;
                }
                try {
                    int num = convert.fromStringArray(
                        dbArray,arrayOffset,values.length,values,0);
                    arrayOffset += values.length;
                    if(values.length!=num) {
                        errorHandler.warningMessage(
                            "not all values were written");
                    }
                } catch (NumberFormatException e) {
                    errorHandler.warningMessage(
                        e.toString());
                }
                arrayState.arrayOffset = arrayOffset;
                stringBuilder.setLength(0);
                break;
            case dbStructure:
                structureState = structureStack.pop();
                ++arrayState.arrayOffset;
                break;
            case dbArray:
            case dbLink:
            case dbMenu:
                ++arrayState.arrayOffset;
            }
        }
    }
    
    private static class EnumDBDAttributeValues implements DBDAttributeValues
    {   
        private static Map<String,String> attributeMap = new TreeMap<String,String>();
        
        private EnumDBDAttributeValues(String fieldName) {
            attributeMap.put("type","enum");
            attributeMap.put("name",fieldName);
        }
        public int getLength() {
            return attributeMap.size();
        }
        public String getValue(String name) {
            return attributeMap.get(name);
        }
        public Set<String> keySet() {
            return attributeMap.keySet();
        }
 
    }
    private static class LinkDBDAttributeValues implements DBDAttributeValues
    {   
        private static Map<String,String> attributeMap = new TreeMap<String,String>();
        
        private LinkDBDAttributeValues(String fieldName) {
            attributeMap.put("type","link");
            attributeMap.put("name",fieldName);
        }
        public int getLength() {
            return attributeMap.size();
        }
        public String getValue(String name) {
            return attributeMap.get(name);
        }
        public Set<String> keySet() {
            return attributeMap.keySet();
        }
    }
    
    private static class StructureDBDAttributeValues
    implements DBDAttributeValues
    {
        private static Map<String,String> attributeMap = new TreeMap<String,String>();

        public StructureDBDAttributeValues(String structureName,
        String fieldName)
        {
            attributeMap.put("type","structure");
            attributeMap.put("name",fieldName);
            attributeMap.put("structureName",structureName);
        }
        public int getLength() {
            return attributeMap.size();
        }
        public String getValue(String name) {
            return attributeMap.get(name);
        }
        public Set<String> keySet() {
            return attributeMap.keySet();
        }

    }
    
    private static class ArrayDBDAttributeValues
    implements DBDAttributeValues
    {
        private static Map<String,String> attributeMap = new TreeMap<String,String>();
        
        public ArrayDBDAttributeValues(String elementType, String fieldName) {
            attributeMap.put("type","array");
            attributeMap.put("name",fieldName);
            attributeMap.put("elementType",elementType);
        }
        public int getLength() {
            return attributeMap.size();
        }
        public String getValue(String name) {
            return attributeMap.get(name);
        }
        public Set<String> keySet() {
            return attributeMap.keySet();
        }

    }
    
    private static class MenuDBDAttributeValues
    implements DBDAttributeValues
    {
        private static Map<String,String> attributeMap = new TreeMap<String,String>();

        public MenuDBDAttributeValues(String menuName,String fieldName) {
            attributeMap.put("type","menu");
            attributeMap.put("name",fieldName);
            attributeMap.put("menuName",menuName);
        }
        public int getLength() {
            return attributeMap.size();
        }
        public String getValue(String name) {
            return attributeMap.get(name);
        }
        public Set<String> keySet() {
            return attributeMap.keySet();
        }

    }
        
}
