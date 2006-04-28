/**
 * 
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


/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToIOCDBFactory {
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbd Database Definition
     * @param iocdbd IOC Database
     * @param fileName filename containing xml record instance definitions
     * @throws MalformedURLException if SAX throws it.
     * @throws IllegalStateException if any errors were detected.
     * @return (true,false) if all xml statements (were, were not) succesfully converted.
     */
    public static void convert(DBD dbdin, IOCDB iocdbin, String fileName)
        throws MalformedURLException//,IllegalStateException
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
            "\n   XMLToIOCDBFactory.convert terminating with MalformedURLException\n   "
            + e.getMessage());
        } catch (SAXException e) {
            throw new IllegalStateException(
                "\n   XMLToIOCDBFactory.convert terminating with SAXException\n   "
                + e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException (
                "\n   XMLToIOCDBFactory.convert terminating with IOException\n   "
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
    private static TypeDBDAttributeValues enumDBDAttributeValues =
        new TypeDBDAttributeValues("enum");
    private static TypeDBDAttributeValues linkDBDAttributeValues =
        new TypeDBDAttributeValues("link");

    private static class TypeDBDAttributeValues implements DBDAttributeValues
    {

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
        
        TypeDBDAttributeValues(String type) {
            this.type= type;
        }
        
        private String type;
    }

    private static class StructureDBDAttributeValues
    implements DBDAttributeValues
    {

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
            if(index==0) return "null";
            if(index==1) return "structure";
            if(index==2) return structureName;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return "null";
            if(name.equals("type")) return "structure";
            if(name.equals("structureName")) return structureName;
            return null;
        }

        public StructureDBDAttributeValues(String structureName) {
            this.structureName = structureName;
        }

        private String structureName;
    }
    
    private static class ArrayDBDAttributeValues
    implements DBDAttributeValues
    {

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

        public ArrayDBDAttributeValues(String elementType) {
            this.elementType = elementType;
        }

        private String elementType;
    }
    
    private static class MenuDBDAttributeValues
    implements DBDAttributeValues
    {

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

        public MenuDBDAttributeValues(String menuName) {
            this.menuName = menuName;
        }

        private String menuName;
    }
        
    private static class Handler  implements ContentHandler, ErrorHandler {
        
        public void warning(SAXParseException e) throws SAXException {
            System.err.printf("warning %s\n",printSAXParseExceptionMessage(e));
            nWarning++;
        }
        public void error(SAXParseException e) throws SAXException {
            System.err.printf("error %s\n",printSAXParseExceptionMessage(e));
            nError++;
        }
        
        public void fatalError(SAXParseException e) throws SAXException {
            System.err.printf("fatal error %s\n",printSAXParseExceptionMessage(e));
            nFatal++;
        }
        
        public void setDocumentLocator(Locator locatorin) {
            errorHandler = this;
            locator = locatorin;
            recordHandler = new RecordHandler();
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
                    state = State.record;
                    recordHandler.start(qName,attributes);
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
                    recordHandler.end(qName);
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

        Handler()  throws MalformedURLException {
        }
        
        private enum State {
            startDocument,
            idle,
            record,
        } 
        private State state = State.startDocument;
        private int nWarning = 0;
        private int nError = 0;
        private int nFatal = 0;
        
        private String printSAXParseExceptionMessage(SAXParseException e)
        {
            return String.format("line %d column %d\nreason %s\n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                e.toString());
        }

        RecordHandler  recordHandler;
    }


    private static class RecordHandler {
    
        void start(String qName, Attributes attributes)
        throws SAXException {
            String recordName = attributes.getValue("name");
            String recordTypeName = attributes.getValue("type");
            if(recordName==null) {
                errorHandler.error(new SAXParseException(
                    "attribute name not specified",locator));
                state = State.idle;
            }
            if(recordTypeName==null) {
                errorHandler.error(new SAXParseException(
                    "attribute type not specified",locator));
                state = State.idle;
            }
            DBDRecordType dbdRecordType = dbd.getDBDRecordType(recordTypeName);
            if(dbdRecordType==null) {
                errorHandler.warning(new SAXParseException(
                    "record type " + recordTypeName + " does not exist.",
                    locator));
                state = State.idle;
            } else {
                DBRecord dbRecord = iocdb.getRecord(recordName);
                if(dbRecord==null) {
                    boolean result = iocdb.createRecord(recordName,dbdRecordType);
                    if(!result) {
                        errorHandler.warning(new SAXParseException(
                                "failed to create record " + recordTypeName,
                                locator));
                        state = State.idle;
                        return;
                    }
                    dbRecord = iocdb.getRecord(recordName);
                }
                state = State.structure;
                structureHandler = new StructureHandler(dbRecord);
            }
        }
    
        void end(String qName) throws SAXException {
            state= State.idle;
            structureHandler = null;
        }
    
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            if(state==State.idle) return;
            structureHandler.startElement(qName,attributes);
        }
 
        void endElement(String qName) throws SAXException {
            if(state==State.idle) return;
            structureHandler.endElement(qName);
        }
        
        void characters(char[] ch, int start, int length)
        throws SAXException {
            if(state==State.idle) return;
            structureHandler.characters(ch,start,length);
        }
        
        private State state = State.idle;
        StructureHandler structureHandler;
        private enum State {idle, structure}
    
    }
    
    private static class  StructureHandler {
        
        void startElement(String qName, Attributes attributes)
        throws SAXException
        {
            switch(state) {
            case idle: 
                 startField(qName,attributes);
                 return;
            case field: 
                errorHandler.error(new SAXParseException(
                    qName + " illegal element",
                    locator));
                return;
            case structure:
                structureHandler.startElement(qName,attributes);
                return;
            case enumerated:
                enumHandler.startElement(qName);
                return;
            case array:
                arrayHandler.startElement(qName,attributes);
                return;
            case link:
                linkHandler.startElement(qName,attributes);
                return;
            }
        }

        void characters(char[] ch, int start, int length) throws SAXException
        {
            if(state==State.field) {
                if(!buildString) return;
                while(start<ch.length && length>0
                && Character.isWhitespace(ch[start])) {
                    start++; length--;
                }
                while(length>0 && Character.isWhitespace(ch[start+ length-1])) {
                    length--;
                }
                if(length<=0) return;
                stringBuilder.append(ch,start,length);
            }
            switch(state) {
            case idle: 
                 return;
            case field: 
                 break;
            case structure:
                structureHandler.characters(ch,start,length);
                return;
            case enumerated:
                enumHandler.characters(ch,start,length);
                return;
            case array:
                arrayHandler.characters(ch,start,length);
                return;
            case link:
                linkHandler.characters(ch,start,length);
                return;
            } 
        }

        void endElement(String qName) throws SAXException
        {
            switch(state) {
            case idle: return;
            case field: 
                endField();
                state = State.idle;
                return;
            case structure:
                if(qName.equals(handlerFieldName)) {
                    state = State.idle;
                    structureHandler = null;
                    return;
                }
                structureHandler.endElement(qName);
                return;
            case enumerated:
                if(qName.equals(handlerFieldName)) {
                    enumHandler.end();
                    state = State.idle;
                    enumHandler  = null;
                    return;
                }
                enumHandler.endElement(qName);
                return;
            case array:
                if(qName.equals(handlerFieldName)) {
                    state = State.idle;
                    arrayHandler = null;
                    return;
                }
                arrayHandler.endElement(qName);
                return;
            case link:
                if(qName.equals(handlerFieldName)) {
                    state = State.idle;
                    linkHandler = null;
                    return;
                }
                linkHandler.endElement(qName);
                return;
            }
        }

        private void startField(String qName, Attributes attributes)
            throws SAXException
        {
            dbDataIndex = dbStructure.getFieldDBDataIndex(qName);
            if(dbDataIndex<0) {
                errorHandler.error(new SAXParseException(
                    "fieldName " + qName + " not found",
                     locator));
                state = State.idle;
                return;
            }
            data = dbData[dbDataIndex];
            dbdField = data.getDBDField();
            dbType = dbdField.getDBType();
            switch(dbType) {
            case dbPvType: {
                Type type= dbdField.getType();
                if(type.isScalar()) {
                    state = State.field;
                    buildString = true;
                    stringBuilder.setLength(0);
                    return;
                }
                if(type!=Type.pvEnum) {
                    errorHandler.error(new SAXParseException(
                            "fieldName " + qName + " illegal type ???",
                             locator));
                    state = State.idle;
                    return;
                }
                state = State.enumerated;
                enumHandler = new EnumHandler((DBEnum)data);
                handlerFieldName = qName;
                return;
            }
            case dbMenu:
                state = State.field;
                stringBuilder.setLength(0);
                buildString = true;
                return;
            case dbStructure:
                state = State.structure;
                structureHandler = new StructureHandler((DBStructure)data);
                handlerFieldName = qName;
                return;
            case dbArray:
                state = State.array;
                handlerFieldName = qName;
                arrayHandler = new ArrayHandler((DBArray)data);
                arrayHandler.start(attributes);
                return;
            case dbLink:
                state = State.link;
                handlerFieldName = qName;
                linkHandler = new LinkHandler((DBLink)data);
                linkHandler.start(attributes);
                return;
            }
        }

        private void endField() throws SAXException {
           if(dbType==DBType.dbPvType) {
                Type type= dbdField.getType();
                if(type.isScalar()) {
                    String value = stringBuilder.toString();
                    if(value!=null) {
                        convert.fromString(data, stringBuilder.toString());
                        try {
                            convert.fromString(data, stringBuilder.toString());
                        } catch (NumberFormatException e) {
                            errorHandler.warning(new SAXParseException(
                                e.toString(),locator));
                        }

                    }
                    return;
                }
                if(type!=Type.pvEnum) {
                    errorHandler.error(new SAXParseException(
                            " Logic Error endField illegal type ???",
                             locator));
                    state = State.idle;
                    return;
                }
                enumHandler.end();
                return;
            }
            if(dbType==DBType.dbMenu) {
                String value = stringBuilder.toString();
                DBMenu menu = (DBMenu)dbData[dbDataIndex];
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
            }
            errorHandler.error(new SAXParseException(
                "Logic error in endField",
                locator));
        }

        StructureHandler(DBStructure dbStructure)
        {
            this.dbStructure = dbStructure;
            dbData = dbStructure.getFieldDBDatas();
            state = State.idle;
            stringBuilder = new StringBuilder();
        }
        
        private DBStructure dbStructure;
        private DBData[] dbData;
        private int dbDataIndex;
        private State state;
        
        // following are for State.field
        private boolean buildString;
        private StringBuilder stringBuilder;
        private DBData data;
        private DBDField dbdField;
        private DBType dbType;
        
        private String handlerFieldName = null;
        private EnumHandler enumHandler;
        private StructureHandler structureHandler;
        private ArrayHandler arrayHandler;
        private LinkHandler linkHandler;
        private enum State {idle, field, enumerated, structure, array, link}

    }
    
    private static class  EnumHandler {

        void end() throws SAXException
        {
            if(state==State.idle) return;
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
         
        void startElement(String qName)
        throws SAXException
        {
            if(!qName.equals("choice")) {
                errorHandler.error(new SAXParseException(
                    qName + " illegal. Only choice is valid.",
                    locator));
                    state = State.idle;
                    return;
            }
            if(stringBuilder.length()>0) {
                value = value + stringBuilder.toString();
                stringBuilder.setLength(0);
            }
            state = State.choice;
        }
        
        void characters(char[] ch, int start, int length)
        throws SAXException {
            if(state==State.idle) return;
            while(start<ch.length && length>0
            && Character.isWhitespace(ch[start])) {
                start++; length--;
            }
            while(length>0 && Character.isWhitespace(ch[start+ length-1])) {
                length--;
            }
            if(length<=0) return;
            stringBuilder.append(ch,start,length);
        }
        
        void endElement(String qName) throws SAXException {
            if(state!=State.choice) return;
            enumChoiceList.add(stringBuilder.toString());
            stringBuilder.setLength(0);
            state = State.enumerated;
        }

        EnumHandler(DBEnum dbEnum)
        {
            super();
            this.dbEnum = dbEnum;
            stringBuilder = new StringBuilder();
            enumChoiceList = new LinkedList<String>();
            state = State.enumerated;
        }
        
        private DBEnum dbEnum;
        private enum State {idle,enumerated,choice}
        private StringBuilder stringBuilder;
        private LinkedList<String> enumChoiceList;
        private State state;
        private String value = "";
    }


    private static class  ArrayHandler {
        
        void start(Attributes attributes) {
            String value = attributes.getValue("capacity");
            if(value!=null)
                dbArray.setCapacity(Integer.parseInt(value));
            value = attributes.getValue("length");
            if(value!=null)
                dbArray.setLength(Integer.parseInt(value));
        }

        void startElement(String qName, Attributes attributes)
            throws SAXException
        { 
            if(state==State.idle) return;
            if(qName.equals("value")) {
                valueLevel++;
                if(valueLevel==1) {
                    startField(qName,attributes);
                    return;
                }
            }
            switch(handlerType) {
            case none: 
                 return;
            case structure:
                if(structureHandler==null) {
                    errorHandler.error(new SAXParseException(
                        qName + " illegal element",locator));
                    state = State.idle;
                    return;
                }
                structureHandler.startElement(qName,attributes);
                return;
            case enumerated:
                if(enumHandler==null) {
                    errorHandler.error(new SAXParseException(
                        qName + " illegal element",locator));
                    state = State.idle;
                    return;
                }
                enumHandler.startElement(qName);
                return;
            case array:
                if(arrayHandler==null) {
                    errorHandler.error(new SAXParseException(
                        qName + " illegal element",locator));
                    state = State.idle;
                    return;
                }
                arrayHandler.startElement(qName,attributes);
                return;
            case link:
                if(linkHandler==null) {
                    errorHandler.error(new SAXParseException(
                        qName + " illegal element",locator));
                    state = State.idle;
                    return;
                }
                linkHandler.startElement(qName,attributes);
                return;
            }
        }

        void characters(char[] ch, int start, int length) throws SAXException
        {
            if(handlerType==HandlerType.none || handlerType==HandlerType.menu) {
                if(!buildString) return;
                while(start<ch.length && length>0
                && Character.isWhitespace(ch[start])) {
                    start++; length--;
                }
                while(length>0 && Character.isWhitespace(ch[start+ length-1])) {
                    length--;
                }
                if(length<=0) return;
                stringBuilder.append(ch,start,length);
                return;
            }
            switch(handlerType) {
            case none: return;
            case structure:
                if(structureHandler!=null)
                    structureHandler.characters(ch,start,length);
                return;
            case enumerated:
                if(enumHandler!=null)
                    enumHandler.characters(ch,start,length);
                return;
            case array:
                if(arrayHandler!=null)
                    arrayHandler.characters(ch,start,length);
                return;
            case link:
                if(linkHandler!=null)
                    linkHandler.characters(ch,start,length);
                return;
            }
        }
        
        void endElement(String qName) throws SAXException
        {
            if(state==State.idle) return;
            if(qName.equals("value")) {
                valueLevel--;
                if(valueLevel==0) {
                    switch(handlerType) {
                    case none:
                        endField();
                        return;
                    case structure:
                        arrayOffset += 1;
                        return;
                    case enumerated:
                        enumHandler.end();
                        return;
                    case menu: {
                        buildString = false;
                        arrayOffset += 1;
                        String value = stringBuilder.toString();
                        if(value==null || value.length()==0) return;
                        String[]choice = menuData[0].getChoices();
                        for(int i = 0; i<choice.length; i++ ) {
                            if(value.equals(choice[i])) {
                                menuData[0].setIndex(i);
                                return;
                            }
                        }
                        errorHandler.error(new SAXParseException(
                            value + " not a menu choice",locator));
                        return;
                    }
                    case array:
                        arrayOffset += 1;
                        return;
                    case link:
                        arrayOffset += 1;
                        return;
                    }
                    return;
                }
            }
            switch(handlerType) {
            case none: return;
            case structure:
                structureHandler.endElement(qName);
                return;
            case enumerated:
                enumHandler.endElement(qName);
                return;
            case array:
                arrayHandler.endElement(qName);
                return;
            case link:
                linkHandler.endElement(qName);
                return;
            }
        }

        private void startField(String qName, Attributes attributes)
            throws SAXException
        {
            String offsetString = attributes.getValue("offset");
            if(offsetString!=null) {
                arrayOffset = Integer.parseInt(offsetString);
            }
            DBDAttribute dbdAttribute;
            DBDField dbdField;
            switch(handlerType) {
            case none: 
                buildString = true;
                stringBuilder.setLength(0);
                return;
            case enumerated:
                dbdAttribute = DBDAttributeFactory.create(
                     dbd,enumDBDAttributeValues);
                dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
                enumData[0] = (DBEnum)FieldDataFactory.createEnumData(
                    dbdField,null);
                dbEnumArray.put(arrayOffset,1,enumData,0);
                enumHandler = new EnumHandler(enumData[0]);
                return;
            case menu: {
                String menuName = attributes.getValue("menuName");
                if(menuName==null) {
                    errorHandler.warning(new SAXParseException(
                            "menuName not given",
                            locator));
                    state = State.idle;
                }
                DBDAttributeValues dbdAttributeValues =
                    new MenuDBDAttributeValues(menuName);
                dbdAttribute = DBDAttributeFactory.create(
                    dbd,dbdAttributeValues);
                dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
                menuData[0] = (DBMenu)FieldDataFactory.
                    createData(dbdField);
                dbMenuArray.put(arrayOffset,1,menuData,0);
                buildString = true;
                stringBuilder.setLength(0);
                return;
            }
            case structure: {
                String structureName = attributes.getValue("structureName");
                if(structureName==null) {
                    errorHandler.warning(new SAXParseException(
                            "structureName not given",
                            locator));
                    state = State.idle;
                }
                DBDAttributeValues dbdAttributeValues =
                    new StructureDBDAttributeValues(structureName);
                dbdAttribute = DBDAttributeFactory.create(
                    dbd,dbdAttributeValues);
                dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
                structureData[0] = (DBStructure)FieldDataFactory.createData(dbdField);
                dbStructureArray.put(arrayOffset,1,structureData,0);
                structureHandler = new StructureHandler(structureData[0]);
                return;
            }
            case array: {
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
                dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
                arrayData[0] = (DBArray)FieldDataFactory.createData(dbdField);
                dbArrayArray.put(arrayOffset,1,arrayData,0);
                arrayHandler = new ArrayHandler(arrayData[0]);
                arrayHandler.start(attributes);
                return;
            }
            case link:
                dbdAttribute = DBDAttributeFactory.create(
                        dbd,linkDBDAttributeValues);
                dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
                linkData[0] = (DBLink)FieldDataFactory.createData(dbdField);
                dbLinkArray.put(arrayOffset,1,linkData,0);
                linkHandler = new LinkHandler(linkData[0]);
                linkHandler.start(attributes);
                return;
            }
        }

        private void endField() throws SAXException {
            String value = stringBuilder.toString();
            buildString = false;
            String[] values = null;
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

        }

        ArrayHandler(DBArray dbArray) throws SAXException
        {
            this.dbArray = dbArray;
            DBDAttribute dbdAttribute = dbArray.getDBDField().getDBDAttribute();
            arrayElementType = dbdAttribute.getElementType();
            arrayElementDBType = dbdAttribute.getElementDBType();
            switch(arrayElementDBType) {
            case dbPvType:
                if(arrayElementType.isScalar()) {
                    handlerType = HandlerType.none;
                } else if(arrayElementType==Type.pvEnum) {
                    handlerType = HandlerType.enumerated;
                    enumData = new DBEnum[1];
                    dbEnumArray = (DBEnumArray)dbArray;
                } else {
                    errorHandler.error(new SAXParseException(
                        " Logic error ArrayHandler",
                        locator));
                }
                break;
            case dbMenu:
                handlerType = HandlerType.menu;
                menuData = new DBMenu[1];
                dbMenuArray = (DBMenuArray)dbArray;
                break;
            case dbStructure:
                handlerType = HandlerType.structure;
                structureData = new DBStructure[1];
                dbStructureArray = (DBStructureArray)dbArray;
                break;
            case dbArray:
                handlerType = HandlerType.array;
                arrayData = new DBArray[1];
                dbArrayArray = (DBArrayArray)dbArray;
                break;
            case dbLink:
                handlerType = HandlerType.link;
                linkData = new DBLink[1];
                dbLinkArray = (DBLinkArray)dbArray;
                break;
            }
            state = State.processing;
            arrayOffset = 0;
            valueLevel = 0;
            stringBuilder = new StringBuilder();
        }
        
        private DBArray dbArray;
        private Type arrayElementType;
        private DBType arrayElementDBType;
        private int arrayOffset;
        private int valueLevel;

        private HandlerType handlerType;
        private State state;
        
        private boolean buildString;
        private StringBuilder stringBuilder;
        
        private EnumHandler enumHandler;
        private DBEnum[] enumData;
        private DBEnumArray dbEnumArray;
        
        private DBMenu[] menuData;
        private DBMenuArray dbMenuArray;

        private StructureHandler structureHandler;
        private DBStructure[] structureData;
        private DBStructureArray dbStructureArray;

        private ArrayHandler arrayHandler;
        private DBArray[] arrayData;
        private DBArrayArray dbArrayArray;

        private LinkHandler linkHandler;
        private DBLink[] linkData;
        private DBLinkArray dbLinkArray;

        private enum HandlerType {none,enumerated,menu, structure, array, link}
        private enum State {idle, processing}

    }
    
    private static class  LinkHandler {
        void start(Attributes attributes) throws SAXException
        {
            String linkSupportName = attributes.getValue("linkSupportName");
            if(linkSupportName==null) {
                errorHandler.warning(new SAXParseException(
                        "linkSupportName not defined",
                        locator));
                return;
            }
            DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(linkSupportName);
            if(dbdLinkSupport==null) {
                errorHandler.warning(new SAXParseException(
                        "linkSupport does not exist",
                        locator));
                return;
            }
            String configStructureName = attributes.getValue("configStructureName");
            if(configStructureName==null) {
                errorHandler.warning(new SAXParseException(
                        "configStructureName not defined",
                        locator));
                return;
            }
            String supportConfigName = dbdLinkSupport.getConfigStructureName();
            if(!configStructureName.equals(supportConfigName)) {
                errorHandler.warning(new SAXParseException(
                        "configStructureName must be " + supportConfigName,
                        locator));
                return;
            }
            DBDStructure dbdStructure = dbd.getDBDStructure(configStructureName);
            if(dbdStructure==null) {
                errorHandler.warning(new SAXParseException(
                        "configuration structure does not exist",
                        locator));
                return;
            }
            dbLink.putLinkSupportName(linkSupportName);
            dbLink.putConfigStructureName(configStructureName);
            DBDAttributeValues dbdAttributeValues =
                new StructureDBDAttributeValues(configStructureName);
            DBDAttribute dbdAttribute = DBDAttributeFactory.create(
                dbd,dbdAttributeValues);
            DBDField dbdField = DBDCreateFactory.createDBDField(dbdAttribute,null);
            configDBStructure = (DBStructure)FieldDataFactory.createData(dbdField);
            dbLink.putConfigDBStructure(configDBStructure);
            structureHandler = new StructureHandler(configDBStructure);
            state = State.structure;
        }

        void startElement(String qName, Attributes attributes)
            throws SAXException
        {
            if(state!=State.structure) return;
            structureHandler.startElement(qName,attributes);
        }

        void characters(char[] ch, int start, int length) throws SAXException
        {
            if(state!=State.structure) return;
            structureHandler.characters(ch,start,length);
        }

        void endElement(String qName) throws SAXException
        {
            if(state!=State.structure) return;
            structureHandler.endElement(qName);
        }

        LinkHandler(DBLink dbLink)
        {
            super();
            this.dbLink = dbLink;
            state = State.idle;
        }
        
        private DBLink dbLink;
        private DBStructure configDBStructure = null;
        private StructureHandler structureHandler = null;
        private State state;
        private enum State {idle, structure}

    }
    
}
