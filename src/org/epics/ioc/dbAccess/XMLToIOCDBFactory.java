/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Type;


/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToIOCDBFactory {
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbdin the reflection database.
     * @param iocdbin IOC database.
     * @param fileName The name of the file containing xml record instance definitions.
     * @throws MalformedURLException If SAX throws it.
     * @throws IllegalStateException If any errors were detected.
     */
    public static void convert(DBD dbdin, IOCDB iocdbin, String fileName)
        throws MalformedURLException
    {
        String uri = new File(fileName).toURL().toString();
        XMLReader reader;
        
        dbd = dbdin;
        iocdb = iocdbin;
        Handler handler = new Handler();
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(uri);
        } catch(MalformedURLException e) {
            throw new MalformedURLException (
            "\nXMLToIOCDBFactory.convert terminating with MalformedURLException\n"
            + e.getMessage());
        } catch (SAXException e) {
            throw new IllegalStateException(
                "\nXMLToIOCDBFactory.convert terminating with SAXException\n"
                + e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException (
                "\nXMLToIOCDBFactory.convert terminating with IOException\n"
                + e.getMessage());
        } catch (IllegalStateException e) {
            handler.error("IllegalStateException " + e.getMessage());
            throw new IllegalStateException(
                "\nXMLToDBDFactory.convert terminating with IllegalStateException\n"
                + e.getMessage());
        } catch (IllegalArgumentException e) {
            handler.error("IllegalArgumentException " + e.getMessage());
            throw new IllegalStateException(
                "\nXMLToDBDFactory.convert terminating with IllegalArgumentException\n"
                + e.getMessage());
        } catch (Exception e) {
            handler.error("Exception " + e.getMessage());
            throw new IllegalStateException(
                "\nXMLToDBDFactory.convert terminating with Exception\n"
                + e.getMessage());
        }
    }
            
    // for use by private classes
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");
    private static Pattern stringPattern = Pattern.compile("\\s*,\\s*");
    private static DBD dbd;
    private static IOCDB iocdb;
    private static ErrorHandler errorHandler;
    private static Locator locator;
    
    private static class Handler  implements ContentHandler, ErrorHandler
    {
        private enum State {
            startDocument,
            idle,
            record,
        } 
        private State state = State.startDocument;
        private int nWarning = 0;
        private int nError = 0;
        private int nFatal = 0;
        private RecordHandler  recordHandler = null;

        Handler()  throws MalformedURLException {
        }
        
        private String printSAXParseExceptionMessage(SAXParseException e)
        {
            return String.format("line %d column %d\nreason %s\n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                e.toString());
        }
 
        public void error(String message) {
            System.err.printf("line %d column %d\nreason %s\n",
                    locator.getLineNumber(),
                    locator.getColumnNumber(),
                    message);
                nError++;
        }
        public void warning(SAXParseException e) throws SAXException {
            System.err.printf("warning %s\n",
                printSAXParseExceptionMessage(e));
            nWarning++;
        }
        public void error(SAXParseException e) throws SAXException {
            System.err.printf("error %s\n",
                printSAXParseExceptionMessage(e));
            nError++;
        }
        
        public void fatalError(SAXParseException e) throws SAXException {
            System.err.printf("fatal error %s\n",
                printSAXParseExceptionMessage(e));
            nFatal++;
        }
        
        public void setDocumentLocator(Locator locatorin) {
            errorHandler = this;
            locator = locatorin;
        }
        
        public void startDocument() throws SAXException {
            state = State.startDocument;
        }
        
        
        public void endDocument() throws SAXException {
            if(nWarning>0 || nError>0 || nFatal>0) {
                System.err.printf(
                    "endDocument: warning %d severe %d fatal %d\n",
                    nWarning,nError,nFatal);
            }
        }       

        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
        {
            switch(state) {
            case startDocument:
                if(qName.equals("IOCDatabase")) state = State.idle;
                break;
            case idle:
                if(qName.equals("record")) {
                    recordHandler = new RecordHandler(qName,attributes);
                    state = State.record;
                } else {
                    System.err.printf(
                        "startElement element %s not understood\n",qName);
                    nError++;
                }
                break;
            case record: 
                recordHandler.startElement(qName,attributes);
                break;
            }
        }
        
        public void endElement(String uri, String localName, String qName)
        throws SAXException
        {
            switch(state) {
            case startDocument:
                break;
            case idle:
                if(qName.equals("IOCDatabase")) {
                    state = State.startDocument;
                } else {
                    System.err.printf(
                        "startElement element %s not understood\n",qName);
                    nError++;
                }
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
        throws SAXException
        {
            switch(state) {
            case startDocument:
                break;
            case idle:
                break;
            case record: 
                recordHandler.characters(ch,start,length);
                break;
            }
        }
        
        public void endPrefixMapping(String prefix) throws SAXException {
            // nothing to do
            
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
        {
            // nothing to do
            
        }

        public void processingInstruction(String target, String data)
        throws SAXException
        {
            // nothing to do
            
        }

        public void skippedEntity(String name) throws SAXException {
            // nothing to do
            
        }

        public void startPrefixMapping(String prefix, String uri)
        throws SAXException
        {
            // nothing to do
            
        }

    }

    private static class RecordHandler
    {
        enum State {idle, structure, field, enumerated, array}
        private State state;
        private StringBuilder stringBuilder = new StringBuilder();
        private DBRecord dbRecord = null;
        
        private static class IdleState {
            private State prevState;
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
        
        RecordHandler(String qName, Attributes attributes)
        throws SAXException {
            String recordName = attributes.getValue("name");
            String recordTypeName = attributes.getValue("type");
            if(recordName==null) {
                errorHandler.error(new SAXParseException(
                    "attribute name not specified",locator));
                state = State.idle;
                return;
            }
            if(recordTypeName==null) {
                errorHandler.error(new SAXParseException(
                    "attribute type not specified",locator));
                state = State.idle;
                return;
            }
            DBDRecordType dbdRecordType = dbd.getRecordType(recordTypeName);
            if(dbdRecordType==null) {
                errorHandler.warning(new SAXParseException(
                    "record type " + recordTypeName + " does not exist.",
                    locator));
                state = State.idle;
                return;
            }
            dbRecord = iocdb.findRecord(recordName);
            if(dbRecord==null) {
                boolean result = iocdb.createRecord(recordName,dbdRecordType);
                if(!result) {
                    errorHandler.warning(new SAXParseException(
                            "failed to create record " + recordTypeName,
                            locator));
                    state = State.idle;
                    return;
                }
                dbRecord = iocdb.findRecord(recordName);
                dbRecord.setDBD(dbd);
            }
            String supportName = attributes.getValue("supportName");
            if(supportName==null) {
                supportName = dbRecord.getSupportName();
            }
            if(supportName==null) {
                supportName = dbdRecordType.getSupportName();
            }
            if(supportName==null) {
                errorHandler.error(new SAXParseException(
                        "record  " + recordName + " has no record support",
                        locator));
            } else {
                String error = dbRecord.setSupportName(supportName);
                if(error!=null) {
                    errorHandler.error(new SAXParseException(
                            "record  " + recordName
                            + " " +error,
                            locator));
                }
            }
            structureState.dbStructure = dbRecord;
            state = State.structure;
        }
        
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            switch(state) {
            case idle: 
                idleStack.push(idleState);
                idleState = new IdleState();
                idleState.prevState = state;
                state = State.idle;
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
        
        void characters(char[] ch, int start, int length) throws SAXException {
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
        
        void endElement(String qName) throws SAXException {
            switch(state) {
            case idle: 
                idleState = idleStack.pop();
                state = idleState.prevState;
                if(state==State.idle) {
                    idleState = idleStack.pop();
                }
                break;
            case structure: 
                if(!qName.equals(structureState.fieldName)) {
                    errorHandler.error(new SAXParseException(
                            "Logic error: qName " + qName
                            + " but expected " + structureState.fieldName,
                            locator));
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
        
        private void startStructureElement(String qName, Attributes attributes) throws SAXException{
            DBStructure dbStructure = structureState.dbStructure;
            int dbDataIndex = dbStructure.getFieldDBDataIndex(qName);
            if(dbDataIndex<0) {
                errorHandler.error(new SAXParseException(
                    "fieldName " + qName + " not found",
                     locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBData dbData = dbStructure.getFieldDBDatas()[dbDataIndex];
            String supportName = attributes.getValue("supportName");
            if(supportName!=null) dbData.setSupportName(supportName);
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
                        errorHandler.error(new SAXParseException(
                                "fieldName " + qName + " illegal type ???",
                                 locator));
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
                    String structureName = attributes.getValue("structureName");
                    if(structureName!=null) {
                        DBDStructure dbdStructure = dbd.getStructure(structureName);
                        if(dbdStructure==null) {
                            errorHandler.error(new SAXParseException(
                                    "structureName " + structureName + " not defined",
                                     locator));
                            idleState.prevState = state;
                            state = State.idle;
                            return;
                        }
                        DBStructure fieldStructure = (DBStructure)dbData;
                        if(!fieldStructure.createFields(dbdStructure)) {
                            errorHandler.warning(new SAXParseException(
                                "structureName " + structureName
                                + " not used because a structure was already defined",
                                locator));
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
                    if(supportName!=null) dbData.setSupportName(supportName);
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
        private void endField() throws SAXException {
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
                            errorHandler.warning(new SAXParseException(
                                e.toString(),locator));
                        }
                    }
                    return;
                } else {
                    errorHandler.error(new SAXParseException(
                            " Logic Error endField illegal type ???",
                             locator));
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
                errorHandler.error(new SAXParseException(
                    "menu value " + value + " is not a valid choice",
                    locator));
                return;
            } else if(dbType==DBType.dbLink) {
                return;
            }
            errorHandler.error(new SAXParseException(
                "Logic error in endField",
                locator));
        }
        
        private void startEnumElement(String qName, Attributes attributes) throws SAXException{
            if(!qName.equals("choice")) {
                errorHandler.error(new SAXParseException(
                    qName + " illegal. Only choice is valid.",
                    locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            if(stringBuilder.length()>0) {
                enumState.value +=  stringBuilder.toString();
                stringBuilder.setLength(0);
            }
        }
        private void endEnum() throws SAXException{
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
                errorHandler.warning(new SAXParseException(
                    value + " is not a valid choice",locator));
            }
            return;
        }
        
        private void endEnumElement(String qName) throws SAXException{
            enumState.enumChoiceList.add(stringBuilder.toString());
            stringBuilder.setLength(0);
        }
        
        private void supportStart(DBData dbData, Attributes attributes) throws SAXException{
            String supportName = dbData.getSupportName();
            if(supportName==null) {
                errorHandler.error(new SAXParseException(
                        "no support is defined",
                        locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            DBDSupport support = dbd.getSupport(supportName);
            if(support==null) {
                errorHandler.error(new SAXParseException(
                        "support " + supportName + " not defined",
                        locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String configurationStructureName = support.getConfigurationStructureName();
            if(configurationStructureName==null) {
                errorHandler.error(new SAXParseException(
                        "support " + supportName + " does not define a configurationStructureName",
                        locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            String structureName = attributes.getValue("structureName");
            if(structureName==null) {
                errorHandler.error(new SAXParseException(
                        "structureName was not specified",
                        locator));
                idleState.prevState = state;
                state = State.idle;
                return;
            }
            if(!structureName.equals(configurationStructureName)) {
                errorHandler.error(new SAXParseException(
                        "structureName was not specified",
                        locator));
                idleState.prevState = state;
                state = State.idle;
                return; 
            }
            String error = dbData.setSupportName(supportName);
            if(error!=null) {
                errorHandler.error(new SAXParseException(
                        error, locator));
                return;
            }
            DBStructure dbStructure = dbData.getConfigurationStructure();
            if(dbStructure==null) {
                errorHandler.error(new SAXParseException(
                        "support " + supportName + " does not use a configuration structure",
                         locator));
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
        private void supportEnd() throws SAXException{
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
        private void arrayStart(Attributes attributes) throws SAXException {
            DBArray dbArray= arrayState.dbArray;
            DBDAttribute dbdAttribute = dbArray.getDBDField().getAttribute();
            Type arrayElementType = dbdAttribute.getElementType();
            DBType arrayElementDBType = dbdAttribute.getElementDBType();
            arrayState.arrayOffset = 0;
            arrayState.arrayElementType = arrayElementType;
            arrayState.arrayElementDBType = arrayElementDBType;
            String supportName = attributes.getValue("supportName");
            if(supportName!=null) dbArray.setSupportName(supportName);
            String value = attributes.getValue("capacity");
            if(value!=null) dbArray.setCapacity(Integer.parseInt(value));
            value = attributes.getValue("length");
            if(value!=null) dbArray.setLength(Integer.parseInt(value));
            switch (arrayElementDBType) {
            case dbPvType:
                if (arrayElementType.isScalar()) {
                } else if (arrayElementType == Type.pvEnum) {
                    arrayState.enumData = new DBEnum[1];
                    arrayState.dbEnumArray = (DBEnumArray)dbArray;
                } else {
                    errorHandler.error(new SAXParseException(
                            " Logic error ArrayHandler", locator));
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
        
        private void startArrayElement(String qName, Attributes attributes) throws SAXException {
            if(!qName.equals("value")) {
                errorHandler.error(new SAXParseException(
                        "arrayStartElement Logic error: expected value",
                         locator));
            }
            String offset = attributes.getValue("offset");
            if(offset!=null) arrayState.arrayOffset = Integer.parseInt(offset);
            int arrayOffset = arrayState.arrayOffset;
            DBArray dbArray = arrayState.dbArray;
            String fieldName = "value";
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
                        errorHandler.error(new SAXParseException(
                                "fieldName " + qName + " illegal type ???",
                                 locator));
                        idleState.prevState = state;
                        state = State.idle;
                        return;
                    }
                    dbdAttribute = DBDAttributeFactory.create(
                         dbd,enumDBDAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBEnumArray dbEnumArray = arrayState.dbEnumArray;
                    DBEnum[] enumData = arrayState.enumData;
                    enumData[0] = (DBEnum)FieldDataFactory.createEnumData(
                        dbArray.getParent(),dbdField,null);
                    dbEnumArray.put(arrayOffset,1,enumData,0);
                    String supportName = attributes.getValue("supportName");
                    if(supportName!=null) enumData[0].setSupportName(supportName);
                    enumState.prevState = state;
                    enumState.dbEnum = enumData[0];
                    enumState.fieldName = fieldName;
                    state = State.enumerated;
                    return;
                }
            case dbMenu: {
                    String menuName = attributes.getValue("menuName");
                    if(menuName==null) {
                        errorHandler.warning(new SAXParseException(
                                "menuName not given",
                                locator));
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new MenuDBDAttributeValues(menuName);
                    dbdAttribute = DBDAttributeFactory.create(
                        dbd,dbdAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBMenuArray dbMenuArray = arrayState.dbMenuArray;
                    DBMenu[] menuData = arrayState.menuData;
                    menuData[0] = (DBMenu)FieldDataFactory.createData(dbArray.getParent(),dbdField);
                    dbMenuArray.put(arrayOffset,1,menuData,0);
                    String supportName = attributes.getValue("supportName");
                    if(supportName!=null) menuData[0].setSupportName(supportName);
                    fieldState.prevState = state;
                    fieldState.dbData = menuData[0];
                    state = State.field;
                    stringBuilder.setLength(0);
                    return;
                }
            case dbStructure: {
                    String structureName = attributes.getValue("structureName");
                    if(structureName==null) {
                        errorHandler.warning(new SAXParseException(
                                "structureName not given",
                                locator));
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDStructure structure = dbd.getStructure(structureName);
                    if(structure==null) {
                        errorHandler.warning(new SAXParseException(
                                "structureName not found",
                                locator));
                        idleState.prevState = state;
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new StructureDBDAttributeValues(structureName,fieldName.toString());
                    dbdAttribute = DBDAttributeFactory.create(
                        dbd,dbdAttributeValues);
                    Property[] property = structure.getPropertys();
                    dbdField = DBDCreateFactory.createField(dbdAttribute,property);
                    DBStructureArray dbStructureArray = arrayState.dbStructureArray;
                    DBStructure[] structureData = arrayState.structureData;
                    structureData[0] = (DBStructure)FieldDataFactory.createData(dbArray.getParent(),dbdField);
                    dbStructureArray.put(arrayOffset,1,structureData,0);
                    String supportName = attributes.getValue("supportName");
                    if(supportName!=null) structureData[0].setSupportName(supportName);
                    structureStack.push(structureState);
                    structureState = new StructureState();
                    structureState.prevState = state;
                    structureState.dbStructure = structureData[0];
                    structureState.fieldName = fieldName;
                    state = State.structure;
                    return;
                }
            case dbArray: {
                    String elementType = attributes.getValue("elementType");
                    if(elementType==null) {
                        errorHandler.warning(new SAXParseException(
                                "elementType not given",
                                locator));
                        state = State.idle;
                    }
                    DBDAttributeValues dbdAttributeValues =
                        new ArrayDBDAttributeValues(elementType);
                    dbdAttribute = DBDAttributeFactory.create(dbd,dbdAttributeValues);
                    dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                    DBArrayArray dbArrayArray = arrayState.dbArrayArray;
                    DBArray[] arrayData = arrayState.arrayData;
                    arrayData[0] = (DBArray)FieldDataFactory.createData(dbArray.getParent(),dbdField);
                    dbArrayArray.put(arrayOffset,1,arrayData,0);
                    String value = attributes.getValue("capacity");
                    if(value!=null) arrayData[0].setCapacity(Integer.parseInt(value));
                    value = attributes.getValue("length");
                    if(value!=null) arrayData[0].setLength(Integer.parseInt(value));
                    String supportName = attributes.getValue("supportName");
                    if(supportName!=null) arrayData[0].setSupportName(supportName);
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
                       dbdAttribute = DBDAttributeFactory.create(
                            dbd,linkDBDAttributeValues);
                       dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                       DBLinkArray dbLinkArray = arrayState.dbLinkArray;
                       DBLink[] linkData = arrayState.linkData;
                       linkData[0] = (DBLink)FieldDataFactory.createData(
                           dbArray.getParent(),dbdField);
                       dbLinkArray.put(arrayOffset,1,linkData,0);
                       String supportName = attributes.getValue("supportName");
                       if(supportName!=null) linkData[0].setSupportName(supportName);
                       fieldState.prevState = state;
                       fieldState.dbData = linkData[0];
                       state = State.field;
                       return;
                }
            }
        }
        
        private void endArrayElement(String qName) throws SAXException {
            if(!qName.equals("value")) {
                errorHandler.error(new SAXParseException(
                        "arrayEndElement Logic error: expected value",
                         locator));
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
                    errorHandler.warning(new SAXParseException(
                        "illegal array element type "
                        + arrayElementType.toString(),
                        locator));
                    return;
                }
                try {
                    int num = convert.fromStringArray(
                        dbArray,arrayOffset,values.length,values,0);
                    arrayOffset += values.length;
                    if(values.length!=num) {
                        errorHandler.warning(new SAXParseException(
                            "not all values were written",
                            locator));
                    }
                } catch (NumberFormatException e) {
                    errorHandler.warning(new SAXParseException(
                        e.toString(),locator));
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
    private static TypeDBDAttributeValues enumDBDAttributeValues =
        new TypeDBDAttributeValues("enum");
    private static TypeDBDAttributeValues linkDBDAttributeValues =
        new TypeDBDAttributeValues("link");

    private static class TypeDBDAttributeValues implements DBDAttributeValues
    {   
        private String type;
        
        TypeDBDAttributeValues(String type) {
            this.type= type;
        }

        public int getLength() {
            return 2;
        }

        public String getName(int index) {
            if(index==0) return "name";
            if(index==1) return "type";
            return null;
        }

        public String getValue(int index) {
            if(index==0) return "null";
            if(index==1) return type;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return "null";
            if(name.equals("type")) return type;
            return null;
        }
 
    }

    private static class StructureDBDAttributeValues
    implements DBDAttributeValues
    {
        private String structureName;
        private String fieldName;

        public StructureDBDAttributeValues(String structureName,
            String fieldName)
        {
            this.structureName = structureName;
            this.fieldName = fieldName;
        }

        public int getLength() {
            return 3;
        }

        public String getName(int index) {
            if(index==0) return "name";
            if(index==1) return "type";
            if(index==2) return "structureName";
            return null;
        }

        public String getValue(int index) {
            if(index==0) return fieldName;
            if(index==1) return "structure";
            if(index==2) return structureName;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return fieldName;
            if(name.equals("type")) return "structure";
            if(name.equals("structureName")) return structureName;
            return null;
        }
    }
    
    private static class ArrayDBDAttributeValues
    implements DBDAttributeValues
    {
        private String elementType;
        
        public ArrayDBDAttributeValues(String elementType) {
            this.elementType = elementType;
        }
        
        public int getLength() {
            return 3;
        }

        public String getName(int index) {
            if(index==0) return "name";
            if(index==1) return "type";
            if(index==2) return "elementType";
            return null;
        }

        public String getValue(int index) {
            if(index==0) return "null";
            if(index==1) return "array";
            if(index==2) return elementType;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return "null";
            if(name.equals("type")) return "array";
            if(name.equals("elementType")) return elementType;
            return null;
        }

    }
    
    private static class MenuDBDAttributeValues
    implements DBDAttributeValues
    {
        private String menuName;

        public MenuDBDAttributeValues(String menuName) {
            this.menuName = menuName;
        }
        
        public int getLength() {
            return 3;
        }

        public String getName(int index) {
            if(index==0) return "name";
            if(index==1) return "type";
            if(index==2) return "menuName";
            return null;
        }

        public String getValue(int index) {
            if(index==0) return "null";
            if(index==1) return "menu";
            if(index==2) return menuName;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return "null";
            if(name.equals("type")) return "menu";
            if(name.equals("menuName")) return menuName;
            return null;
        }

    }
        
}
