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
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static Requester requester;
    private static IOCXMLReader iocxmlReader;
    /**
     * Convert an xml file to Database definitions and put
     * the definitions in a database.
     * @param dbd a Database Definition Database
     * @param fileName the name of the xml file.
     * @param requester The requester.
     */
    public static void convert(DBD dbd, String fileName,Requester requester)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToDBDFactory is already active",MessageType.fatalError);
        }
        try {
            XMLToDBDFactory.dbd = dbd;
            XMLToDBDFactory.requester = requester;
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
     * @param requester A listener for error messages.
     * @return a DBD or null. If not null than the definitions are not in the master DBD.
     * The caller must call DBD.mergeIntoMaster to put the definitions into the master.
     */
    public static DBD create(String dbdName,String fileName,Requester requester) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToDBDFactory is already active",MessageType.error);
            return null;
        }
        try {
            DBD dbd  = DBDFactory.create(dbdName);
            if(dbd==null) {
                requester.message(
                    "XMLToDBDFactory failed to create DBD " + dbdName,
                    MessageType.error);
                return null;
            }
            XMLToDBDFactory.dbd = dbd;
            XMLToDBDFactory.requester = requester;
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
            structure,
            recordType,
            support,
            create
        } 
        private State state = State.idle;

        private DBDXMLHandler structureHandler = new DBDXMLStructureHandler();
        private DBDXMLHandler supportHandler = new DBDXMLSupportHandler();
        private DBDXMLHandler createHandler = new DBDXMLCreateHandler();
        
        public void endDocument(){}       

        public void startElement(String qName,Map<String,String> attributes)
        {
            switch(state) {
            case idle:
                if(qName.equals("structure")) {
                    state = State.structure;
                    structureHandler.start(qName,attributes);
                } else if(qName.equals("recordType")) {
                    state = State.structure;
                    structureHandler.start(qName,attributes);
                } else if(qName.equals("support")) {
                    state = State.support;
                    supportHandler.start(qName,attributes);
                } else if(qName.equals("create")) {
                    state = State.create;
                    createHandler.start(qName,attributes);
                } else {
                    iocxmlReader.message("startElement " + qName + " not understood",
                            MessageType.error);
                }
                break;
            case structure:
                // no break. structure and recordType handled by structureHandler
            case recordType:
                structureHandler.startElement(qName,attributes);
                break;
            case support:
                supportHandler.startElement(qName,attributes);
                break;
            case create:
                createHandler.startElement(qName,attributes);
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
            case create:
                if(qName.equals("create")) {
                    createHandler.end(qName);
                    state = State.idle;
                } else {
                    createHandler.endElement(qName);
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
            case recordType:
                structureHandler.characters(ch,start,length);
                break;
            case support:
                supportHandler.characters(ch,start,length);
                break;
            case create:
                createHandler.characters(ch,start,length);
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
        private boolean isRecordType;
        private String structureSupportName = null;
        private String structureCreateName = null;
        private String fieldSupportName = null;
        private String fieldCreateName = null;
        private LinkedList<Property> structurePropertyList;
        private LinkedList<Field> fieldList;
        // remaining are for field elements
        private String fieldName;
        private DBDStructure fieldStructure;
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
            structureCreateName = attributes.get("createName");
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
            structureAttribute = fieldCreate.createFieldAttribute();
            structureAttribute.setAttributes(attributes,excludeString);
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
            for(int i=0; i<field.length; i++) {
                field[i] = iter1.next();
            }
            if(isRecordType) {
                DBDRecordType dbdRecordType = dbd.createRecordType(
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
                if(structureCreateName!=null) {
                    dbdRecordType.setCreateName(structureCreateName);
                }
            } else {
                DBDStructure dbdStructure = dbd.createStructure(
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
                if(structureCreateName!=null) {
                    dbdStructure.setCreateName(structureCreateName);
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
                Property property= fieldCreate.createProperty(
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
            case pvStructure:
                // Combine the current properties with the dbdStructure properties
                DBDStructure dbdStructure = dbd.getStructure(fieldStructure.getStructureName());
                Property[] structPropertys = dbdStructure.getPropertys();
                LinkedList<Property> combinedPropertyList = new  LinkedList<Property>();
                for(Property newProperty: property) combinedPropertyList.add(newProperty);
                outer:
                for(Property structProperty: structPropertys) {
                    String propertyName = structProperty.getPropertyName();
                    for(Property combined: combinedPropertyList) {
                        if(combined.getPropertyName().equals(propertyName)) continue outer;
                    }
                    combinedPropertyList.add(structProperty);
                }
                property = new Property[combinedPropertyList.size()];
                iter = combinedPropertyList.listIterator();
                for(int i=0; i<property.length; i++) {
                     property[i] = iter.next();
                }
                field = fieldCreate.createStructure(fieldName,
                    fieldStructure.getStructureName(),fieldStructure.getFields(),
                    property,fieldAttribute);
                break;
            case pvArray:
                field = fieldCreate.createArray(fieldName,elementType,
                    property,fieldAttribute);
                break;
            default:
                field = fieldCreate.createField(fieldName,type,
                        property,fieldAttribute);
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
            String structureName = attributes.get("structureName");
            String factoryName = attributes.get("factoryName");
            if(factoryName==null||factoryName.length()==0) {
                iocxmlReader.message(
                    "factoryName was not specified correctly",
                    MessageType.error);
                return;
            }
            support = dbd.createSupport(
                name,factoryName,structureName);
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
}
