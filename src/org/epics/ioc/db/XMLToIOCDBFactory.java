/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldAttribute;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVArrayArray;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.IOCXMLListener;
import org.epics.ioc.util.IOCXMLReader;
import org.epics.ioc.util.IOCXMLReaderFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;


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
     * @param requester A listener for error messages.
     * @return An IOC Database that has the newly created record instances.
     */
    public static IOCDB convert(String iocdbName,String fileName,Requester requester) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToIOCDBFactory is already active", MessageType.error);
            return null;
        }
        try {
            iocdb =IOCDBFactory.create(iocdbName);           
            dbd = DBDFactory.getMasterDBD();
            iocdbMaster = IOCDBFactory.getMaster();
            XMLToIOCDBFactory.requester = requester;
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
            structure,
            support,
            create,
            record
        } 
        private State state = State.idle;
        private DBDXMLHandler structureHandler = new DBDXMLStructureHandler();
        private DBDXMLHandler supportHandler = new DBDXMLSupportHandler();
        private DBDXMLHandler createHandler = new DBDXMLCreateHandler();
        private RecordHandler  recordHandler = null;
 
        public void endDocument() {}       

        public void startElement(String qName,Map<String,String> attributes)
        {
            switch(state) {
            case idle:
                if(qName.equals("structure")) {
                    state = State.structure;
                    structureHandler.start(qName,attributes);
                } else if(qName.equals("support")) {
                    state = State.support;
                    supportHandler.start(qName,attributes);
                } else if(qName.equals("create")) {
                    state = State.create;
                    createHandler.start(qName,attributes);
                } else if(qName.equals("record")) {
                    recordHandler = new RecordHandler(qName,attributes);
                    state = State.record;
                } else {
                    iocxmlReader.message(
                        "startElement " + qName + " not understood",
                        MessageType.error);
                }
                break;
            case structure:
                structureHandler.startElement(qName,attributes);
                break;
            case support:
                supportHandler.startElement(qName,attributes);
                break;
            case create:
                createHandler.startElement(qName,attributes);
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
            case structure:
                if(qName.equals("structure")) {
                    structureHandler.end(qName);
                    state = State.idle;
                } else {
                    structureHandler.endElement(qName);
                }
                break;
            case support:
                if(qName.equals("support")) {
                    supportHandler.end(qName);
                    state = State.idle;
                } else {
                    supportHandler.endElement(qName);
                }
                break;
            case create:
                if(qName.equals("create")) {
                    createHandler.end(qName);
                    state = State.idle;
                } else {
                    createHandler.endElement(qName);
                }
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
            case structure:
                structureHandler.characters(ch,start,length);
                break;
            case support:
                supportHandler.characters(ch,start,length);
                break;
            case create:
                createHandler.characters(ch,start,length);
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
    
    private interface DBDXMLHandler {
        void start(String qName, Map<String,String> attributes);
        void end(String qName);
        void startElement(String qName, Map<String,String> attributes);
        void characters(char[] ch, int start, int length);
        void endElement(String qName);
    }
    
    private static class DBDXMLStructureHandler implements DBDXMLHandler
    {
        static final String[] excludeString = {
            "name","supportName","createName","associatedField",
            "type","elementType","structureName","factoryName"};
 
        private enum State {idle, structure, field}      
        
        private State state = State.idle;
        private String structureName;
        private String structureSupportName = null;
        private String structureCreateName = null;
        private String fieldSupportName = null;
        private String fieldCreateName = null;
        private LinkedList<Field> fieldList;
        // remaining are for field elements
        private String fieldName;
        private DBDStructure fieldStructure;
        private Type type;
        private Type elementType;
        private FieldAttribute fieldAttribute;
        private FieldAttribute structureAttribute;
        
        DBDXMLStructureHandler()
        {
            super();
        }
            
        public void start(String qName, Map<String,String> attributes) {
            if(state!=State.idle) {
                iocxmlReader.message(
                   "DBDXMLStructureHandler.start logic error not idle",
                   MessageType.error);
                state = State.idle;
                return;
            }
            structureName = attributes.get("name");
            if(structureName==null || structureName.length() == 0) {
                iocxmlReader.message("name not specified",
                        MessageType.error);
                state = State.idle;
                return;
            }
            structureSupportName = attributes.get("supportName");
            structureCreateName = attributes.get("createName");
            if(qName.equals("structure")){
                if(dbd.getStructure(structureName)!=null) {
                    iocxmlReader.message(
                        "structure " + structureName + " already exists",
                        MessageType.warning);
                    state = State.idle;
                    return;
                }
            } else {
                iocxmlReader.message(
                        "DBDXMLStructureHandler.start logic error",
                        MessageType.error);
                state = State.idle;
                return;
            }
            structureAttribute = fieldCreate.createFieldAttribute();
            structureAttribute.setAttributes(attributes,excludeString);
            fieldList = new LinkedList<Field>();
            state = State.structure;
        }
    
        public void end(String qName){
            if(state==State.idle) {
                fieldList = null;
                return;
            }
            Field[] field = new Field[fieldList.size()];
            ListIterator<Field> iter1 = fieldList.listIterator();
            for(int i=0; i<field.length; i++) {
                field[i] = iter1.next();
            }
            DBDStructure dbdStructure = dbd.createStructure(
                    structureName,field,structureAttribute);
            boolean result = dbd.addStructure(dbdStructure);
            if(!result) {
                iocxmlReader.message(
                        "structure " + structureName + " already exists",
                        MessageType.warning);
            }
            if(structureSupportName!=null) {
                dbdStructure.setSupportName(structureSupportName);
            }
            if(structureCreateName!=null) {
                dbdStructure.setCreateName(structureCreateName);
            }
            fieldList = null;
            state= State.idle;
        }
     
        public void startElement(String qName, Map<String,String> attributes){
            if(state==State.idle) return;
            if(qName.equals("field")) {
                assert(state==State.structure);
                fieldSupportName = null;
                fieldCreateName = null;
                fieldAttribute = fieldCreate.createFieldAttribute();
                fieldAttribute.setAttributes(attributes, excludeString);
                fieldName = attributes.get("name");
                if(fieldName==null) {
                    iocxmlReader.message("name not specified",
                            MessageType.error);
                    state= State.idle;
                    return;
                }
                type = fieldCreate.getType(attributes);
                if(type==null) {
                    iocxmlReader.message("type not specified correctly",
                            MessageType.error);
                    state= State.idle;
                    return;
                }
                if(type==Type.pvStructure) {
                    String fieldStructureName = attributes.get("structureName");
                    if(fieldStructureName==null) fieldStructureName = "null";
                    fieldStructure = dbd.getStructure(fieldStructureName);
                    if(fieldStructure==null) {
                        iocxmlReader.message("structure " + fieldStructureName + " not found in DBD database",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                    fieldSupportName = attributes.get("supportName");
                    if(fieldSupportName==null) fieldSupportName = fieldStructure.getSupportName();
                    fieldCreateName = attributes.get("createName");
                    if(fieldCreateName==null) fieldCreateName = fieldStructure.getCreateName();
                }
                if(type==Type.pvArray) {
                    elementType = fieldCreate.getElementType(attributes);
                    if(elementType==null) {
                        iocxmlReader.message("elementType not specified correctly",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                }
                if(fieldSupportName==null) fieldSupportName = attributes.get("supportName");
                if(fieldCreateName==null) fieldCreateName = attributes.get("createName");
                state = State.field;
            }
        }
    
        public void endElement(String qName){
            if(state==State.idle) return;
            if(!qName.equals("field")) return;
            assert(state==State.field);
            state = State.structure;
            Field field = null;
            switch(type) {
            case pvStructure:
                field = fieldCreate.createStructure(fieldName,
                    fieldStructure.getStructureName(),fieldStructure.getFields(),fieldAttribute);
                break;
            case pvArray:
                field = fieldCreate.createArray(fieldName,elementType,fieldAttribute);
                break;
            default:
                field = fieldCreate.createField(fieldName,type,fieldAttribute);
                    break;
            }
            if(field==null) {
                throw new IllegalStateException("logic error");
            }
            fieldList.add(field);
            if(fieldSupportName!=null) {
                field.setSupportName(fieldSupportName);
            }
            if(fieldCreateName!=null) {
                field.setCreateName(fieldCreateName);
            }
            return;
        }
    
        public void characters(char[] ch, int start, int length){}
    }
    
    private static class DBDXMLSupportHandler implements DBDXMLHandler{
        
        public void start(String qName, Map<String,String> attributes) {
            String name = attributes.get("name");
            if(name==null||name.length()==0) {
                iocxmlReader.message(
                    "name was not specified correctly",
                    MessageType.error);
                return;
            }
            DBDSupport support = dbd.getSupport(name);
            if(support!=null) {
                iocxmlReader.message(
                    "support " + name  + " already exists",
                    MessageType.warning);
                    return;
            }
            String factoryName = attributes.get("factoryName");
            if(factoryName==null||factoryName.length()==0) {
                iocxmlReader.message(
                    "factoryName was not specified correctly",
                    MessageType.error);
                return;
            }
            support = dbd.createSupport(name,factoryName);
            if(support==null) {
                iocxmlReader.message(
                    "failed to create support " + qName,
                    MessageType.error);
                    return;
            }
            if(!dbd.addSupport(support)) {
                iocxmlReader.message(
                    "support " + qName + " already exists",
                    MessageType.warning);
                return;
            }
        }
    
        public void end(String qName) {}
    
        public void startElement(String qName, Map<String,String> attributes) {}
    
        public void endElement(String qName) {}
    
        public void characters(char[] ch, int start, int length) {}
        
    }
    
    private static class DBDXMLCreateHandler implements DBDXMLHandler{
        
        public void start(String qName, Map<String,String> attributes) {
            String name = attributes.get("name");
            if(name==null||name.length()==0) {
                iocxmlReader.message(
                    "name was not specified correctly",
                    MessageType.error);
                return;
            }
            DBDCreate create = dbd.getCreate(name);
            if(create!=null) {
                iocxmlReader.message(
                    "create " + name  + " already exists",
                    MessageType.warning);
                    return;
            }
            String factoryName = attributes.get("factoryName");
            if(factoryName==null||factoryName.length()==0) {
                iocxmlReader.message(
                    "factoryName was not specified correctly",
                    MessageType.error);
                return;
            }
            create = dbd.createCreate(
                name,factoryName);
            if(create==null) {
                iocxmlReader.message(
                    "failed to create create " + qName,
                    MessageType.error);
                    return;
            }
            if(!dbd.addCreate(create)) {
                iocxmlReader.message(
                    "create " + qName + " already exists",
                    MessageType.warning);
                return;
            }
        }
    
        public void end(String qName) {}
    
        public void startElement(String qName, Map<String,String> attributes) {}
    
        public void endElement(String qName) {}
    
        public void characters(char[] ch, int start, int length) {}
        
    }
    
    private static class RecordHandler
    {
        enum State {idle, structure, field, array}
        private State state = State.idle;
        private StringBuilder stringBuilder = new StringBuilder();
        private PVRecord pvRecord = null;
        //private DBRecord dbRecord = null;
        
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
        
        private static class StructureState {
            private State prevState = null;
            private String fieldName = null;
            private PVStructure pvStructure = null;
        }
        private StructureState structureState = new StructureState();
        private Stack<StructureState> structureStack = new Stack<StructureState>();
        
        private static class ArrayState {
            private boolean elementActive = false;
            private State prevState = null;
            private String fieldName = null;
            private PVArray pvArray = null;
            private Type arrayElementType = null;
            private int arrayOffset = 0;

            private PVStructure[] structureData = null;
            private PVStructureArray pvStructureArray = null;

            private PVArray[] arrayData = null;
            private PVArrayArray pvArrayArray = null;
        }
        private ArrayState arrayState = null;
        private Stack<ArrayState> arrayStack = new Stack<ArrayState>();
        
        private RecordHandler(String qName, Map<String,String> attributes) {
            String recordName = attributes.get("name");
            String structureName = attributes.get("structureName");
            if(recordName==null) {
                iocxmlReader.message(
                    "attribute name not specified",
                    MessageType.error);
                state = State.idle;
                return;
            }
            if(structureName==null) structureName = "generic";
            DBDStructure dbdStructure = dbd.getStructure(structureName);
            if(dbdStructure==null) {
                iocxmlReader.message(
                    "structure " + structureName + " does not exist.",
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
            DBRecord dbRecord = iocdb.findRecord(recordName);
            if(dbRecord==null) {              
                Structure structure = dbd.getStructure(structureName);
                pvRecord = pvDataCreate.createPVRecord(recordName, structure);
            } else {
                pvRecord = dbRecord.getPVRecord();
            }
            String supportName = attributes.get("supportName");
            if(supportName==null) {
                supportName = pvRecord.getSupportName();
            }
            if(supportName==null) {
                supportName = dbdStructure.getSupportName();
            }
            if(supportName==null) {
                supportName = "generic";
            }
            pvRecord.setSupportName(supportName);
            structureState.pvStructure = pvRecord;
            state = State.structure;
        }
        
        private void endRecord() {
            if(state==State.idle) return;
            DBRecord dbRecord = iocdb.findRecord(pvRecord.getRecordName());
            if(dbRecord!=null) return;
            dbRecord = DBRecordFactory.create(pvRecord,iocdb,dbd);
            if(!iocdb.addRecord(dbRecord)) {
                iocxmlReader.message(
                        "failed to add record to iocdb",
                        MessageType.info);
            }
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
                // just ignore any additional xml elements
                idleState.prevState = state;
                state = State.idle;
                break;
            case array:
                startArrayElement(qName,attributes);
                break;
            }
        }
        
        private void characters(char[] ch, int start, int length)  {
            while(start<ch.length && length>0
                    && Character.isWhitespace(ch[start])) {
                        start++; length--;
                    }
            while(length>0 && Character.isWhitespace(ch[start+ length-1])) {
                length--;
            }
            if(length<=0) return;
            switch(state) {
            case idle: break;
            case structure:
                iocxmlReader.message(
                        "unexpected characters",
                        MessageType.warning);
                break;
            case array:
            case field:
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
            case array:
                if(qName.equals("element")) {
                    if(arrayState.elementActive) {
                        endArrayElement(qName);
                        break;
                    }
                    state = arrayState.prevState;
                    if(state!=State.array) {
                        iocxmlReader.message(
                                "logic error ",
                                MessageType.error);
                        break;
                    }
                    arrayState = arrayStack.pop();
                    endArrayElement(qName);
                    break;
                }
                if(!arrayState.elementActive && qName.equals(arrayState.fieldName)) {
                    state = arrayState.prevState;
                    if(!arrayStack.isEmpty()) arrayState = arrayStack.pop();
                    break;
                }
                iocxmlReader.message(
                        "endElement error: expected element or " + arrayState.fieldName,
                        MessageType.error);
                break;
            }
        }
        
        private void startStructureElement(String qName, Map<String,String> attributes) {
            PVStructure pvStructure = structureState.pvStructure;
            Structure structure = pvStructure.getStructure();
            int fieldIndex = structure.getFieldIndex(qName);
            if(fieldIndex<0) {
                String message = appendStructureElement(qName,attributes);
                if(message!=null) {
                    iocxmlReader.message("fieldName " + qName + " " + message,MessageType.error);
                    idleState.prevState = state;
                    state = State.idle;
                    return;
                }
                pvStructure = structureState.pvStructure;
                structure = pvStructure.getStructure();
                fieldIndex = structure.getFieldIndex(qName);
            }
            PVField pvField = pvStructure.getPVFields()[fieldIndex];
            String supportName = attributes.get("supportName");
            if(supportName!=null) {
                pvField.setSupportName(supportName);
            }
            String createName = attributes.get("createName");
            if(createName!=null) {
                pvField.setCreateName(createName);
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
            if(type==Type.pvStructure) {
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
                    pvField = pvStructure.getPVFields()[fieldIndex];
                }
                pushNewStructure((PVStructure)pvField,qName,attributes);
                return;
            } else if(type==Type.pvArray) {
                pushNewArray((PVArray)pvField,qName,attributes);
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
            if(value==null || value.length()<=0) return;
            PVField pvField = fieldState.pvField;
            Field field = pvField.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                try {
                    convert.fromString(pvField, value);
                } catch (NumberFormatException e) {
                    iocxmlReader.message(e.toString(),
                            MessageType.warning);
                }
                return;
            }
            iocxmlReader.message("Logic error in endField",MessageType.error);
        }
        
        
        private void startArrayElement(String qName, Map<String,String> attributes)  {
            if(!qName.equals("element")) {
                iocxmlReader.message(
                        "startArrayElement Logic error: expected element",
                        MessageType.error);
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            arrayState.elementActive = true;
            String offset = attributes.get("offset");
            if(offset!=null) arrayState.arrayOffset = Integer.parseInt(offset);
            Type arrayElementType = arrayState.arrayElementType;
            if(arrayElementType.isScalar()) {
                stringBuilder.setLength(0);
                return;
            }
            int arrayOffset = arrayState.arrayOffset;
            PVArray pvArray = arrayState.pvArray;
            String actualFieldName = "[" + arrayOffset + "]";
            Field field;
            if(arrayElementType==Type.pvStructure) {
                String structureName = attributes.get("structureName");
                if(structureName==null) {
                    structureName = "generic";
                }
                DBDStructure dbdStructure = dbd.getStructure(structureName);
                if(dbdStructure==null) {
                    iocxmlReader.message(
                            "structureName not found",
                            MessageType.warning);
                    idleState.prevState = state;
                    state = State.idle;
                    return;
                }
                field = fieldCreate.createStructure(
                        actualFieldName,
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getFieldAttribute());
                PVStructureArray pvStructureArray = arrayState.pvStructureArray;
                PVStructure[] structureData = arrayState.structureData;
                structureData[0] = (PVStructure)pvDataCreate.createPVField(pvArray,field);
                pvStructureArray.put(arrayOffset,1,structureData,0);
                pushNewStructure((PVStructure)structureData[0],qName,attributes);
                return;
            } else if(arrayElementType==Type.pvArray) {
                String elementType = attributes.get("elementType");
                String structureName = attributes.get("structureName");
                if(elementType.equals("structure") && structureName==null) {
                    structureName = "generic";
                }
                if(elementType==null) {
                    if(structureName==null) {
                        iocxmlReader.message("elementType not given",MessageType.warning);
                        state = State.idle;
                        return;
                    }
                    elementType = "structure";
                }
                field = fieldCreate.createArray(
                        actualFieldName,fieldCreate.getType(elementType));
                PVArrayArray pvArrayArray = arrayState.pvArrayArray;
                PVArray[] arrayData = arrayState.arrayData;
                int capacity = 0;
                String capacityAttribute = attributes.get("capacity");
                if(capacityAttribute!=null) {
                    capacity = Integer.decode(capacityAttribute);
                }
                boolean capacityMutable = true;
                String capacityMutableAttribute = attributes.get("capacityMutable");
                if(capacityMutableAttribute!=null) {
                    capacityMutable = Boolean.getBoolean(capacityMutableAttribute);
                }
                arrayData[0] = pvDataCreate.createPVArray(pvArrayArray, field, capacity, capacityMutable);
                pvArrayArray.put(arrayOffset,1,arrayData,0);
                pushNewArray((PVArray)arrayData[0],qName,attributes);
                return;
            }
            iocxmlReader.message(
                    "fieldName " + qName + " illegal type ???",
                    MessageType.error);
            idleState.prevState = state;
            state = State.idle;
        }
        
        private void endArrayElement(String qName)  {
            if(!qName.equals("element")) {
                iocxmlReader.message(
                        "endArrayElement Logic error: expected element",
                        MessageType.error);
                state = State.idle;
                return;
            }
            arrayState.elementActive = false;
            int arrayOffset = arrayState.arrayOffset;
            PVArray pvArray = arrayState.pvArray;
            Type type = arrayState.arrayElementType;
            if(type.isScalar()) {
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
        
        private void pushNewStructure(PVStructure pvStructure,String fieldName, Map<String,String> attributes) {
            structureStack.push(structureState);
            structureState = new StructureState();
            structureState.prevState = state;
            structureState.pvStructure = pvStructure;
            structureState.fieldName = fieldName;
            state = State.structure;
            DBDStructure dbdStructure = dbd.getStructure(pvStructure.getStructure().getStructureName());
            String supportName = attributes.get("supportName");
            if(supportName==null) {
                if(pvStructure.getSupportName()==null && dbdStructure!=null) {
                    supportName = dbdStructure.getSupportName();
                }
            }
            if(supportName!=null) {
                pvStructure.setSupportName(supportName);
            }
            String createName = attributes.get("createName");
            if(createName==null) {
                if(pvStructure.getCreateName()==null && dbdStructure!=null) {
                    createName = dbdStructure.getCreateName();
                }
            }
            if(createName!=null) {
                pvStructure.setCreateName(createName);
            }
        }
        
        private String appendStructureElement(String qName, Map<String,String> attributes) {
            PVStructure pvStructure = structureState.pvStructure;
            String typeName =  attributes.get("type");   
            String structureName =  attributes.get("structureName");
            String elementTypeName =  attributes.get("elementType");
            if(typeName==null) {
                if(structureName!=null)  {
                    typeName = "structure";
                } else if(elementTypeName!=null) {
                    typeName = "array";
                }   
            }
            if(typeName==null) return "not found";
            if(typeName.equals("structure") && structureName==null) {
                structureName = "generic";
            }
            if(typeName.equals("array") && elementTypeName==null) {
                elementTypeName = "structure";
            }
            if(structureName!=null && !typeName.equals("structure")) {
                iocxmlReader.message(
                        "structureName specified but type is not structure",
                        MessageType.warning);
            }
            if(elementTypeName!=null && !typeName.equals("array")) {
                iocxmlReader.message(
                        "elementTypeName specified but type is not array",
                        MessageType.warning);
            }
            Field newField = null;
            PVField newPVField = null;
            if(structureName!=null) {
                DBDStructure dbdStructure = dbd.getStructure(structureName);
                if(dbdStructure==null) return "structureName not found";
                newField = fieldCreate.createStructure(
                        qName,
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getFieldAttribute());
                newPVField = pvDataCreate.createPVField(pvStructure, newField);  
            } else if(elementTypeName!=null) {
                Type type = fieldCreate.getType(elementTypeName);
                if(type==null) return "illegal elementType";
                int capacity = 0;
                String capacityAttribute = attributes.get("capacity");
                if(capacityAttribute!=null) capacity = Integer.decode(capacityAttribute);
                boolean capacityMutable = true;
                String capacityMutableAttribute = attributes.get("capacityMutable");
                if(capacityMutableAttribute!=null) {
                    capacityMutable = Boolean.getBoolean(capacityMutableAttribute);
                }
                Array array = fieldCreate.createArray(qName,type);
                newField = array;
                newPVField = pvDataCreate.createPVArray(pvStructure, array, capacity, capacityMutable);
                if(attributes.get("supportName")==null) {
                    if(type==Type.pvArray || type==Type.pvStructure) {
                        newPVField.setSupportName("generic");
                    }
                }
            } else {
                Type type = fieldCreate.getType(typeName);
                if(type==null) return "illegal type";
                newField = fieldCreate.createField(qName, type);
                newPVField = pvDataCreate.createPVField(pvStructure, newField);
            }
            pvStructure.appendPVField(newPVField);
            return null;
        }
        
        private void pushNewArray(PVArray pvArray,String fieldName, Map<String,String> attributes) {
            if(arrayState!=null) arrayStack.push(arrayState);
            arrayState = new ArrayState();
            arrayState.prevState = state;
            arrayState.pvArray = pvArray;
            arrayState.fieldName = fieldName;
            state = State.array;
            String supportName = attributes.get("supportName");
            if(supportName!=null) {
                pvArray.setSupportName(supportName);
            }
            String createName = attributes.get("createName");
            if(createName!=null) {
                pvArray.setCreateName(createName);
            }
            String lengthAttribute = attributes.get("length");
            if(lengthAttribute!=null) pvArray.setLength(Integer.parseInt(lengthAttribute));
            Array array = pvArray.getArray();
            Type arrayElementType = array.getElementType();
            arrayState.arrayElementType = arrayElementType;
            arrayState.arrayOffset = 0;
            String capacityMutable = attributes.get("capacityMutable");
            if(capacityMutable!=null && capacityMutable.equals("true")) {
                pvArray.setCapacityMutable(true);
            }
            String value = attributes.get("capacity");
            if(value!=null) {
                int capacity = Integer.parseInt(value);
                if(capacity!=pvArray.getCapacity()) {
                    if(pvArray.isCapacityMutable()) {
                        pvArray.setCapacity(capacity);
                    } else {
                        iocxmlReader.message(
                                "capacityMutable is false",
                                MessageType.error);
                    }
                }
            }
            if(capacityMutable!=null && capacityMutable.equals("false")) {
                pvArray.setCapacityMutable(false);
            }
            value = attributes.get("length");
            if(value!=null) pvArray.setLength(Integer.parseInt(value));
            if (arrayElementType.isScalar()) return;
            if(arrayElementType==Type.pvStructure) {
                arrayState.structureData = new PVStructure[1];
                arrayState.pvStructureArray = (PVStructureArray)pvArray;
                return;
            } else if(arrayElementType==Type.pvArray) {
                arrayState.arrayData = new PVArray[1];
                arrayState.pvArrayArray = (PVArrayArray)pvArray;
                return;
            }
            iocxmlReader.message(" Logic error pushNewArray",MessageType.error);
           
        }
    }  
}
