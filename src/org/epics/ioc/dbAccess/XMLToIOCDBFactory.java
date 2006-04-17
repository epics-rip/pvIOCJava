/**
 * 
 */
package org.epics.ioc.dbAccess;

import java.io.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.epics.ioc.dbDefinition.*;


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
                throw new IllegalStateException(
                    String.format("endDocument: warning %d severe %d fatal %d\n",
                    nWarning,nError,nFatal));
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
                state = State.idle;
                dbRecord = iocdb.getRecord(recordName);
                if(dbRecord==null) {
                    boolean result = iocdb.createRecord(recordName,dbdRecordType);
                    if(!result) {
                        errorHandler.warning(new SAXParseException(
                                "failed to create record " + recordTypeName,
                                locator));
                            state = State.idle;
                    } else {
                        state = State.record;
                    }
                }
                state = State.record;
            }
        }
    
        void end(String qName) throws SAXException {
            if(state==State.idle) return;
            
            state= State.idle;
        }
    
        void startElement(String qName, Attributes attributes)
        throws SAXException {
            if(state==State.idle) return;
            
        }
    
        void endElement(String qName) throws SAXException {
            if(state==State.idle) return;
            
        }
    
        void characters(char[] ch, int start, int length)
        throws SAXException {
            if(state==State.idle) return;
            
        }
        
        RecordHandler(DBD dbd, IOCDB iocdb,
            ErrorHandler errorHandler, Locator locator)
        {
            super();
            this.dbd = dbd;
            this.iocdb = iocdb;
            this.errorHandler = errorHandler;
            this.locator = locator;
        }
            
        
        private DBD dbd;
        private IOCDB iocdb;
        private ErrorHandler errorHandler;
        private Locator locator;
        private String recordTypeName;
        private DBDRecordType dbdRecordType;
        private String recordName;
        private DBRecord dbRecord;
        private State state = State.idle;
        
        private enum State {idle, record}
    
    }
    
}
