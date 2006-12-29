/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.epics.ioc.util.*;
import org.epics.ioc.pv.*;

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
    private static Requestor requestor;
    private static IOCXMLReader iocxmlReader;
    /**
     * Convert an xml file to Database definitions and put
     * the definitions in a database.
     * @param dbd a Database Definition Database
     * @param fileName the name of the xml file.
     * @param requestor The requestor.
     */
    public static void convert(DBD dbd, String fileName,Requestor requestor)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requestor.message("XMLToDBDFactory is already active",MessageType.fatalError);
        }
        try {
            XMLToDBDFactory.dbd = dbd;
            XMLToDBDFactory.requestor = requestor;
            IOCXMLListener listener = new Listener();
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("DBDefinition",fileName,listener);
        } finally {
            isInUse.set(false);
        }
    }

    /**
     * Create a Database Definition Database (DBD) and populate it
     * with definitions from an XML Database Definition file.
     * @param dbdName The name for the database.
     * This name will not appear in the map returned by DBDFactory.getDBDMap.
     * @param fileName The XML file containing database definitions.
     * @param requestor A listener for error messages.
     * @return a DBD or null. If not null than the definitions are not in the master DBD.
     * The caller must call DBD.mergeIntoMaster to put the definitions into the master.
     */
    public static DBD create(String dbdName,String fileName,Requestor requestor) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requestor.message("XMLToDBDFactory is already active",MessageType.error);
            return null;
        }
        try {
            DBD dbd  = DBDFactory.create(dbdName);
            if(dbd==null) {
                requestor.message(
                    "XMLToDBDFactory failed to create DBD " + dbdName,
                    MessageType.error);
                return null;
            }
            XMLToDBDFactory.dbd = dbd;
            XMLToDBDFactory.requestor = requestor;
            IOCXMLListener listener = new Listener();
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("DBDefinition",fileName,listener);
            return dbd;        
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
            support,
            linkSupport
        } 
        private State state = State.idle;

        private DBDXMLHandler menuHandler = new DBDXMLMenuHandler();
        private DBDXMLHandler structureHandler = new DBDXMLStructureHandler();
        private DBDXMLHandler supportHandler = new DBDXMLSupportHandler();
        private DBDXMLHandler linkSupportHandler = new DBDXMLLinkSupportHandler();
        
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
                } else if(qName.equals("linkSupport")) {
                    state = State.linkSupport;
                    linkSupportHandler.start(qName,attributes);
                } else {
                    iocxmlReader.message("startElement " + qName + " not understood",
                            MessageType.error);
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
            case linkSupport:
                linkSupportHandler.startElement(qName,attributes);
                break;
            }
        }
        
        public void endElement(String qName)
        {
            switch(state) {
            case idle:
                iocxmlReader.message(
                    "endElement " + qName + " not understood",
                    MessageType.error);
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
            case linkSupport:
                if(qName.equals("linkSupport")) {
                    linkSupportHandler.end(qName);
                    state = State.idle;
                } else {
                    linkSupportHandler.endElement(qName);
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
            case linkSupport:
                linkSupportHandler.characters(ch,start,length);
                break;
            }
        }

        public void message(String message,MessageType messageType) {
            requestor.message(message, messageType);
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
                        MessageType.error);
                state = State.idle;
            }
            if(dbd.getMenu(menuName)!=null) {
                iocxmlReader.message(
                    "menu " + menuName + " ignored because it already exists",
                    MessageType.warning);
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
                    MessageType.error);
                state = State.idle;
                return;
            }
            if(menuName==null || menuName.length()==0
            || choiceList==null || choiceList.size()==0) {
                iocxmlReader.message(
                        "menu definition is not complete",
                        MessageType.error);
            } else {
                String[] choice = new String[choiceList.size()];
                ListIterator<String> iter = choiceList.listIterator();
                for(int i=0; i<choice.length; i++) {
                    choice[i] = iter.next();
                }
                choiceList = null;
                DBDMenu dbdMenu = DBDFieldFactory.createMenu(
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
                        MessageType.error);
                state = State.idle;
                return;
            }
            if(!qName.equals("choice")) {
                iocxmlReader.message(
                        "illegal element. only choice is allowed",
                        MessageType.error);
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
                        MessageType.error);
                state = State.idle;
                return;
            }
            String newChoice = choiceBuilder.toString();
            if(newChoice.length()<=0) {
                iocxmlReader.message("illegal choice",
                        MessageType.error);
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

    private static class DBDXMLStructureHandler implements DBDXMLHandler
    {
 
        private enum State {idle, structure, field}      
        
        private State state = State.idle;
        private String structureName;
        private boolean isRecordType;
        private String structureSupportName = null;
        private String fieldSupportName = null;
        private LinkedList<Property> structurePropertyList;
        private LinkedList<Field> fieldList;
        // remaining are for field elements
        private String fieldName;
        private String menuName;
        private String fieldStructureName;
        private DBDStructure fieldStructure;
        private DBDMenu dbdMenu;
        private Type type;
        private Type elementType;
        private FieldAttribute fieldAttribute;
        private FieldAttribute structureAttribute;
        private LinkedList<Property> fieldPropertyList;
        
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
            if(qName.equals("recordType")) {
                if(dbd.getRecordType(structureName)!=null) {
                    iocxmlReader.message(
                        "recordType " + structureName + " already exists",
                        MessageType.warning);
                    state = State.idle;
                    return;
                }
                isRecordType = true;
            } else if(qName.equals("structure")){
                if(dbd.getStructure(structureName)!=null) {
                    iocxmlReader.message(
                        "structure " + structureName + " already exists",
                        MessageType.warning);
                    state = State.idle;
                    return;
                }
                isRecordType = false;
            } else {
                iocxmlReader.message(
                        "DBDXMLStructureHandler.start logic error",
                        MessageType.error);
                state = State.idle;
                return;
            }
            structureAttribute = FieldFactory.createFieldAttribute(attributes);
            structurePropertyList = new LinkedList<Property>();
            fieldList = new LinkedList<Field>();
            state = State.structure;
        }
    
        public void end(String qName){
            if(state==State.idle) {
                structurePropertyList = null;
                fieldList = null;
                return;
            }
            Property[] property = new Property[structurePropertyList.size()];
            ListIterator<Property> iter = structurePropertyList.listIterator();
            for(int i=0; i<property.length; i++) {
                property[i] = iter.next();
            }
            Field[] field = new Field[fieldList.size()];
            ListIterator<Field> iter1 = fieldList.listIterator();
            FieldAttribute[] fieldAttribute = new FieldAttribute[fieldList.size()];
            for(int i=0; i<field.length; i++) {
                field[i] = iter1.next();
            }
            if(isRecordType) {
                DBDRecordType dbdRecordType = DBDFieldFactory.createRecordType(
                        structureName,field,property,structureAttribute);
                boolean result = dbd.addRecordType(dbdRecordType);
                if(!result) {
                    iocxmlReader.message(
                            "recordType " + structureName + " already exists",
                            MessageType.warning);
                }
                if(structureSupportName!=null) {
                    dbdRecordType.setSupportName(structureSupportName);
                }
            } else {
                DBDStructure dbdStructure = DBDFieldFactory.createStructure(
                        structureName,field,property,structureAttribute);
                boolean result = dbd.addStructure(dbdStructure);
                if(!result) {
                    iocxmlReader.message(
                            "structure " + structureName + " already exists",
                            MessageType.warning);
                }
                if(structureSupportName!=null) {
                    dbdStructure.setSupportName(structureSupportName);
                }
            }
            structurePropertyList = null;
            fieldList = null;
            state= State.idle;
        }
     
        public void startElement(String qName, Map<String,String> attributes){
            if(state==State.idle) return;
            if(qName.equals("field")) {
                assert(state==State.structure);
                fieldAttribute = FieldFactory.createFieldAttribute(attributes);
                fieldName = attributes.get("name");
                if(fieldName==null) {
                    iocxmlReader.message("name not specified",
                            MessageType.error);
                    state= State.idle;
                    return;
                }
                type = DBDFieldFactory.getType(attributes);
                if(type==null) {
                    iocxmlReader.message("type not specified correctly",
                            MessageType.error);
                    state= State.idle;
                    return;
                }
                if(type==Type.pvMenu) {
                    menuName = attributes.get("menuName");
                    if(menuName==null) {
                        iocxmlReader.message("menuName not specified",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                    dbdMenu = dbd.getMenu(menuName);
                    if(dbdMenu==null) {
                        iocxmlReader.message("menuName not found in DBD database",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                }
                if(type==Type.pvStructure) {
                    fieldStructureName = attributes.get("structureName");
                    if(fieldStructureName==null) {
                        iocxmlReader.message("structureName not specified",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                    fieldStructure = dbd.getStructure(fieldStructureName);
                    if(fieldStructure==null) {
                        iocxmlReader.message("structureName not found in DBD database",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                }
                if(type==Type.pvArray) {
                    elementType = DBDFieldFactory.getElementType(attributes);
                    if(elementType==null) {
                        iocxmlReader.message("elementType not specified correctly",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
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
                            MessageType.warning);
                    return;
                }
                if(associatedName==null || associatedName.length()==0) {
                    iocxmlReader.message(
                            "associatedField not specified",
                            MessageType.error);
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
                            MessageType.fatalError);
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
            Field field = null;
            switch(type) {
            case pvMenu:
                field = DBDFieldFactory.createMenuField(fieldName,
                    property,fieldAttribute,dbdMenu.getMenuName());
                break;
            case pvStructure:
                field = DBDFieldFactory.createStructureField(fieldName,
                    property,fieldAttribute,fieldStructure);
                break;
            case pvArray:
                field = DBDFieldFactory.createArrayField(fieldName,
                    property,fieldAttribute,elementType);
                break;
            default:
                field = DBDFieldFactory.createField(fieldName,
                        property,fieldAttribute,type);
                    break;
            }
            if(field==null) {
                throw new IllegalStateException("logic error");
            }
            fieldList.add(field);
            fieldPropertyList = null;
            if(fieldSupportName!=null) {
                field.setSupportName(fieldSupportName);
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
            support = DBDFieldFactory.createSupport(
                name,factoryName);
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
    
private static class DBDXMLLinkSupportHandler implements DBDXMLHandler{
        
        public void start(String qName, Map<String,String> attributes) {
            String name = attributes.get("name");
            if(name==null||name.length()==0) {
                iocxmlReader.message(
                    "name was not specified correctly",
                    MessageType.error);
                return;
            }
            DBDLinkSupport support = dbd.getLinkSupport(name);
            if(support!=null) {
                iocxmlReader.message(
                    "support " + name  + " already exists",
                    MessageType.warning);
                    return;
            }
            String configurationStructureName =
                attributes.get("configurationStructureName");
            String factoryName = attributes.get("factoryName");
            if(factoryName==null||factoryName.length()==0) {
                iocxmlReader.message(
                    "factoryName was not specified correctly",
                    MessageType.error);
                return;
            }
            support = DBDFieldFactory.createLinkSupport(
                name,factoryName,configurationStructureName);
            if(support==null) {
                iocxmlReader.message(
                    "failed to create support " + qName,
                    MessageType.error);
                    return;
            }
            if(!dbd.addLinkSupport(support)) {
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
    
}
