/**
 * 
 */
package org.epics.ioc.dbDefinition;

import java.io.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.epics.ioc.pvAccess.*;

import java.util.*;

public class XMLToDBD {
    public static void convert(DBD dbd, String fileName)
        throws MalformedURLException
    {
        String uri = new File(fileName).toURL().toString();
        XMLReader reader;
        
        Handler handler = new Handler(dbd);
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(uri);
        } catch (SAXException e) {
            System.err.println(
                "XMLToDBD.convert terminating with SAXException "
                + e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println (
                "XMLToDBD.convert terminating with IOException "
                + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println (
                "XMLToDBD.convert terminating with Exception "
                + e.getMessage());
            return;
        }
    }

    private static class Handler  implements ContentHandler, ErrorHandler {

        public void warning(SAXParseException e) throws SAXException {
            System.out.printf("warning %s\n",printSAXParseExceptionMessage(e));
        }
        public void error(SAXParseException e) throws SAXException {
            System.out.printf("error %s\n",printSAXParseExceptionMessage(e));
        }
        
        public void fatalError(SAXParseException e) throws SAXException {
            System.out.printf("fatal error %s\n",printSAXParseExceptionMessage(e));
        }
        
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
            menuHandler = new DBDXMLMenuHandler(dbd,this,locator);
            structureHandler = new DBDXMLStructureHandler(dbd,this,locator);
        }
        
        public void startDocument() throws SAXException {
            state = State.startDocument;
        }
        
        
        public void endDocument() throws SAXException {
            // nothing to do.
        }       

        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
        {
            switch(state) {
            case startDocument:
                if(qName.equals("DBDefinition")) state = State.idle;
                break;
            case idle:
                if(qName.equals("menu")) {
                    state = State.menu;
                    menuHandler.start(qName,attributes);
                } else if(qName.equals("structure")) {
                    state = State.structure;
                    structureHandler.start(qName,attributes);
                } else if(qName.equals("recordType")) {
                    state = State.structure;
                    structureHandler.start(qName,attributes);
                } else if(qName.equals("linkSupport")){
                    state = State.linkSupport;
                } else {
                    throw new SAXParseException(
                        "expected menu or structure or recordType",locator);
                }
                break;
            case menu: 
                menuHandler.startElement(qName,attributes);
                break;
            case structure:
                // no break. structure and recordType handled by structureHandler
            case recordType:
                structureHandler.startElement(qName,attributes);
                break;
            case linkSupport:
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
                if(qName.equals("DBDefinition")) {
                    state = State.startDocument;
                } else {
                    throw new SAXParseException(
                        "endElement: Logic error state idle",locator);
                }
                break;
            case menu: 
                if(qName.equals("menu")) {
                    menuHandler.end(qName);
                    state = State.idle;
                } else {
                    menuHandler.endElement(qName);
                }
                break;
            case structure:
            case recordType:
                if(qName.equals("structure")
                || qName.equals("recordType")) {
                    structureHandler.end(qName);
                    state = State.idle;
                } else {
                    structureHandler.endElement(qName);
                }
                break;
            case linkSupport:
                if(qName.equals("linkSupport")) {
                    state = State.idle;
                }
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
            case menu: 
                menuHandler.characters(ch,start,length);
                break;
            case structure:
            case recordType:
                structureHandler.characters(ch,start,length);
                break;
            case linkSupport:
                break;
            }
        }
 
        
        Handler(DBD dbd)  throws MalformedURLException {
            this.dbd = dbd;
            
        }
        
        public void endPrefixMapping(String prefix) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
        {
            // TODO Auto-generated method stub
            
        }

        public void processingInstruction(String target, String data)
        throws SAXException
        {
            // TODO Auto-generated method stub
            
        }

        public void skippedEntity(String name) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void startPrefixMapping(String prefix, String uri)
        throws SAXException
        {
            // TODO Auto-generated method stub
            
        }
        
        private enum State {
            startDocument,
            idle,
            menu,
            structure,
            recordType,
            linkSupport
        } 
        private DBD dbd;
        private Locator locator = null;
        private State state = State.startDocument;
        
        private String printSAXParseExceptionMessage(SAXParseException e)
        {
            return String.format("line %d column %d\nreason %s\n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                e.toString());
        }

        DBDXMLHandler menuHandler;
        DBDXMLHandler structureHandler;
    }


    private static abstract class  DBDXMLHandler {
        abstract void start(String qName, Attributes attributes)
            throws SAXException;
        abstract void end(String qName) throws SAXException;
        abstract void startElement(String qName, Attributes attributes)
            throws SAXException;
        abstract void characters(char[] ch, int start, int length)
            throws SAXException;
        abstract void endElement(String qName) throws SAXException;
    }

    private static class DBDXMLMenuHandler extends DBDXMLHandler{
    
        void start(String qName, Attributes attributes)
        throws SAXException {
            menuName = attributes.getValue("name");
            if(menuName==null) {
                errorHandler.error(new SAXParseException(
                    "attribute name not specified",locator));
                state = State.idle;
            }
            if(dbd.getMenu(menuName)!=null) {
                errorHandler.warning(new SAXParseException(
                    "menu " + menuName + " ignored because it already exists",
                    locator));
                state = State.idle;
            } else {
                choiceList = new LinkedList<String>();
                state = State.nextChoice;
            }
        }
    
        void end(String qName) throws SAXException {
            if(state==State.idle) return;
            if(state!=State.nextChoice) {
                errorHandler.error(new SAXParseException(
                    "Logic error in DBDXMLMenuHandler.end"
                    + " state should be nextChoice",locator));
                state = State.idle;
                return;
            }
            if(menuName==null || menuName.length()==0
            || choiceList==null || choiceList.size()==0) {
                errorHandler.error(new SAXParseException(
                        "menu definition is not complete",locator));
            } else {
                String[] choice = new String[choiceList.size()];
                ListIterator<String> iter = choiceList.listIterator();
                for(int i=0; i<choice.length; i++) {
                    choice[i] = iter.next();
                }
                choiceList = null;
                DBDMenu dbdMenu = DBDCreateFactory.createDBDMenu(
                    menuName,choice);
                dbd.addMenu(dbdMenu);
            }
            state= State.idle;
        }
    
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            if(state==State.idle) return;
            if(state!=State.nextChoice) {
                errorHandler.error(new SAXParseException(
                        "Logic error in DBDXMLMenuHandler.startElement"
                        + "state should be nextChoice",locator));
                state = State.idle;
                return;
            }
            if(!qName.equals("choice")) {
                errorHandler.error(new SAXParseException(
                        "illegal element. only choice is allowed",locator));
                state = State.idle;
                return;
            }
            state = State.getChoice;
            choiceBuilder = new StringBuilder();
        }
    
        void endElement(String qName) throws SAXException {
            if(state==State.idle) return;
            if(state!=State.getChoice) {
                errorHandler.error(new SAXParseException(
                        "Logic error in DBDXMLMenuHandler.startElement"
                        + "state should be nextChoice",locator));
                state = State.idle;
                return;
            }
            String newChoice = choiceBuilder.toString();
            if(newChoice.length()<=0) {
                errorHandler.error(new SAXParseException(
                        "illegal choice",locator));
                    state = State.idle;
                    return;
            }
            choiceList.add(choiceBuilder.toString());
            choiceBuilder = null;
            state = State.nextChoice;
        }
    
        void characters(char[] ch, int start, int length)
        throws SAXException {
            if(state!=State.getChoice) return;
            while(start<ch.length && length>0 && ch[start]==' ') {
                start++; length--;
            }
            while(length>0 && ch[start+ length-1]==' ') length--;
            if(length<=0) return;
            choiceBuilder.append(ch,start,length);
        }
        
        DBDXMLMenuHandler(DBD dbd, ErrorHandler errorHandler, Locator locator) {
            super();
            this.dbd = dbd;
            this.errorHandler = errorHandler;
            this.locator = locator;
        }
        
        private DBD dbd;
        ErrorHandler errorHandler;
        Locator locator;
        State state = State.idle;
        String menuName;
        LinkedList<String> choiceList;
        StringBuilder choiceBuilder;
        
        private enum State {idle, nextChoice, getChoice}
    
    }

    private static class DBDXMLStructureHandler extends DBDXMLHandler{
    
        void start(String qName, Attributes attributes)
        throws SAXException {
            if(state!=State.idle) {
                errorHandler.error(new SAXParseException(
                   "DBDXMLStructureHandler.start logic error not idle",
                   locator));
                state = State.idle;
                return;
            }
            structureName = attributes.getValue("name");
            if(structureName==null || structureName.length() == 0) {
                errorHandler.error(new SAXParseException(
                    "attribute name not specified",locator));
                state = State.idle;
                return;
            }
            if(qName.equals("recordType")) {
                if(dbd.getDBDRecordType(structureName)!=null) {
                    errorHandler.warning(new SAXParseException(
                        "recordType " + structureName + " already exists",
                        locator));
                    state = State.idle;
                    return;
                }
                isRecordType = true;
            } else if(qName.equals("structure")){
                if(dbd.getDBDStructure(structureName)!=null) {
                    errorHandler.warning(new SAXParseException(
                        "structure " + structureName + " already exists",
                        locator));
                    state = State.idle;
                    return;
                }
                isRecordType = false;
            } else {
                errorHandler.error(new SAXParseException(
                        "DBDXMLStructureHandler.start logic error",locator));
                state = State.idle;
                return;
            }
            structurePropertyList = new LinkedList<Property>();
            dbdFieldList = new LinkedList<DBDField>();
            state = State.structure;
        }
    
        void end(String qName) throws SAXException {
            if(state==State.idle) {
                structurePropertyList = null;
                dbdFieldList = null;
                return;
            }
            if(dbdFieldList.size()==0) {
                errorHandler.error(new SAXParseException(
                   "DBDXMLStructureHandler.end no fields were defined",
                   locator));
                state = State.idle;
                structurePropertyList = null;
                dbdFieldList = null;
                return;
            }
            Property[] property = new Property[structurePropertyList.size()];
            ListIterator<Property> iter = structurePropertyList.listIterator();
            for(int i=0; i<property.length; i++) {
                property[i] = iter.next();
            }
            DBDField[] dbdField = new DBDField[dbdFieldList.size()];
            ListIterator<DBDField> iter1 = dbdFieldList.listIterator();
            for(int i=0; i<dbdField.length; i++) {
                dbdField[i] = iter1.next();
            }
            DBDStructure dbdStructure = DBDCreateFactory.createDBDStructure(
                structureName,dbdField,property);
            if(isRecordType) {
                dbd.addRecordType(dbdStructure);
            } else {
                dbd.addStructure(dbdStructure);
            }
            structurePropertyList = null;
            dbdFieldList = null;
            state= State.idle;
        }
     
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            if(state==State.idle) return;
            if(qName.equals("field")) {
                assert(state==State.structure);
                getCommonAttributes(attributes);
                fieldPropertyList = new LinkedList<Property>();
                state = State.field;
                gettingProperties = true;
                if(fieldType==Type.pvUnknown) {
                    errorHandler.error(new SAXParseException(
                            "DBDXMLStructureHandler.startElement"
                            + " illegal type ",locator));
                    state = State.idle;
                } else if(fieldDbType==DBType.dbMenu) {
                    String menuName = attributes.getValue("menuName");
                    dbdMenu = dbd.getMenu(menuName);
                    if(dbdMenu==null){
                        errorHandler.error(new SAXParseException(
                                "menu " + menuName + " was not found",locator));
                        state = State.idle;
                        return;
                    }
                } else if(fieldDbType==DBType.dbStructure) {
                    String structName = attributes.getValue("structureName");
                    dbdStructure = dbd.getDBDStructure(structName);
                    if(dbdStructure==null) {
                        errorHandler.error(new SAXParseException(
                            " structureName not given",locator));
                        state = State.idle;
                        return;
                    }
                } else if(fieldDbType==DBType.dbArray) {
                    String elementType = attributes.getValue("elementType");
                    if(elementType==null || elementType.length()==0) {
                        errorHandler.error(new SAXParseException(
                            " elementType not given",locator));
                        state = State.idle;
                        return;
                    }
                    arrayFieldElementDBType = getDBType(elementType);
                    arrayFieldElementType = getType(elementType);
                    if(arrayFieldElementType==Type.pvUnknown) {
                        errorHandler.error(new SAXParseException(
                            " elementType is unknown",locator));
                        state = State.idle;
                        return;
                    }
                }
            } else if(qName.equals("property")) {
                if(!gettingProperties) return;
                String propertyName = attributes.getValue("name");
                String associatedName = attributes.getValue("associatedField");
                if(propertyName==null || propertyName.length()==0) {
                    errorHandler.warning(new SAXParseException(
                            "property name not specified",locator));
                    return;
                }
                if(associatedName==null || associatedName.length()==0) {
                    errorHandler.warning(new SAXParseException(
                            "associatedField not specified",locator));
                    return;
                }
                Property property= FieldFactory.createProperty(
                    propertyName,associatedName);
                if(state==State.structure) {
                    structurePropertyList.add(property);
                } else if(state==State.field) {
                    fieldPropertyList.add(property);
                } else {
                    errorHandler.warning(new SAXParseException(
                            "logic error",locator));
                }
            }
        }
    
        void endElement(String qName) throws SAXException {
            if(state==State.idle) return;
            if(!qName.equals("field")) return;
            assert(state==State.field);
            state = State.structure;
            Property[] property = new Property[fieldPropertyList.size()];
            ListIterator<Property> iter = fieldPropertyList.listIterator();
            for(int i=0; i<property.length; i++) {
                property[i] = iter.next();
            }
            switch(fieldDbType) {
            case dbPvType:
                DBDField dbdField = DBDCreateFactory.createDBDField(fieldName,
                    fieldType,fieldDbType,property);
                dbdFieldList.add(dbdField);
                break;
            case dbMenu:
                DBDMenuField dbdMenuField = 
                    DBDCreateFactory.createDBDMenuField(
                    fieldName,dbdMenu,property);
                dbdFieldList.add(dbdMenuField);
                break;
            case dbStructure:
                DBDStructureField dbdStructureField = 
                    DBDCreateFactory.createDBDStructureField(fieldName,
                    dbdStructure,property);
                dbdFieldList.add(dbdStructureField);
                break;
            case dbArray:
                DBDArrayField dbdArrayField = 
                    DBDCreateFactory.createDBDArrayField(fieldName,
                    arrayFieldElementType,arrayFieldElementDBType,property);
                dbdFieldList.add(dbdArrayField);
                break;
            case dbLink:
                DBDLinkField dbdLinkField =
                    DBDCreateFactory.createDBDLinkField(fieldName,property);
                dbdFieldList.add(dbdLinkField);
                break;
            }
            return;
        }
    
        void characters(char[] ch, int start, int length)
        throws SAXException {
            // nothing to do
        }
        
        DBDXMLStructureHandler(DBD dbd, ErrorHandler errorHandler,
        Locator locator)
        {
            super();
            this.dbd = dbd;
            this.errorHandler = errorHandler;
            this.locator = locator;
        }
        
        private void getCommonAttributes(Attributes attributes) {
            String value;
            value = attributes.getValue("name");
            if(value==null) {
                fieldName = null;
            } else {
                fieldName = value;
            }
            value = attributes.getValue("type");
            if(value==null) {
                type = null;
            } else {
                type = value;
            }
            value = attributes.getValue("default");
            if(value==null) {
                defaultValue = null;
            } else {
                defaultValue = value;
            }
            value = attributes.getValue("asl");
            if(value==null) {
                asl = 1;
            } else {
                asl = Integer.parseInt(value);
            }
            value = attributes.getValue("link");
            if(value==null) {
                isLink = true;
            } else {
                isLink = Boolean.getBoolean(value);
            }
            value = attributes.getValue("readonly");
            if(value==null) {
                isReadOnly = true;
            } else {
                isReadOnly = Boolean.getBoolean(value);
            }
            fieldDbType = getDBType(type);
            fieldType = getType(type);
        }
        
        private enum State {idle, structure, field}
        
        private DBD dbd;
        private ErrorHandler errorHandler;
        private Locator locator;
        private State state = State.idle;
        private String structureName;
        private boolean isRecordType;
        private LinkedList<Property> structurePropertyList;
        private LinkedList<DBDField> dbdFieldList;
        // remaining are for field elements
        private boolean gettingProperties;
        private String fieldName;
        private String type;
        private DBType fieldDbType;
        private Type fieldType;
        private DBDMenu dbdMenu;
        private DBDStructure dbdStructure;
        private DBType arrayFieldElementDBType;
        private Type arrayFieldElementType;
        private int asl;
        private String defaultValue;
        private boolean isDesign;
        private boolean isLink;
        private boolean isReadOnly;
        private LinkedList<Property> fieldPropertyList;
    }
    
    static Type getType(String type) {
        if(type==null) return Type.pvUnknown;
        if(type.equals("boolean")) return Type.pvBoolean;
        if(type.equals("byte")) return Type.pvByte;
        if(type.equals("short")) return Type.pvShort;
        if(type.equals("int")) return Type.pvInt;
        if(type.equals("long")) return Type.pvLong;
        if(type.equals("float")) return Type.pvFloat;
        if(type.equals("double")) return Type.pvDouble;
        if(type.equals("string")) return Type.pvString;
        if(type.equals("enum")) return Type.pvEnum;
        if(type.equals("structure")) return Type.pvStructure;
        if(type.equals("array")) return Type.pvArray;
        if(type.equals("menu")) return Type.pvEnum;
        if(type.equals("link")) return Type.pvStructure;
        return Type.pvUnknown;
    }
    
    static DBType getDBType (String type) {
        if(type==null) return DBType.dbPvType;
        if(type.equals("boolean")) return DBType.dbPvType;
        if(type.equals("byte")) return DBType.dbPvType;
        if(type.equals("short")) return DBType.dbPvType;
        if(type.equals("int")) return DBType.dbPvType;
        if(type.equals("long")) return DBType.dbPvType;
        if(type.equals("float")) return DBType.dbPvType;
        if(type.equals("double")) return DBType.dbPvType;
        if(type.equals("string")) return DBType.dbPvType;
        if(type.equals("enum")) return DBType.dbPvType;
        if(type.equals("structure")) return DBType.dbStructure;
        if(type.equals("array")) return DBType.dbArray;
        if(type.equals("menu")) return DBType.dbMenu;
        if(type.equals("link")) return DBType.dbLink;
        return DBType.dbPvType;
        
    }
}
