/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.io.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.epics.ioc.pvAccess.*;

import java.util.*;

/**
 * Factory to convert an xml file to a Database Definition and put it in a database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToDBDFactory {
    // For use by all private classes
    private static DBD dbd;
    private static ErrorHandler errorHandler;
    private static Locator locator; 
    /**
     * Convert an xml file to Database definitions and put
     * the definitions in a database.
     * @param dbd a Database Definition Database
     * @param fileName the name of the xml file.
     * @throws MalformedURLException if SAX throws it.
     * @throws IllegalStateException if any errors were detected.
     */
    public static void convert(DBD dbd, String fileName)
        throws MalformedURLException,IllegalStateException
    {
        XMLToDBDFactory.dbd = dbd;
        String uri = new File(fileName).toURL().toString();
        XMLReader reader;
        
        
        Handler handler = new Handler();
        errorHandler = handler;
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(uri);
        } catch (SAXException e) {
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with SAXException"
                + String.format("%n")
                + e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IOException"
                + String.format("%n")
                + e.getMessage());
        } catch (IllegalStateException e) {
            handler.error("IllegalStateException " + e.getMessage());
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IllegalStateException"
                + String.format("%n")
                + e.getMessage());
        } catch (IllegalArgumentException e) {
            handler.error("IllegalArgumentException " + e.getMessage());
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IllegalArgumentException"
                + String.format("%n")
                + e.getMessage());
        } catch (Exception e) {
            handler.error("Exception " + e.getMessage());
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with Exception"
                + String.format("%n")
                + e.getMessage());
        }
    }

    private static class Handler  implements ContentHandler, ErrorHandler {
          
        private enum State {
            startDocument,
            idle,
            menu,
            structure,
            recordType,
            support
        } 
        private State state = State.startDocument;
        private int nWarning = 0;
        private int nError = 0;
        private int nFatal = 0;
        
        Handler()  throws MalformedURLException {}
        private String printSAXParseExceptionMessage(SAXParseException e)
        {
            return String.format("line %d column %d%nreason %s%n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                e.toString());
        }

        private DBDXMLHandler menuHandler;
        private DBDXMLHandler structureHandler;
        private DBDXMLHandler supportHandler;
        
        public void error(String message) {
            System.err.printf("line %d column %d%nreason %s%n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                message);
            nError++;
        }
        public void warning(SAXParseException e) throws SAXException {
            System.err.printf("warning %s%n",printSAXParseExceptionMessage(e));
            nWarning++;
        }
        public void error(SAXParseException e) throws SAXException {
            System.err.printf("error %s%n",printSAXParseExceptionMessage(e));
            nError++;
        }
        
        public void fatalError(SAXParseException e) throws SAXException {
            System.err.printf("fatal error %s%n",printSAXParseExceptionMessage(e));
            nFatal++;
        }
        
        public void setDocumentLocator(Locator locatorin) {
            locator = locatorin;
            menuHandler = new DBDXMLMenuHandler();
            structureHandler = new DBDXMLStructureHandler();
            supportHandler = new DBDXMLSupportHandler();
        }
        
        public void startDocument() throws SAXException {
            state = State.startDocument;
        }
        
        
        public void endDocument() throws SAXException {
            if(nWarning>0 || nError>0 || nFatal>0) {
                System.err.printf("endDocument: warning %d severe %d fatal %d%n",
                    nWarning,nError,nFatal);
            }
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
                } else if(qName.equals("support")) {
                    state = State.support;
                    supportHandler.start(qName,attributes);
                } else {
                    errorHandler.error(new SAXParseException(
                        "startElement " + qName + " not understood",
                        locator));
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
            case support:
                supportHandler.startElement(qName,attributes);
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
                    errorHandler.error(new SAXParseException(
                         "endElement " + qName + " not understood",
                         locator));
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
            case support:
                if(qName.equals("support")) {
                    supportHandler.end(qName);
                    state = State.idle;
                } else {
                    supportHandler.endElement(qName);
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
            case menu: 
                menuHandler.characters(ch,start,length);
                break;
            case structure:
            case recordType:
                structureHandler.characters(ch,start,length);
                break;
            case support:
                supportHandler.characters(ch,start,length);
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
        private State state = State.idle;
        private String menuName;
        private LinkedList<String> choiceList;
        private StringBuilder choiceBuilder;
        private enum State {idle, nextChoice, getChoice}
        
        DBDXMLMenuHandler() {
            super();
        }
        
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
                DBDMenu dbdMenu = DBDCreateFactory.createMenu(
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
    }

    private static class DBDXMLStructureHandler
    extends DBDXMLHandler implements DBDAttributeValues
    {
 
        private enum State {idle, structure, field}      
        
        private State state = State.idle;
        private String structureName;
        private boolean isRecordType;
        private String structureSupportName = null;
        private String fieldSupportName = null;
        private LinkedList<Property> structurePropertyList;
        private LinkedList<DBDField> dbdFieldList;
        // remaining are for field elements
        private Attributes attributes;
        private DBDAttribute dbdAttribute;
        private LinkedList<Property> fieldPropertyList;
        
        DBDXMLStructureHandler()
        {
            super();
        }
            
        public int getLength() {
            return attributes.getLength();
        }

        public String getName(int index) {
            return attributes.getQName(index);
        }

        public String getValue(int index) {
            return attributes.getValue(index);
        }

        public String getValue(String name) {
            return attributes.getValue(name);
        }

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
                    "name not specified",locator));
                state = State.idle;
                return;
            }
            structureSupportName = attributes.getValue("supportName");
            if(qName.equals("recordType")) {
                if(dbd.getRecordType(structureName)!=null) {
                    errorHandler.warning(new SAXParseException(
                        "recordType " + structureName + " already exists",
                        locator));
                    state = State.idle;
                    return;
                }
                isRecordType = true;
            } else if(qName.equals("structure")){
                if(dbd.getStructure(structureName)!=null) {
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
            if(isRecordType) {
                DBDRecordType dbdRecordType = DBDCreateFactory.createRecordType(
                        structureName,dbdField,property);
                boolean result = dbd.addRecordType(dbdRecordType);
                if(!result) {
                    errorHandler.warning(new SAXParseException(
                            "recordType " + structureName + " already exists",
                            locator));
                }
                if(structureSupportName!=null) {
                    dbdRecordType.setSupportName(structureSupportName);
                }
            } else {
                DBDStructure dbdStructure = DBDCreateFactory.createStructure(
                        structureName,dbdField,property);
                boolean result = dbd.addStructure(dbdStructure);
                if(!result) {
                    errorHandler.warning(new SAXParseException(
                            "structure " + structureName + " already exists",
                            locator));
                }
                if(structureSupportName!=null) {
                    dbdStructure.setSupportName(structureSupportName);
                }
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
                this.attributes = attributes;
                try {
                    dbdAttribute = DBDAttributeFactory.create(dbd,this);
                }
                catch(Exception e) {
                    errorHandler.error(new SAXParseException(
                            e.getMessage() ,locator));
                    state = State.idle;
                    return;
                }
                finally {
                    this.attributes = null;
                }
                Type type = dbdAttribute.getType();
                DBType dbType = dbdAttribute.getDBType();
                if(dbType!=DBType.dbLink && type==Type.pvUnknown ) {
                    errorHandler.error(new SAXParseException(
                            "type not specified correctly",locator));
                    state= State.idle;
                    return;
                }
                fieldSupportName = attributes.getValue("supportName");
                fieldPropertyList =  new  LinkedList<Property>();
                state = State.field;
            } else if(qName.equals("property")) {
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
            DBDField dbdField = DBDCreateFactory.createField(dbdAttribute,property);
            dbdFieldList.add(dbdField);
            dbdAttribute = null;
            fieldPropertyList = null;
            if(fieldSupportName!=null) {
                dbdField.setSupportName(fieldSupportName);
            }
            return;
        }
    
        void characters(char[] ch, int start, int length)
        throws SAXException {
            // nothing to do
        }
    }
    
    private static class DBDXMLSupportHandler extends DBDXMLHandler{
        
        void start(String qName, Attributes attributes)
        throws SAXException {
            String name = attributes.getValue("name");
            String configurationStructureName =
                attributes.getValue("configurationStructureName");
            String factoryName = attributes.getValue("factoryName");
            if(name==null||name.length()==0) {
                errorHandler.error(new SAXParseException(
                    "name was not specified correctly",locator));
                return;
            }
            DBDSupport support = dbd.getSupport(name);
            if(support!=null) {
                errorHandler.warning(new SAXParseException(
                    "support " + name  + " already exists",locator));
                    return;
            }
            if(factoryName==null||factoryName.length()==0) {
                errorHandler.error(new SAXParseException(
                    "factoryName was not specified correctly",locator));
                return;
            }
            support = DBDCreateFactory.createSupport(
                name,configurationStructureName,factoryName);
            if(support==null) {
                errorHandler.error(new SAXParseException(
                    "failed to create support " + qName,locator));
                    return;
            }
            if(!dbd.addSupport(support)) {
                errorHandler.warning(new SAXParseException(
                    "support " + qName + " already exists",
                    locator));
                return;
            }
        }
    
        void end(String qName) throws SAXException {
            // nothing to do
        }
    
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            // nothing to do
        }
    
        void endElement(String qName) throws SAXException {
            // nothing to do
        }
    
        void characters(char[] ch, int start, int length)
        throws SAXException {
            // nothing to do
        }
        
        DBDXMLSupportHandler() {
            super();
        }
    }
}
