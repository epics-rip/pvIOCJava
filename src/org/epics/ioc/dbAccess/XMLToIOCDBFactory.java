/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
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
    private static AtomicBoolean isInUse = new AtomicBoolean(false);
//  for use by private classes
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");
    private static Pattern stringPattern = Pattern.compile("\\s*,\\s*");
    private static DBD dbd;
    private static IOCDB iocdb;
    private static IOCDB iocdbMaster;
    private static MessageListener iocMessageListener;
    private static IOCXMLReader iocxmlReader;
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbdin the reflection database.
     * @param iocdbin IOC database.
     * @param fileName The name of the file containing xml record instance definitions.
     */
    public static void convert(DBD dbdin, IOCDB iocdbin, String fileName,MessageListener messageListener)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            messageListener.message("XMLToIOCDBFactory.convert is already active",MessageType.fatalError);
        }
        try {
            dbd = dbdin;
            iocdb = iocdbin;
            iocdbMaster = null;
            iocMessageListener = messageListener;
            IOCXMLListener listener = new Listener();
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("IOCDatabase",fileName,listener);
        } finally {
            isInUse.set(false);
        }
        
    }
    
    /**
     * Create an IOC Database (IOCDB) and populate it
     * with definitions from an XML record instance.
     * @param iocdbName The name for the IOC Database
     * The definitions are not added to the master IOCDB but the caller can call IOCDB.mergeIntoMaster
     * to add them to master.
     * The DBD database is named master.
     * Attempting to add definitions for a record instance that is already in master is an error.
     * @param iocdbName The name to be given to the newly created IOCDB.
     * @param fileName The file containing record instances definitions.
     * @param messageListener A listener for error messages.
     * @return An IOC Database that has the newly created record instances.
     */
    public static IOCDB convert(String iocdbName,String fileName,MessageListener messageListener) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            messageListener.message("XMLToIOCDBFactory is already active", MessageType.error);
            return null;
        }
        try {
            iocdb =IOCDBFactory.create(iocdbName);           
            dbd = DBDFactory.getMasterDBD();
            iocdbMaster = IOCDBFactory.getMaster();
            iocMessageListener = messageListener;
            IOCXMLListener listener = new Listener();
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("IOCDatabase",fileName,listener);
            return iocdb;
            
        } finally {
            isInUse.set(false);
        }
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
                    iocxmlReader.message(
                        "startElement " + qName + " not understood",
                        MessageType.error);
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
                iocxmlReader.message(
                            "endElement element " + qName + " not understood",
                            MessageType.error);
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
        
        public void message(String message,MessageType messageType) {
            iocMessageListener.message(message, messageType);
        }
    }

    private static class RecordHandler
    {
        enum State {idle, structure, field, enumerated, array}
        private State state = State.idle;
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
                iocxmlReader.message(
                    "attribute name not specified",
                    MessageType.error);
                state = State.idle;
                return;
            }
            if(recordTypeName==null) {
                iocxmlReader.message(
                    "attribute type not specified",
                    MessageType.error);
                state = State.idle;
                return;
            }
            DBDRecordType dbdRecordType = dbd.getRecordType(recordTypeName);
            if(dbdRecordType==null) {
                iocxmlReader.message(
                    "record type " + recordTypeName + " does not exist.",
                    MessageType.warning);
                state = State.idle;
                return;
            }
            if(iocdbMaster!=null && iocdbMaster!=iocdb) {
                if(iocdbMaster.findRecord(recordName)!=null) {
                    iocxmlReader.message(
                            "record " + recordName + " already present in master",
                            MessageType.warning);
                    state = State.idle;
                    return;
                }
            }
            dbRecord = iocdb.findRecord(recordName);
            if(dbRecord==null) {
                DBRecord record = FieldDataFactory.createRecord(recordName,dbdRecordType);
                boolean result = iocdb.addRecord(record);
                if(!result) {
                    iocxmlReader.message(
                            "failed to create record " + recordTypeName,
                            MessageType.warning);
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
                iocxmlReader.message(
                        "record  " + recordName + " has no record support",
                        MessageType.error);
            } else {
                String error = dbRecord.setSupportName(supportName);
                if(error!=null) {
                    iocxmlReader.message(
                            "record  " + recordName
                            + " " + error,
                            MessageType.error);
                }
            }
            structureState.dbStructure = dbRecord;
            state = State.structure;
        }
        
        void startElement(String qName, Map<String,String> attributes) {
            switch(state) {
            case idle: 
                idleStack.push(idleState);
                idleState = new IdleState();
                idleState.prevState  = state;
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
                    iocxmlReader.message(
                            "Logic error: qName " + qName
                            + " but expected " + structureState.fieldName,
                            MessageType.error);
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
                iocxmlReader.message(
                    "fieldName " + qName + " not found",
                    MessageType.error);
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
                    iocxmlReader.message(
                            "fieldName " + qName + " setSupportName failed "
                            + error,
                            MessageType.error);
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
                        iocxmlReader.message(
                                "fieldName " + qName + " illegal type ???",
                                MessageType.error);
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
                            iocxmlReader.message(
                                    "structureName " + structureName + " not defined",
                                    MessageType.error);
                            idleState.prevState = state;
                            state = State.idle;
                            return;
                        }
                        DBStructure structure = (DBStructure)dbData;
                        if(!structure.createFields(dbdStructure)) {
                            iocxmlReader.message(
                                "structureName " + structureName
                                + " not used because a structure was already defined",
                                MessageType.warning);
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
                            iocxmlReader.message(
                                    "fieldName " + qName + " setSupportName failed "
                                    + error,
                                    MessageType.error);
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
                            iocxmlReader.message(e.toString(),
                                    MessageType.warning);
                        }
                    }
                    return;
                } else {
                    iocxmlReader.message(
                            " Logic Error endField illegal type ???",
                            MessageType.error);
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
                iocxmlReader.message(
                    "menu value " + value + " is not a valid choice",
                    MessageType.error);
                return;
            } else if(dbType==DBType.dbLink) {
                return;
            }
            iocxmlReader.message(
                "Logic error in endField",
                MessageType.error);
        }
        
        private void startEnumElement(String qName, Map<String,String> attributes) {
            if(!qName.equals("choice")) {
                iocxmlReader.message(
                    qName + " illegal. Only choice is valid.",
                    MessageType.error);
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
                iocxmlReader.message(
                    value + " is not a valid choice",
                    MessageType.warning);
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
                iocxmlReader.message(
                        "no support is defined",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBDSupport support = dbd.getSupport(supportName);
            if(support==null) {
                iocxmlReader.message(
                        "support " + supportName + " not defined",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String configurationStructureName = support.getConfigurationStructureName();
            if(configurationStructureName==null) {
                iocxmlReader.message(
                        "support " + supportName + " does not define a configurationStructureName",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String structureName = attributes.get("structureName");
            if(structureName==null) {
                iocxmlReader.message(
                        "structureName was not specified",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            if(!structureName.equals(configurationStructureName)) {
                iocxmlReader.message(
                        "structureName was not specified",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return; 
            }
            String error = dbData.setSupportName(supportName);
            if(error!=null) {
                iocxmlReader.message(error,
                        MessageType.error);
                return;
            }
            DBStructure dbStructure = dbData.getConfigurationStructure();
            if(dbStructure==null) {
                iocxmlReader.message(
                        "support " + supportName + " does not use a configuration structure",
                        MessageType.error);
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
                    iocxmlReader.message(error,
                            MessageType.error);
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
                    iocxmlReader.message(
                            " Logic error ArrayHandler",
                            MessageType.error);
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
                iocxmlReader.message(
                        "arrayStartElement Logic error: expected value",
                        MessageType.error);
            }
            String offset = attributes.get("offset");
            if(offset!=null) arrayState.arrayOffset = Integer.parseInt(offset);
            int arrayOffset = arrayState.arrayOffset;
            DBArray dbArray = arrayState.dbArray;
            String fieldName = "value";
            String actualFieldName = "[" + String.format("%d",arrayOffset) + "]";
            DBDFieldAttribute dbdFieldAttribute = DBDFieldFactory.createFieldAttribute(attributes);
            DBDField dbdField;
            switch(arrayState.arrayElementDBType) {
            case dbPvType:{
                    Type arrayElementType = arrayState.arrayElementType;
                    if(arrayElementType.isScalar()) {
                        stringBuilder.setLength(0);
                        return;
                    }
                    if(arrayElementType!=Type.pvEnum) {
                        iocxmlReader.message(
                                "fieldName " + qName + " illegal type ???",
                                MessageType.error);
                        idleState.prevState = state;
                        state = State.idle;
                        return;
                    }
                    dbdField = DBDFieldFactory.createField(actualFieldName, null,dbdFieldAttribute,
                            arrayState.arrayElementType, arrayState.arrayElementDBType);
                    DBEnumArray dbEnumArray = arrayState.dbEnumArray;
                    DBEnum[] enumData = arrayState.enumData;
                    enumData[0] = (DBEnum)FieldDataFactory.createEnumData(
                        dbArray,dbdField,null);
                    dbEnumArray.put(arrayOffset,1,enumData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = enumData[0].setSupportName(supportName);
                        if(error!=null) {
                            iocxmlReader.message(error,
                                    MessageType.error);
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
                        iocxmlReader.message(
                                "menuName not given",
                                MessageType.warning);
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    dbdField = DBDFieldFactory.createMenuField(actualFieldName,null,
                            dbdFieldAttribute, dbd.getMenu(menuName));
                    DBMenuArray dbMenuArray = arrayState.dbMenuArray;
                    DBMenu[] menuData = arrayState.menuData;
                    menuData[0] = (DBMenu)FieldDataFactory.createData(dbArray,dbdField);
                    dbMenuArray.put(arrayOffset,1,menuData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = menuData[0].setSupportName(supportName);
                        if(error!=null) {
                            iocxmlReader.message(error,MessageType.error);
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
                        iocxmlReader.message(
                                "structureName not given",
                                MessageType.warning);
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDStructure structure = dbd.getStructure(structureName);
                    if(structure==null) {
                        iocxmlReader.message(
                                "structureName not found",
                                MessageType.warning);
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    dbdField = DBDFieldFactory.createStructureField(actualFieldName,
                            structure.getPropertys(),
                            dbdFieldAttribute,dbd.getStructure(structureName));
                    DBStructureArray dbStructureArray = arrayState.dbStructureArray;
                    DBStructure[] structureData = arrayState.structureData;
                    structureData[0] = (DBStructure)FieldDataFactory.createData(dbArray,dbdField);
                    dbStructureArray.put(arrayOffset,1,structureData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        String error = structureData[0].setSupportName(supportName);
                        if(error!=null) {
                            iocxmlReader.message(
                                    error,
                                    MessageType.error);
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
                        iocxmlReader.message(
                                "elementType not given",
                                MessageType.warning);
                        state = State.idle;
                    }
                    dbdField = DBDFieldFactory.createArrayField(actualFieldName, null,dbdFieldAttribute,
                            DBDFieldFactory.getType(elementType),DBDFieldFactory.getDBType(elementType));
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
                            iocxmlReader.message(
                                    error,
                                    MessageType.error);
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
                       dbdField = DBDFieldFactory.createField(actualFieldName, null,dbdFieldAttribute,
                               arrayState.arrayElementType, arrayState.arrayElementDBType);
                       DBLinkArray dbLinkArray = arrayState.dbLinkArray;
                       DBLink[] linkData = arrayState.linkData;
                       linkData[0] = (DBLink)FieldDataFactory.createData(
                           dbArray,dbdField);
                       dbLinkArray.put(arrayOffset,1,linkData,0);
                       String supportName = attributes.get("supportName");
                       if(supportName!=null) {
                           String error = linkData[0].setSupportName(supportName);
                           if(error!=null) {
                               iocxmlReader.message(
                                       error,
                                       MessageType.error);
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
                iocxmlReader.message(
                        "arrayEndElement Logic error: expected value",
                        MessageType.error);
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
                    iocxmlReader.message(
                        "illegal array element type "
                        + arrayElementType.toString(),
                        MessageType.warning);
                    return;
                }
                try {
                    int num = convert.fromStringArray(
                        dbArray,arrayOffset,values.length,values,0);
                    arrayOffset += values.length;
                    if(values.length!=num) {
                        iocxmlReader.message(
                            "not all values were written",
                            MessageType.warning);
                    }
                } catch (NumberFormatException e) {
                    iocxmlReader.message(
                        e.toString(),
                        MessageType.warning);
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
        
}
