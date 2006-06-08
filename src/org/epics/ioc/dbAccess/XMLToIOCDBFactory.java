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
import java.lang.reflect.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Type;
import org.epics.ioc.dbProcess.*;


/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToIOCDBFactory {
    /**
     * Convert an xml file to IOCDatabase definitions and put the definitions in a database.
     * @param dbdin reflection database.
     * @param iocdbin IOC database.
     * @param fileName filename containing xml record instance definitions.
     * @throws MalformedURLException if SAX throws it.
     * @throws IllegalStateException if any errors were detected.
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

        Handler()  throws MalformedURLException {
        }
        
        private String printSAXParseExceptionMessage(SAXParseException e)
        {
            return String.format("line %d column %d\nreason %s\n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                e.toString());
        }

        RecordHandler  recordHandler;
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

    }


    private static class RecordHandler
    {
        private State state = State.idle;
        StructureHandler structureHandler;
        private enum State {idle, structure}
    
        void start(String qName, Attributes attributes)
        throws SAXException {
            DBRecord dbRecord = null;
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
            DBDRecordType dbdRecordType = dbd.getRecordType(recordTypeName);
            if(dbdRecordType==null) {
                errorHandler.warning(new SAXParseException(
                    "record type " + recordTypeName + " does not exist.",
                    locator));
                state = State.idle;
            } else {
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
                }
                state = State.structure;
                structureHandler = new StructureHandler(dbRecord);
            }
            String supportName = attributes.getValue("recordSupport");
            if(supportName==null) {
                supportName = dbdRecordType.getRecordSupportName();
            }
            if(supportName==null) {
                errorHandler.warning(new SAXParseException(
                        "record  " + recordName + " has no record support",
                        locator));
                return;
            }
            Class supportClass;
            RecordSupport recordSupport = null;
            Class[] argClass = null;
            Object[] args = null;
            Constructor constructor = null;
            try {
                supportClass = Class.forName(supportName);
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "recordSupport " + supportName + " does not exist",
                        locator));
                return;
            }
            try {
                argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBRecord")};
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "class DBRecord " + " does not exist??? Why??",
                        locator));
                return;
            }
            try {
                args = new Object[] {dbRecord};
                constructor = supportClass.getConstructor(argClass);
            } catch (NoSuchMethodException e) {
                errorHandler.warning(new SAXParseException(
                        "recordSupport " + supportName + " does not have a valid constructor",
                        locator));
                return;
            }
            try {
                recordSupport = (RecordSupport)constructor.newInstance(args);
            } catch(Exception e) {
                errorHandler.warning(new SAXParseException(
                        "recordSupport " + supportName + " not found",
                        locator));
                return;
            }
            dbRecord.setRecordSupport(recordSupport);
            
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
 
    }
    
    private static class  StructureHandler
    {        
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
        private int handlerFieldNameLevel = 0;
        private EnumHandler enumHandler;
        private StructureHandler structureHandler;
        private ArrayHandler arrayHandler;
        private LinkHandler linkHandler;
        private enum State {idle, field, enumerated, structure, array, link}

        StructureHandler(DBStructure dbStructure)
        {
            this.dbStructure = dbStructure;
            dbData = dbStructure.getFieldDBDatas();
            state = State.idle;
            stringBuilder = new StringBuilder();
        }
        
        void startElement(String qName, Attributes attributes)
        throws SAXException
        {
            if(handlerFieldName!=null && qName.equals(handlerFieldName)) {
                handlerFieldNameLevel++;
            }
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
                    if(handlerFieldNameLevel>0) {
                        handlerFieldNameLevel--;
                    } else {
                        state = State.idle;
                        structureHandler = null;
                        return;
                    }
                }
                structureHandler.endElement(qName);
                return;
            case enumerated:
                if(qName.equals(handlerFieldName)) {
                    if(handlerFieldNameLevel>0) {
                        handlerFieldNameLevel--;
                    } else {
                        enumHandler.end();
                        state = State.idle;
                        enumHandler  = null;
                        return;
                    }
                }
                enumHandler.endElement(qName);
                return;
            case array:
                if(qName.equals(handlerFieldName)) {
                    if(handlerFieldNameLevel>0) {
                        handlerFieldNameLevel--;
                    } else {
                        state = State.idle;
                        arrayHandler = null;
                        return;
                    }
                }
                arrayHandler.endElement(qName);
                return;
            case link:
                if(qName.equals(handlerFieldName)) {
                    if(handlerFieldNameLevel>0) {
                        handlerFieldNameLevel--;
                    } else {
                        state = State.idle;
                        linkHandler = null;
                        return;
                    }
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
                structureHandler.lookForSupport(attributes);
                handlerFieldName = qName;
                return;
            case dbArray:
                state = State.array;
                handlerFieldName = qName;
                arrayHandler = new ArrayHandler(dbStructure,(DBArray)data);
                arrayHandler.start(attributes);
                return;
            case dbLink:
                state = State.link;
                handlerFieldName = qName;
                linkHandler = new LinkHandler(dbStructure,(DBLink)data);
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

        private void lookForSupport(Attributes attributes) throws SAXException
        {
            String supportName = attributes.getValue("structureSupport");

            if(supportName==null) {
                DBDStructureField dbdStructureField = (DBDStructureField)dbStructure.getDBDField();
                DBDStructure dbdStructure = dbdStructureField.getDBDStructure();
                supportName = dbdStructure.getStructureSupportName();
            }
            if(supportName==null) return;
            Class supportClass;
            RecordSupport recordSupport = null;
            Class[] argClass = null;
            Object[] args = null;
            Constructor constructor = null;
            try {
                supportClass = Class.forName(supportName);
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "structureSupport " + supportName + " does not exist",
                        locator));
                return;
            }
            try {
                argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBStructure")};
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "class DBStructure " + " does not exist??? Why??",
                        locator));
                return;
            }
            try {
                args = new Object[] {dbStructure};
                constructor = supportClass.getConstructor(argClass);
            } catch (NoSuchMethodException e) {
                errorHandler.warning(new SAXParseException(
                        "structureSupport " + supportName + " does not have a valid constructor",
                        locator));
                return;
            }
            try {
                recordSupport = (RecordSupport)constructor.newInstance(args);
            } catch(Exception e) {
                errorHandler.warning(new SAXParseException(
                        "structureSupport " + supportName + " not found",
                        locator));
                return;
            }
            dbStructure.setStructureSupport(recordSupport);
        }
    }
    
    private static class  EnumHandler
    {
        private DBEnum dbEnum;
        private enum State {idle,enumerated,choice}
        private StringBuilder stringBuilder;
        private LinkedList<String> enumChoiceList;
        private State state;
        private String value = "";
        
        EnumHandler(DBEnum dbEnum)
        {
            super();
            this.dbEnum = dbEnum;
            stringBuilder = new StringBuilder();
            enumChoiceList = new LinkedList<String>();
            state = State.enumerated;
        }
        
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
    }

    private static class  ArrayHandler
    {
        private DBStructure parent;
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
        
        ArrayHandler(DBStructure parent,DBArray dbArray) throws SAXException
        {
            this.parent = parent;
            this.dbArray = dbArray;
            DBDAttribute dbdAttribute = dbArray.getDBDField().getAttribute();
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
                dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                enumData[0] = (DBEnum)FieldDataFactory.createEnumData(
                    parent,dbdField,null);
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
                dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                menuData[0] = (DBMenu)FieldDataFactory.createData(parent,dbdField);
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
                DBDStructure structure = dbd.getStructure(structureName);
                if(structure==null) {
                    errorHandler.warning(new SAXParseException(
                            "structureName not found",
                            locator));
                    state = State.idle;
                }
                StringBuilder fieldName = new StringBuilder(dbArray.getField().getName());
                fieldName.append('[');
                fieldName.append(String.valueOf(arrayOffset));
                fieldName.append(']');
                DBDAttributeValues dbdAttributeValues =
                    new StructureDBDAttributeValues(structureName,fieldName.toString());
                dbdAttribute = DBDAttributeFactory.create(
                    dbd,dbdAttributeValues);
                
                Property[] property = structure.getPropertys();
                dbdField = DBDCreateFactory.createField(dbdAttribute,property);
                structureData[0] = (DBStructure)FieldDataFactory.createData(parent,dbdField);
                dbStructureArray.put(arrayOffset,1,structureData,0);
                structureHandler = new StructureHandler(structureData[0]);
                structureHandler.lookForSupport(attributes);
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
                dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                arrayData[0] = (DBArray)FieldDataFactory.createData(parent,dbdField);
                dbArrayArray.put(arrayOffset,1,arrayData,0);
                arrayHandler = new ArrayHandler(parent,arrayData[0]);
                arrayHandler.start(attributes);
                return;
            }
            case link:
                dbdAttribute = DBDAttributeFactory.create(
                        dbd,linkDBDAttributeValues);
                dbdField = DBDCreateFactory.createField(dbdAttribute,null);
                linkData[0] = (DBLink)FieldDataFactory.createData(parent,dbdField);
                dbLinkArray.put(arrayOffset,1,linkData,0);
                linkHandler = new LinkHandler(parent,linkData[0]);
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
    }
    
    private static class  LinkHandler
    {

        private DBStructure parent;
        private DBLink dbLink;
        private DBStructure configDBStructure = null;
        private StructureHandler structureHandler = null;
        private State state;
        private enum State {idle, structure}
        
        LinkHandler(DBStructure parent,DBLink dbLink)
        {
            super();
            this.parent = parent;
            this.dbLink = dbLink;
            state = State.idle;
        }
        
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
            DBDStructure dbdStructure = dbd.getStructure(configStructureName);
            if(dbdStructure==null) {
                errorHandler.warning(new SAXParseException(
                        "configuration structure does not exist",
                        locator));
                return;
            }
            dbLink.putLinkSupportName(linkSupportName);
            dbLink.putConfigStructureName(configStructureName);
            DBDAttributeValues dbdAttributeValues =
                new StructureDBDAttributeValues(configStructureName,"configStructure");
            DBDAttribute dbdAttribute = DBDAttributeFactory.create(
                dbd,dbdAttributeValues);
            DBDField dbdField = DBDCreateFactory.createField(
                dbdAttribute,dbLink.getField().getPropertys());
            configDBStructure = (DBStructure)FieldDataFactory.createData(parent,dbdField);
            dbLink.putConfigStructure(configDBStructure);
            structureHandler = new StructureHandler(configDBStructure);
            state = State.structure;
            Class supportClass;
            LinkSupport linkSupport = null;
            Class[] argClass = null;
            Object[] args = null;
            Constructor constructor = null;
            try {
                supportClass = Class.forName(linkSupportName);
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "linkSupport " + linkSupportName + " does not exist",
                        locator));
                return;
            }
            try {
                argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBLink")};
            }catch (ClassNotFoundException e) {
                errorHandler.warning(new SAXParseException(
                        "class DBLink " + " does not exist??? Why??",
                        locator));
                return;
            }
            try {
                args = new Object[] {dbLink};
                constructor = supportClass.getConstructor(argClass);
            } catch (NoSuchMethodException e) {
                errorHandler.warning(new SAXParseException(
                        "recordSupport " + linkSupportName + " does not have a valid constructor",
                        locator));
                return;
            }
            try {
                linkSupport = (LinkSupport)constructor.newInstance(args);
            } catch(Exception e) {
                errorHandler.warning(new SAXParseException(
                        "linkSupport " + linkSupportName + " not found",
                        locator));
                return;
            }
            dbLink.setLinkSupport(linkSupport);
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
    }
    
}
