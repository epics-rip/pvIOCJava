/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;


/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToIOCDBFactory {
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static AtomicBoolean isInUse = new AtomicBoolean(false);
    //  for use by private classes
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");
    private static Pattern stringPattern = Pattern.compile("\\s*,\\s*");
    private static DBD dbd;
    private static IOCDB iocdb;
    private static IOCDB iocdbMaster;
    private static Requester requester;
    private static IOCXMLReader iocxmlReader;
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbdin the reflection database.
     * @param iocdbin IOC database.
     * @param fileName The name of the file containing xml record instance definitions.
     * @param requester The requester.
     */
    public static void convert(DBD dbdin, IOCDB iocdbin, String fileName,Requester requester)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToIOCDBFactory.convert is already active",MessageType.fatalError);
        }
        try {
            dbd = dbdin;
            iocdb = iocdbin;
            iocdbMaster = null;
            XMLToIOCDBFactory.requester = requester;
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
     * @param fileName The file containing record instances definitions.
     * @param messageListener A listener for error messages.
     * @return An IOC Database that has the newly created record instances.
     */
    public static IOCDB convert(String iocdbName,String fileName,Requester messageListener) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            messageListener.message("XMLToIOCDBFactory is already active", MessageType.error);
            return null;
        }
        try {
            iocdb =IOCDBFactory.create(iocdbName);           
            dbd = DBDFactory.getMasterDBD();
            iocdbMaster = IOCDBFactory.getMaster();
            requester = messageListener;
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
                    recordHandler.endRecord();
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
            requester.message(message, messageType);
        }
    }

    private static class RecordHandler
    {
        enum State {idle, structure, field, enumerated, array}
        private State state = State.idle;
        private StringBuilder stringBuilder = new StringBuilder();
        private PVRecord pvRecord = null;
        private DBRecord dbRecord = null;
        
        private static class IdleState {
            private State prevState = null;
        }
        private IdleState idleState = new IdleState();
        private Stack<IdleState> idleStack = new Stack<IdleState>();
        
        private static class FieldState {
            private State prevState = null;
            private PVField pvField = null;
        }
        private FieldState fieldState = new FieldState();
        private Stack<FieldState> fieldStack = new Stack<FieldState>();
        
        private static class StructureState {
            private State prevState = null;
            private String fieldName = null;
            private PVStructure pvStructure = null;
        }
        private StructureState structureState = new StructureState();
        private Stack<StructureState> structureStack = new Stack<StructureState>();
        
        private static class EnumState {
            private State prevState = null;
            private PVEnum pvEnum = null;
            private String fieldName = null;
            private LinkedList<String> enumChoiceList = new LinkedList<String>();
            private String value = "";
        }
        private EnumState enumState = new EnumState();
        private Stack<EnumState> enumStack = new Stack<EnumState>();
        
        private static class ArrayState {
            private boolean valueActive = false;
            private State prevState = null;
            private String fieldName = null;
            private PVArray pvArray = null;
            private Type arrayElementType = null;
            private int arrayOffset = 0;
            
            private PVEnum[] enumData = null;
            private PVEnumArray pvEnumArray = null;
            
            private PVMenu[] menuData = null;
            private PVMenuArray pvMenuArray = null;

            private PVStructure[] structureData = null;
            private PVStructureArray pvStructureArray = null;

            private PVArray[] arrayData = null;
            private PVArrayArray pvArrayArray = null;

            private PVLink[] linkData = null;
            private PVLinkArray pvLinkArray = null;
        }
        private ArrayState arrayState = new ArrayState();
        private Stack<ArrayState> arrayStack = new Stack<ArrayState>();
        
        private RecordHandler(String qName, Map<String,String> attributes) {
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
                Structure structure = dbd.getRecordType(recordTypeName);
                pvRecord = pvDataCreate.createPVRecord(recordName, structure);
            } else {
               pvRecord = dbRecord.getPVRecord();
            }
            String supportName = attributes.get("supportName");
            if(supportName==null) {
                supportName = pvRecord.getSupportName();
            }
            if(supportName==null) {
                supportName = dbdRecordType.getSupportName();
            }
            if(supportName==null) {
                iocxmlReader.message(
                        "record  " + recordName + " has no record support",
                        MessageType.error);
            } else {
                pvRecord.setSupportName(supportName);
            }
            structureState.pvStructure = pvRecord;
            state = State.structure;
        }
        
        private void endRecord() {
            if(state==State.idle) return;
            dbRecord = DBRecordFactory.create(pvRecord);
            if(!iocdb.addRecord(dbRecord)) {
                iocxmlReader.message(
                        "failed to add record to iocdb",
                        MessageType.info);
            }
            dbRecord.setIOCDB(iocdb);
            dbRecord.setDBD(dbd);
            dbRecord.getPVRecord().addRequester(iocdb);
        }
        
        private void startElement(String qName, Map<String,String> attributes) {
            switch(state) {
            case idle: 
                idleStack.push(idleState);
                idleState = new IdleState();
                idleState.prevState  = state;
                break;
            case structure:
                startStructureElement(qName,attributes);
                break;
            case field:
                if(qName.equals("configure")) {
                    linkSupportStart(fieldState.pvField,attributes);
                    break;
                }
                // just ignore any additional xml elements
                idleState.prevState = state;
                state = State.idle;
                break;
            case enumerated:
                startEnumElement(qName,attributes);
                break;
            case array:
                startArrayElement(qName,attributes);
                break;
            }
        }
        
        private void characters(char[] ch, int start, int length)  {
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
        
        private void endElement(String qName)  {
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
                    linkSupportEnd();
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
                if(!arrayState.valueActive && qName.equals(arrayState.fieldName)) {
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
            PVStructure pvStructure = structureState.pvStructure;
            Structure structure = (Structure)pvStructure.getField();
            int fieldIndex = structure.getFieldIndex(qName);
            if(fieldIndex<0) {
                iocxmlReader.message(
                    "fieldName " + qName + " not found",
                    MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            PVField pvField = pvStructure.getFieldPVFields()[fieldIndex];
            String supportName = attributes.get("supportName");
            if(supportName!=null) {
                pvField.setSupportName(supportName);
            }
            Field field = pvField.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                fieldState.prevState = state;
                fieldState.pvField = pvField;
                state = State.field;
                stringBuilder.setLength(0);
                return;
            }
            switch(type) {
            case pvEnum:
                enumState.prevState = state;
                enumState.pvEnum = (PVEnum)pvField;
                enumState.fieldName = qName;
                enumState.value = "";
                enumState.enumChoiceList.clear();
                state = State.enumerated;
                return;
            case pvMenu:
                fieldState.prevState = state;
                fieldState.pvField = pvField;
                state = State.field;
                stringBuilder.setLength(0);
                return;
            case pvStructure: {
                    String structureName = attributes.get("structureName");
                    if(structureName!=null) {
                        Structure replacement = dbd.getStructure(structureName);
                        if(replacement==null) {
                            iocxmlReader.message(
                                    "structureName " + structureName + " not defined",
                                    MessageType.error);
                            idleState.prevState = state;
                            state = State.idle;
                            return;
                        }
                        if(!pvStructure.replaceStructureField(qName, replacement)) {
                            iocxmlReader.message(
                                    "structureName " + structureName + " not replaced",
                                    MessageType.error);
                            idleState.prevState = state;
                            state = State.idle;
                            return;
                        }
                        pvField = pvStructure.getFieldPVFields()[fieldIndex];
                    }
                }
                structureStack.push(structureState);
                structureState = new StructureState();
                structureState.prevState = state;
                structureState.pvStructure = (PVStructure)pvField;
                structureState.fieldName = qName;
                state = State.structure;
                if(supportName==null) {
                    Structure fieldStructure = (Structure)pvField.getField();
                    DBDStructure dbdStructure = dbd.getStructure(
                            fieldStructure.getStructureName());
                    if(dbdStructure!=null) {
                        supportName = dbdStructure.getSupportName();
                    }
                }
                if(supportName!=null) {
                    pvField.setSupportName(supportName);
                }
                return;
            case pvArray:
                arrayState.pvArray = (PVArray)pvField;
                arrayState.prevState = state;
                arrayState.fieldName = qName;
                state = State.array;
                arrayStart(attributes);
                return;
            case pvLink:
                fieldState.prevState = state;
                fieldState.pvField = pvField;
                state = State.field;
                return;
            }
            iocxmlReader.message(
                    "fieldName " + qName + " illegal type ???",
                    MessageType.error);
            idleState.prevState = state;
            state = State.idle;
        }
        private void endField()  {
            String value = stringBuilder.toString();
            stringBuilder.setLength(0);
            PVField pvField = fieldState.pvField;
            Field field = pvField.getField();
            Type type = field.getType();
           
            if(type.isScalar()) {
                if(value!=null) {
                    try {
                        convert.fromString(pvField, value);
                    } catch (NumberFormatException e) {
                        iocxmlReader.message(e.toString(),
                                MessageType.warning);
                    }
                }
                return;
            } else if(type==Type.pvMenu) {
                PVMenu menu = (PVMenu)pvField;
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
            } else if(type==Type.pvLink) {
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
            PVEnum pvEnum = enumState.pvEnum;
            String value = enumState.value;
            if(enumChoiceList.size()==0) return;
            String[] choice = new String[enumChoiceList.size()];
            for(int i=0; i< choice.length; i++) {
                choice[i] = enumChoiceList.removeFirst();
            }
            pvEnum.setChoices(choice);
            if(stringBuilder.length()>0) {
                value = value + stringBuilder.toString();
                stringBuilder.setLength(0);
            }
            if(value==null) return;
            for(int i=0; i< choice.length; i++) {
                if(value.equals(choice[i])) {
                    pvEnum.setIndex(i);
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
        
        private void linkSupportStart(PVField pvField, Map<String,String> attributes) {
            if(pvField.getField().getType()!=Type.pvLink) {
                iocxmlReader.message(
                        "only a link field can be configured",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            PVLink pvLink = (PVLink)pvField;
            String supportName = pvLink.getSupportName();
            if(supportName==null) {
                iocxmlReader.message(
                        "no support is defined",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBDLinkSupport support = dbd.getLinkSupport(supportName);
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
            Structure structure = dbd.getStructure(structureName);
            if(structure==null) {
                iocxmlReader.message(
                        "structureName not in DBD",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return; 
            }
            pvLink.setSupportName(supportName);
            PVStructure pvStructure = (PVStructure)pvDataCreate.createPVField(pvLink, structure);
            if(pvStructure==null) {
                iocxmlReader.message(
                        "support " + supportName + " does not use a configuration structure",
                        MessageType.warning);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            pvLink.setConfigurationStructure(pvStructure);
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
            structureState.pvStructure = pvStructure;
            state = State.structure;
            structureState.fieldName = "configure";
        }
        private void linkSupportEnd() {
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
            PVArray pvArray= arrayState.pvArray;
            Array array = (Array)pvArray.getField();
            Type arrayElementType = array.getElementType();
            arrayState.arrayOffset = 0;
            arrayState.arrayElementType = arrayElementType;
            String supportName = attributes.get("supportName");
            if(supportName!=null) pvArray.setSupportName(supportName);
            String value = attributes.get("capacity");
            if(value!=null) pvArray.setCapacity(Integer.parseInt(value));
            value = attributes.get("capacityMutable");
            if(value!=null) {
                pvArray.setCapacityMutable(Boolean.parseBoolean(value));
            } else {
                if(pvArray.getCapacity()>0) {
                    pvArray.setCapacityMutable(false);
                }
            }
            value = attributes.get("length");
            if(value!=null) pvArray.setLength(Integer.parseInt(value));
            if (arrayElementType.isScalar()) return;
            switch (arrayElementType) {
            case pvEnum:
                arrayState.enumData = new PVEnum[1];
                arrayState.pvEnumArray = (PVEnumArray)pvArray;
                return;
            case pvMenu:
                arrayState.menuData = new PVMenu[1];
                arrayState.pvMenuArray = (PVMenuArray)pvArray;
                return;
            case pvStructure:
                arrayState.structureData = new PVStructure[1];
                arrayState.pvStructureArray = (PVStructureArray)pvArray;
                return;
            case pvArray:
                arrayState.arrayData = new PVArray[1];
                arrayState.pvArrayArray = (PVArrayArray)pvArray;
                return;
            case pvLink:
                arrayState.linkData = new PVLink[1];
                arrayState.pvLinkArray = (PVLinkArray)pvArray;
                return;
            }
            iocxmlReader.message(" Logic error ArrayHandler",MessageType.error);
        }
        
        private void startArrayElement(String qName, Map<String,String> attributes)  {
            if(!qName.equals("value")) {
                iocxmlReader.message(
                        "arrayStartElement Logic error: expected value",
                        MessageType.error);
            }
            arrayState.valueActive = true;
            String offset = attributes.get("offset");
            if(offset!=null) arrayState.arrayOffset = Integer.parseInt(offset);
            int arrayOffset = arrayState.arrayOffset;
            PVArray pvArray = arrayState.pvArray;
            String fieldName = "value";
            String actualFieldName = "[" + String.format("%d",arrayOffset) + "]";
            Field field;
            Type arrayElementType = arrayState.arrayElementType;
            if(arrayElementType.isScalar()) {
                stringBuilder.setLength(0);
                return;
            }
            switch(arrayElementType) {
            case pvEnum : {
                    field = fieldCreate.createEnum(actualFieldName,true);
                    PVEnumArray pvEnumArray = arrayState.pvEnumArray;
                    PVEnum[] enumData = arrayState.enumData;
                    enumData[0] = (PVEnum)pvDataCreate.createPVField(pvArray,field);
                    pvEnumArray.put(arrayOffset,1,enumData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        enumData[0].setSupportName(supportName);
                    }
                    enumState.prevState = state;
                    enumState.pvEnum = enumData[0];
                    enumState.fieldName = fieldName;
                    state = State.enumerated;
                    return;
                }
            case pvMenu: {
                    String menuName = attributes.get("menuName");
                    if(menuName==null) {
                        iocxmlReader.message(
                                "menuName not given",
                                MessageType.warning);
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDMenu dbdMenu = dbd.getMenu(menuName);
                    if(dbdMenu==null) {
                        iocxmlReader.message(
                                "menuName " + menuName + " is not defined",
                                MessageType.warning);
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    field = fieldCreate.createMenu(actualFieldName,menuName,dbdMenu.getChoices());
                    PVMenuArray pvMenuArray = arrayState.pvMenuArray;
                    PVMenu[] menuData = arrayState.menuData;
                    menuData[0] = (PVMenu)pvDataCreate.createPVField(pvArray,field);
                    pvMenuArray.put(arrayOffset,1,menuData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        menuData[0].setSupportName(supportName);
                    }
                    fieldState.prevState = state;
                    fieldState.pvField = menuData[0];
                    state = State.field;
                    stringBuilder.setLength(0);
                    return;
                }
            case pvStructure: {
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
                    DBDStructure dbdStructure = dbd.getStructure(structureName);
                    field = fieldCreate.createStructure(
                        actualFieldName,
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getPropertys(),
                        dbdStructure.getFieldAttribute());
                    PVStructureArray pvStructureArray = arrayState.pvStructureArray;
                    PVStructure[] structureData = arrayState.structureData;
                    structureData[0] = (PVStructure)pvDataCreate.createPVField(pvArray,field);
                    pvStructureArray.put(arrayOffset,1,structureData,0);
                    String supportName = attributes.get("supportName");
                    if(supportName==null) {
                        supportName = dbdStructure.getSupportName();
                    }
                    if(supportName!=null) {
                        structureData[0].setSupportName(supportName);
                    }
                    structureStack.push(structureState);
                    structureState = new StructureState();
                    structureState.prevState = state;
                    structureState.pvStructure = structureData[0];
                    structureState.fieldName = fieldName;
                    state = State.structure;
                    return;
                }
            case pvArray: {
                    String elementType = attributes.get("elementType");
                    if(elementType==null) {
                        iocxmlReader.message(
                                "elementType not given",
                                MessageType.warning);
                        state = State.idle;
                    }
                    field = fieldCreate.createArray(
                        actualFieldName,fieldCreate.getType(elementType));
                    PVArrayArray pvArrayArray = arrayState.pvArrayArray;
                    PVArray[] arrayData = arrayState.arrayData;
                    arrayData[0] = (PVArray)pvDataCreate.createPVField(pvArray,field);
                    pvArrayArray.put(arrayOffset,1,arrayData,0);
                    String value = attributes.get("capacity");
                    if(value!=null) arrayData[0].setCapacity(Integer.parseInt(value));
                    value = attributes.get("length");
                    if(value!=null) arrayData[0].setLength(Integer.parseInt(value));
                    String supportName = attributes.get("supportName");
                    if(supportName!=null) {
                        arrayData[0].setSupportName(supportName);
                    }
                    arrayStack.push(arrayState);
                    arrayState = new ArrayState();
                    arrayState.prevState = state;
                    arrayState.pvArray = arrayData[0];
                    arrayState.fieldName = fieldName;
                    state = State.array;
                    arrayStart(attributes);
                    return;
                }
            case pvLink: {
                       field = fieldCreate.createField(actualFieldName,Type.pvLink);
                       PVLinkArray pvLinkArray = arrayState.pvLinkArray;
                       PVLink[] linkData = arrayState.linkData;
                       linkData[0] = (PVLink)pvDataCreate.createPVField(pvArray,field);
                       pvLinkArray.put(arrayOffset,1,linkData,0);
                       String supportName = attributes.get("supportName");
                       if(supportName!=null) {
                           linkData[0].setSupportName(supportName);
                       }
                       fieldState.prevState = state;
                       fieldState.pvField = linkData[0];
                       state = State.field;
                       return;
                }
            }
            iocxmlReader.message(
                    "fieldName " + qName + " illegal type ???",
                    MessageType.error);
            idleState.prevState = state;
            state = State.idle;
        }
        
        private void endArrayElement(String qName)  {
            if(!qName.equals("value")) {
                iocxmlReader.message(
                        "arrayEndElement Logic error: expected value",
                        MessageType.error);
            }
            arrayState.valueActive = false;
            int arrayOffset = arrayState.arrayOffset;
            PVArray pvArray = arrayState.pvArray;
            Type type = arrayState.arrayElementType;
            if(type.isPrimitive() || type==Type.pvString) {
                String value = stringBuilder.toString();
                String[] values = null;
                if(type.isPrimitive()) {
                    values = primitivePattern.split(value);
                } else {
                    // ignore blanks , is separator
                    values = stringPattern.split(value);
                }
                try {
                    int num = convert.fromStringArray(
                        pvArray,arrayOffset,values.length,values,0);
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
                return;
            }
            if(type==Type.pvStructure) {
                structureState = structureStack.pop();
                ++arrayState.arrayOffset;
                return;
            }
            
            ++arrayState.arrayOffset;
        }
    }
        
}
