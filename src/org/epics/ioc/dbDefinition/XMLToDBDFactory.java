/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.epics.ioc.util.*;
import org.epics.ioc.pvAccess.*;

/**
 * Factory to convert an xml file to a Database Definition and put it in a database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToDBDFactory {
    private static AtomicBoolean isInUse = new AtomicBoolean(false);
    // For use by all private classes
    private static DBD dbd;
    private static IOCMessageListener iocMessageListener;
    private static IOCMessageType okToAddType = IOCMessageType.fatalError;
    private static boolean okToAdd;
    private static IOCXMLReader iocxmlReader;
    /**
     * Convert an xml file to Database definitions and put
     * the definitions in a database.
     * @param dbd a Database Definition Database
     * @param fileName the name of the xml file.
     * @param messageListener The listener to receive messages.
     */
    public static void convert(DBD dbd, String fileName,IOCMessageListener messageListener)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            messageListener.message("XMLToDBDFactory is already active",IOCMessageType.fatalError);
        }
        try {
            XMLToDBDFactory.dbd = dbd;
            iocMessageListener = messageListener;
            IOCXMLListener listener = new Listener();
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("DBDefinition",fileName,listener);
        } finally {
            isInUse.set(false);
        }
    }

    public static DBD addToMaster(String fileName,IOCMessageListener messageListener,IOCMessageType okToAddType) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            System.out.println("XMLToDBDFactory is already active");
            return null;
        }
        try {
            DBD master = DBDFactory.find("master");
            if(master==null) {
                DBDFactory.create("master", null);
                master = DBDFactory.find("master");
                if(master==null) {
                    System.out.println("XMLToDBDFactory failed to create master DBD");
                    return null;
                }
            }
            DBD add  = DBDFactory.create("add", master);
            if(add==null) {
                System.out.println("XMLToDBDFactory failed to create add DBD");
                return null;
            }
            try {
                XMLToDBDFactory.dbd = add;
                XMLToDBDFactory.okToAddType = okToAddType;
                okToAdd = true;
                IOCXMLListener listener = new Listener();
                iocxmlReader = IOCXMLReaderFactory.getReader();
                iocxmlReader.parse("DBDefinition",fileName,listener);
                if(!okToAdd) return null;
                add.mergeIntoMaster();
                return add;
            } finally {
                DBDFactory.remove(add);
            }
        } finally {
            isInUse.set(false);
        }
    }
    
    private static class Listener implements IOCXMLListener {
          
        private enum State {
            idle,
            menu,
            structure,
            recordType,
            support
        } 
        private State state = State.idle;

        private DBDXMLHandler menuHandler = new DBDXMLMenuHandler();
        private DBDXMLHandler structureHandler = new DBDXMLStructureHandler();
        private DBDXMLHandler supportHandler = new DBDXMLSupportHandler();
        
        public void endDocument(){}       

        public void startElement(String qName,Map<String,String> attributes)
        {
            switch(state) {
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
                    iocxmlReader.message("startElement " + qName + " not understood",
                            IOCMessageType.error);
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
        
        public void endElement(String qName)
        {
            switch(state) {
            case idle:
                iocxmlReader.message(
                    "endElement " + qName + " not understood",
                    IOCMessageType.error);
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
        {
            switch(state) {
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

        public void message(String message,IOCMessageType messageType) {
            iocMessageListener.message(message, messageType);
            if(messageType.ordinal()>okToAddType.ordinal()) okToAdd = false;
        }
    }


    private interface DBDXMLHandler {
        void start(String qName, Map<String,String> attributes);
        void end(String qName);
        void startElement(String qName, Map<String,String> attributes);
        void characters(char[] ch, int start, int length);
        void endElement(String qName);
    }

    private static class DBDXMLMenuHandler implements DBDXMLHandler{
        private State state = State.idle;
        private String menuName;
        private LinkedList<String> choiceList;
        private StringBuilder choiceBuilder = new StringBuilder();
        private enum State {idle, nextChoice, getChoice}
        
        public void start(String qName, Map<String,String> attributes) {
            menuName = attributes.get("name");
            if(menuName==null) {
                iocxmlReader.message("attribute name not specified",
                        IOCMessageType.error);
                state = State.idle;
            }
            if(dbd.getMenu(menuName)!=null) {
                iocxmlReader.message(
                    "menu " + menuName + " ignored because it already exists",
                    IOCMessageType.warning);
                state = State.idle;
            } else {
                choiceList = new LinkedList<String>();
                state = State.nextChoice;
            }
        }
    
        public void end(String qName){
            if(state==State.idle) return;
            if(state!=State.nextChoice) {
                iocxmlReader.message(
                    "Logic error in DBDXMLMenuHandler.end"
                    + " state should be nextChoice",
                    IOCMessageType.error);
                state = State.idle;
                return;
            }
            if(menuName==null || menuName.length()==0
            || choiceList==null || choiceList.size()==0) {
                iocxmlReader.message(
                        "menu definition is not complete",
                        IOCMessageType.error);
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
    
        public void startElement(String qName, Map<String,String> attributes) {
            if(state==State.idle) return;
            if(state!=State.nextChoice) {
                iocxmlReader.message(
                        "Logic error in DBDXMLMenuHandler.startElement"
                        + "state should be nextChoice",
                        IOCMessageType.error);
                state = State.idle;
                return;
            }
            if(!qName.equals("choice")) {
                iocxmlReader.message(
                        "illegal element. only choice is allowed",
                        IOCMessageType.error);
                state = State.idle;
                return;
            }
            state = State.getChoice;
            choiceBuilder.setLength(0);
        }
    
        public void endElement(String qName){
            if(state==State.idle) return;
            if(state!=State.getChoice) {
                iocxmlReader.message(
                        "Logic error in DBDXMLMenuHandler.startElement"
                        + "state should be nextChoice",
                        IOCMessageType.error);
                state = State.idle;
                return;
            }
            String newChoice = choiceBuilder.toString();
            if(newChoice.length()<=0) {
                iocxmlReader.message("illegal choice",
                        IOCMessageType.error);
                    state = State.idle;
                    return;
            }
            choiceList.add(choiceBuilder.toString());
            state = State.nextChoice;
        }
    
        public void characters(char[] ch, int start, int length){
            if(state!=State.getChoice) return;
            while(start<ch.length && length>0 && ch[start]==' ') {
                start++; length--;
            }
            while(length>0 && ch[start+ length-1]==' ') length--;
            if(length<=0) return;
            choiceBuilder.append(ch,start,length);
        }
    }

    private static class DBDXMLStructureHandler implements DBDXMLHandler, DBDAttributeValues
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
        private Map<String,String> attributes;
        private DBDAttribute dbdAttribute;
        private LinkedList<Property> fieldPropertyList;
        
        DBDXMLStructureHandler()
        {
            super();
        }
            
        public int getLength() {
            return attributes.size();
        }

        public String getValue(String name) {
            return attributes.get(name);
        }

        public Set<String> keySet() {
            return attributes.keySet();
        }

        public void start(String qName, Map<String,String> attributes) {
            if(state!=State.idle) {
                iocxmlReader.message(
                   "DBDXMLStructureHandler.start logic error not idle",
                   IOCMessageType.error);
                state = State.idle;
                return;
            }
            structureName = attributes.get("name");
            if(structureName==null || structureName.length() == 0) {
                iocxmlReader.message("name not specified",
                        IOCMessageType.error);
                state = State.idle;
                return;
            }
            structureSupportName = attributes.get("supportName");
            if(qName.equals("recordType")) {
                if(dbd.getRecordType(structureName)!=null) {
                    iocxmlReader.message(
                        "recordType " + structureName + " already exists",
                        IOCMessageType.warning);
                    state = State.idle;
                    return;
                }
                isRecordType = true;
            } else if(qName.equals("structure")){
                if(dbd.getStructure(structureName)!=null) {
                    iocxmlReader.message(
                        "structure " + structureName + " already exists",
                        IOCMessageType.warning);
                    state = State.idle;
                    return;
                }
                isRecordType = false;
            } else {
                iocxmlReader.message(
                        "DBDXMLStructureHandler.start logic error",
                        IOCMessageType.error);
                state = State.idle;
                return;
            }
            structurePropertyList = new LinkedList<Property>();
            dbdFieldList = new LinkedList<DBDField>();
            state = State.structure;
        }
    
        public void end(String qName){
            if(state==State.idle) {
                structurePropertyList = null;
                dbdFieldList = null;
                return;
            }
            if(dbdFieldList.size()==0) {
                iocxmlReader.message(
                   "DBDXMLStructureHandler.end no fields were defined",
                   IOCMessageType.error);
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
                    iocxmlReader.message(
                            "recordType " + structureName + " already exists",
                            IOCMessageType.warning);
                }
                if(structureSupportName!=null) {
                    dbdRecordType.setSupportName(structureSupportName);
                }
            } else {
                DBDStructure dbdStructure = DBDCreateFactory.createStructure(
                        structureName,dbdField,property);
                boolean result = dbd.addStructure(dbdStructure);
                if(!result) {
                    iocxmlReader.message(
                            "structure " + structureName + " already exists",
                            IOCMessageType.warning);
                }
                if(structureSupportName!=null) {
                    dbdStructure.setSupportName(structureSupportName);
                }
            }
            structurePropertyList = null;
            dbdFieldList = null;
            state= State.idle;
        }
     
        public void startElement(String qName, Map<String,String> attributes){
            if(state==State.idle) return;
            if(qName.equals("field")) {
                assert(state==State.structure);
                this.attributes = attributes;
                try {
                    dbdAttribute = DBDAttributeFactory.create(dbd,this);
                }
                catch(Exception e) {
                    iocxmlReader.message(e.getMessage(),
                            IOCMessageType.error);
                    state = State.idle;
                    return;
                }
                finally {
                    this.attributes = null;
                }
                Type type = dbdAttribute.getType();
                DBType dbType = dbdAttribute.getDBType();
                if(dbType!=DBType.dbLink && type==Type.pvUnknown ) {
                    iocxmlReader.message("type not specified correctly",
                            IOCMessageType.error);
                    state= State.idle;
                    return;
                }
                fieldSupportName = attributes.get("supportName");
                fieldPropertyList =  new  LinkedList<Property>();
                state = State.field;
            } else if(qName.equals("property")) {
                String propertyName = attributes.get("name");
                String associatedName = attributes.get("associatedField");
                if(propertyName==null || propertyName.length()==0) {
                    iocxmlReader.message(
                            "property name not specified",
                            IOCMessageType.warning);
                    return;
                }
                if(associatedName==null || associatedName.length()==0) {
                    iocxmlReader.message(
                            "associatedField not specified",
                            IOCMessageType.error);
                    return;
                }
                Property property= FieldFactory.createProperty(
                    propertyName,associatedName);
                if(state==State.structure) {
                    structurePropertyList.add(property);
                } else if(state==State.field) {
                    fieldPropertyList.add(property);
                } else {
                    iocxmlReader.message("logic error",
                            IOCMessageType.fatalError);
                }
            }
        }
    
        public void endElement(String qName){
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
    
        public void characters(char[] ch, int start, int length){}
    }
    
    private static class DBDXMLSupportHandler implements DBDXMLHandler{
        
        public void start(String qName, Map<String,String> attributes) {
            String name = attributes.get("name");
            String configurationStructureName =
                attributes.get("configurationStructureName");
            String factoryName = attributes.get("factoryName");
            if(name==null||name.length()==0) {
                iocxmlReader.message(
                    "name was not specified correctly",
                    IOCMessageType.error);
                return;
            }
            DBDSupport support = dbd.getSupport(name);
            if(support!=null) {
                iocxmlReader.message(
                    "support " + name  + " already exists",
                    IOCMessageType.warning);
                    return;
            }
            if(factoryName==null||factoryName.length()==0) {
                iocxmlReader.message(
                    "factoryName was not specified correctly",
                    IOCMessageType.error);
                return;
            }
            support = DBDCreateFactory.createSupport(
                name,configurationStructureName,factoryName);
            if(support==null) {
                iocxmlReader.message(
                    "failed to create support " + qName,
                    IOCMessageType.error);
                    return;
            }
            if(!dbd.addSupport(support)) {
                iocxmlReader.message(
                    "support " + qName + " already exists",
                    IOCMessageType.warning);
                return;
            }
        }
    
        public void end(String qName) {}
    
        public void startElement(String qName, Map<String,String> attributes) {}
    
        public void endElement(String qName) {}
    
        public void characters(char[] ch, int start, int length) {}
        
    }
}
