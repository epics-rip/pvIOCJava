/**
 * 
 */
package org.epics.ioc.dbAccess;

import java.util.*;
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
    public static void convert(DBD dbd, IOCDB iocdb, String fileName)
        throws MalformedURLException//,IllegalStateException
    {
        String uri = new File(fileName).toURL().toString();
        XMLReader reader;
        
        Handler handler = new Handler(dbd,iocdb);
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
        
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
            recordHandler = new RecordHandler(dbd,iocdb,this,locator);
        }
        
        public void startDocument() throws SAXException {
            state = State.startDocument;
        }
        
        
        public void endDocument() throws SAXException {
            if(nWarning>0 || nError>0 || nFatal>0) {
                    System.err.printf("endDocument: warning %d severe %d fatal %d\n",
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
                    System.err.printf("startElement element %s not understood\n",qName);
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
                    System.err.printf("startElement element %s not understood\n",qName);
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

        Handler(DBD dbd, IOCDB iocdb)  throws MalformedURLException {
            this.dbd = dbd;    
            this.iocdb = iocdb;
        }
        
        private enum State {
            startDocument,
            idle,
            record,
        } 
        private DBD dbd;
        private IOCDB iocdb;
        private Locator locator = null;
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

    private static class RecordHandler extends DBDXMLHandler{
    
        void start(String qName, Attributes attributes)
        throws SAXException {
            stringBuilder.setLength(0);
            buildString = false;
            recordName = attributes.getValue("name");
            recordTypeName = attributes.getValue("type");
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
            dbdRecordType = dbd.getDBDRecordType(recordTypeName);
            if(dbdRecordType==null) {
                errorHandler.warning(new SAXParseException(
                    "record type " + recordTypeName + " does not exist.",
                    locator));
                state = State.idle;
            } else {
                dbRecord = iocdb.getRecord(recordName);
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
                dbStructure = dbRecord;
                dbData = dbStructure.getFieldDBDatas();
                dbStructureList.clear();
            }
        }
    
        void end(String qName) throws SAXException {
            state= State.idle;
        }
    
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            stringBuilder.setLength(0);
            buildString = false;
            switch(state) {
            case idle: return;
            case structure: 
                startStructureElement(qName,attributes);
                return;
            case field:
            case menu:
                errorHandler.error(new SAXParseException(
                    qName + " not found",
                    locator));
                    state = State.idle;
            case enumerated:
                startEnumElement(qName,attributes);
                return;
            case array:
                startArrayElement(qName,attributes);
                return;
            case link:
                startLinkElement(qName,attributes);
                return;
            }     
        }
 
        void endElement(String qName) throws SAXException {
            switch(state) {
            case idle: return;
            case structure:
                endStructureElement(qName);
                return;
            case field: 
                state = State.structure;
                convert.fromString(dbData[dbDataIndex],
                    stringBuilder.toString());
                return;
            case enumerated:
                endEnumElement(qName);
                return;
            case menu:
                state = State.structure;
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
            case array:
                endArrayElement(qName);
                return;
            case link:
                endLinkElement(qName);
                return;
            }
        }
  
        private void startStructureElement(String qName, Attributes attributes)
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
            DBData data = dbData[dbDataIndex];
            DBDField dbdField = data.getDBDField();
            DBType dbType = dbdField.getDBType();
            switch(dbType) {
            case dbPvType: {
                Type type= dbdField.getType();
                if(type.isScalar()) {
                    state = State.field;
                    buildString = true;
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
                enumChoiceList.clear();
                return;
            }
            case dbMenu:
                state = State.menu;
                buildString = true;
                return;
            case dbStructure:
                dbStructureList.add(dbStructure);
                dbStructure = (DBStructure)data;
                dbData = dbStructure.getFieldDBDatas();
                state = State.structure;
                return;
            case dbArray:
                state = State.array;
                arrayState = ArrayState.array;
                arrayOffset = 0;
                dbArray = (DBArray)data;
                String capacity = attributes.getValue("capacity");
                if(capacity!=null) {
                    dbArray.setCapacity(Integer.parseInt(capacity));
                }
                DBDArrayField dbdArrayField = (DBDArrayField)dbdField;
                arrayElementType = dbdArrayField.getElementType();
                arrayElementDBType = dbdArrayField.getDBType();
                return;
            case dbLink:
                startLinkField(qName,attributes);
                return;
            }
        }
        
        void endStructureElement(String qName) throws SAXException {
            if(dbStructureList.size()>0) {
                dbStructure = dbStructureList.getFirst();
                dbData = dbStructure.getFieldDBDatas();
                state = State.structure;
                return;
            }
            errorHandler.error(new SAXParseException(
                qName + " Logic error. Why in  endStructureList?",
                locator));
            state = State.idle;
        }
         
        private void startEnumElement(String qName, Attributes attributes)
        throws SAXException
        {
            if(!qName.equals("choice")) {
                errorHandler.error(new SAXParseException(
                    qName + " not choice ",
                    locator));
                    state = State.idle;
                    return;
            }
            state = State.enumerated;
            buildString = true;
        }
        
        void endEnumElement(String qName) throws SAXException {
            buildString = false;
            switch(enumState) {
            case enumerated:
                String[] choices = new String[enumChoiceList.size()];
                for(int i=0; i< choices.length; i++) {
                    choices[i] = enumChoiceList.removeFirst();
                }
                state = State.structure;
                return;
            case choice:
                String choice = stringBuilder.toString();
                enumChoiceList.add(choice);
                return;
            }
        }

        
        private void startArrayElement(String qName, Attributes attributes)
        throws SAXException
        {
            if(arrayState!=ArrayState.array) {
                errorHandler.error(new SAXParseException(
                    qName + " Logic error. Why in startArrayElement ",
                    locator));
                state = State.idle;
                return;
            }
            if(qName.equals("offset")) {
                arrayState = ArrayState.offset;
                buildString = true;
                return;
            }
            if(qName.equals("value")) {
                arrayState = ArrayState.value;
                buildString = true;
                return;
            }
            errorHandler.error(new SAXParseException(
                    qName + " is illegal. Must be offset or value. ",
                    locator));
            state = State.idle;
            return;
        }

        void endArrayElement(String qName) throws SAXException {
            String value = stringBuilder.toString();
            String[] values = value.split("[, ]");
            
            buildString = false;
            switch(arrayState) {
            case offset:
                arrayOffset = Integer.valueOf(value);
                arrayState = ArrayState.array;
                return;
            case value:
                // TODO must implement other types
                if(arrayElementType.isScalar()) {
                    try {
                        int num = convert.fromStringArray(
                           dbData[dbDataIndex],arrayOffset,values.length,values,0);
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
                arrayState = ArrayState.array;
                return;
            case array:
                state = State.structure;
                return;
            }
        }
        
        private void startLinkField(String qName, Attributes attributes)
        throws SAXException {
            linkFieldName = qName;
            String linkSupport = attributes.getValue("linkSupport");
            String configStructName = attributes.getValue("configStructName");
            DBLink dbLink = (DBLink)dbData[dbDataIndex];
            dbLink.putConfigStructureFieldName(configStructName);
            dbLink.putLinkSupportName(linkSupport);
            state = State.link;
        }
        
        private void startLinkElement(String qName, Attributes attributes)
        throws SAXException {
            // TODO
        }
        
        void endLinkElement(String qName) throws SAXException {
            //MORE TODO
            if(linkFieldName.equals(qName)) {
                state = State.structure;
            }
        }
        
        void characters(char[] ch, int start, int length)
        throws SAXException {
            if(!buildString) return;
            while(start<ch.length && length>0 && ch[start]==' ') {
                start++; length--;
            }
            while(length>0 && ch[start+ length-1]==' ') length--;
            if(length<=0) return;
            stringBuilder.append(ch,start,length);            
        }
        
        RecordHandler(DBD dbd, IOCDB iocdb,
            ErrorHandler errorHandler, Locator locator)
        {
            super();
            this.dbd = dbd;
            this.iocdb = iocdb;
            this.errorHandler = errorHandler;
            this.locator = locator;
            dbStructureList = new LinkedList<DBStructure>();
            stringBuilder = new StringBuilder();
            enumChoiceList = new LinkedList<String>();
        }
            
        private static Convert convert = ConvertFactory.getPVConvert();
        
        private DBD dbd;
        private IOCDB iocdb;
        private ErrorHandler errorHandler;
        private Locator locator;
        private String recordTypeName;
        private DBDRecordType dbdRecordType;
        private String recordName;
        private DBRecord dbRecord;
        private State state = State.idle;
        
        private LinkedList<DBStructure> dbStructureList;
        
        private DBStructure dbStructure;
        private DBData[] dbData;
        private int dbDataIndex;
        
        private boolean buildString;
        private StringBuilder stringBuilder;
        
        private LinkedList<String> enumChoiceList;
        private EnumState enumState;
        
        private ArrayState arrayState;
        private DBArray dbArray;
        private int arrayOffset;
        private Type arrayElementType;
        private DBType arrayElementDBType;
        
        private String linkFieldName;
        
        
        private enum State {idle, structure, field, enumerated, menu, array, link}
        private enum EnumState {enumerated,choice}
        private enum ArrayState {array,offset,value}
    
    }
    
}
