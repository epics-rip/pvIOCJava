/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.net.*;

import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.*;

/**
 * IOCXMLReaderFactory implements IOCXMLReader.
 * Only one reader is created and getReader returns the instance.
 * @author mrk
 *
 */
public class IOCXMLReaderFactory {
    
    static private IOCReader iocReader = new IOCReader();
    static private IOCXMLListener listener = null;
    static private String rootElementName = null;
    static private Map<String,String> substituteMap = new TreeMap<String,String>();
    static private List<String> pathList = new ArrayList<String>();
    static private Pattern separatorPattern = Pattern.compile("[, ]");
    static private Pattern equalPattern = Pattern.compile("[=]");
    
    /**
     * Get the IOCXMLReader.
     * @return The reader.
     */
    static public IOCXMLReader getReader() {
        return iocReader;
    }
    
    private static class IOCReader implements IOCXMLReader {
        private AtomicBoolean isInUse = new AtomicBoolean(false);
        private Handler currentHandler = null;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCXMLReader#parse(java.lang.String, java.lang.String, org.epics.ioc.util.IOCXMLListener)
         */
        public void parse(String rootElementName,String fileName, IOCXMLListener listener) 
        {
            boolean gotIt = isInUse.compareAndSet(false,true);
            if(!gotIt) {
                listener.message("IOCReader is already active",MessageType.fatalError);
                return;
            }
            if(listener==null) {
                System.out.println("IOCXMLReader was called with a null listener");
                return;
            }
            try {
                IOCXMLReaderFactory.rootElementName = rootElementName;
                IOCXMLReaderFactory.listener = listener;
                IOCXMLReaderFactory.substituteMap.clear();
                IOCXMLReaderFactory.pathList.clear();
                create(null,fileName);
            } finally {
                isInUse.set(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCXMLReader#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            currentHandler.message(message,messageType);
            
        }
        
        private void setCurrentReader(Handler handler) {
            currentHandler = handler;
        }
        
        private Handler create(Handler parent,String fileName) throws IllegalStateException
        {
            String uri = null;
            try {
                uri = new File(fileName).toURL().toString();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(
                        String.format("%n")
                        + "IOCXMLReader.convert terminating with MalformedURLException"
                        + String.format("%n")
                        + e.getMessage());
            }
            XMLReader reader;
            Handler handler = new Handler(parent);
            try {
                reader = XMLReaderFactory.createXMLReader();
                reader.setContentHandler(handler);
                reader.setErrorHandler(handler);
                reader.parse(uri);
            } catch (SAXException e) {
                // nothing to do. ErrorHandler reports errors.
            } catch (IOException e) {
                throw new IllegalStateException(
                    String.format("%n")
                    + "IOCXMLReader.convert terminating with IOException"
                    + String.format("%n")
                    + e.getMessage());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                    String.format("%n")
                    + "IOCXMLReader.convert terminating with IllegalArgumentException"
                    + String.format("%n")
                    + e.getMessage());
            }
            return handler;
        }
    }
    private static class Handler implements ContentHandler, ErrorHandler {
        private Handler parent = null;
        private Locator locator;
        private int nInfo = 0;
        private int nWarning = 0;
        private int nError = 0;
        private int nFatal = 0;
        private boolean gotFirstElement = false;
        private StringBuilder charBuilder = new StringBuilder();
        
        private Handler(Handler parent) {
            this.parent = parent;
            iocReader.setCurrentReader(this);
        }
        
        private String showLocation() {
            String result;
            result = String.format("line %d column %d in %s%n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                locator.getSystemId());
            if(parent!=null) result += parent.showLocation();
            return result;
        }
        
        private void message(String message,MessageType messageType)
        {
            listener.message(String.format("%s %s%n%s",
                messageType.name(),message,showLocation()),
                messageType);
            switch(messageType) {
            case info:  nInfo ++; break;
            case warning: nWarning ++; break;
            case error: nError++; break;
            case fatalError: nFatal++; break;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        public void error(SAXParseException e) throws SAXException {
            message(e.toString(),MessageType.error);
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        public void fatalError(SAXParseException e) throws SAXException {
            message(e.toString(),MessageType.fatalError);
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        public void warning(SAXParseException e) throws SAXException {
            message(e.toString(),MessageType.warning);
        }

        private enum CharState {
            idle,
            got$,
            gotPrefix
        }
        private CharState charState = CharState.idle;
        
        public void characters(char[] ch, int start, int length) throws SAXException {
            switch(charState) {
            case idle:
                for(int i=0; i< length; i++) {
                    if(ch[start+i]=='$') {
                        if(i+1<length) {
                            if(ch[start+i+1]=='{') {
                                if(i>0) listener.characters(ch,start,i-start);
                                charState = CharState.got$;
                                characters(ch,start+i+1,length-(i+1));
                                return;
                            } else {
                                continue;
                            }
                        } else {
                            if(i>0) listener.characters(ch,start,i - start);
                            charState = CharState.got$;
                            return;
                        }
                    }
                }
                listener.characters(ch,start,length);
                return;
            case got$:
                if(ch[start]=='{') {
                    charState = CharState.gotPrefix;
                    charBuilder.setLength(0);
                    start++;
                    if(length>1) characters(ch,start,length-1);
                    return;
                }
                char[] str$ = new char[] {'$'};
                listener.characters(str$,0,1);
                charState = CharState.idle;
                if(length>1) listener.characters(ch,start+1,length-1);
                return;
            case gotPrefix:
                for(int i=0; i<length; i++) {
                    if(ch[start+i]=='}') {
                        if(i>0) charBuilder.append(ch,start,i);
                        String from = charBuilder.toString();
                        String to = substituteMap.get(from);
                        if(to!=null) {
                            char[] charArray = to.toCharArray();
                            listener.characters(charArray,0,charArray.length);
                        }
                        charState = CharState.idle;
                        if(i+1<length) characters(ch,start+i+1,length-(i+1));
                        return;
                    }
                }
                charBuilder.append(ch,start,length);
                return;
            }
        }

        public void endDocument() throws SAXException {
            if(parent==null) listener.endDocument();
            if(nWarning>0 || nError>0 || nFatal>0) {
                message(String.format("%s endDocument: warning %d severe %d fatal %d",
                    locator.getSystemId(),nWarning,nError,nFatal),MessageType.info);
            }
            iocReader.setCurrentReader(parent);
            parent = null;
            locator = null;
        }
         
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if(!gotFirstElement) {
                gotFirstElement = true;
                if(!qName.equals(rootElementName)) {
                    message(
                        "rootElementName is " + qName +
                        " but expected " + rootElementName,
                        MessageType.error);
                }
                return;
            }
            if(qName.equals("include")) {
                includeElement(atts);
                return;
            }
            if(qName.equals("substitute")) {
                substituteElement(atts);
                return;
            }
            charBuilder.setLength(0);
            Map<String,String> attributes = new TreeMap<String,String>();
            for(int i=0; i<atts.getLength(); i++) {
                String name = atts.getQName(i);
                String value = atts.getValue(i);
                int prefix = value.indexOf("${");
                int end = 0;
                if(prefix>=0) {
                    end = value.indexOf("}",prefix);
                    if(end<0 || (end-prefix)<3) {
                        message("attribute " + name + " has bad value",
                                MessageType.error);
                    } else {
                        StringBuilder builder = new StringBuilder();
                        if(prefix>0) builder.append(value.substring(0,prefix));
                        String temp = value.substring(prefix+2,end);
                        temp = substituteMap.get(temp);
                        if(temp==null) {
                            message("attribute " + name + " no substitution found",
                                    MessageType.error);
                        } else {
                            builder.append(temp);
                        }
                        if(end+1<value.length()) {
                            builder.append(value.substring(end+1));
                        }
                        value = builder.toString();
                     }
                }
                attributes.put(name,value);
            }
            listener.startElement(qName,attributes);
        }
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(qName.equals(rootElementName)) return;
            if(qName.equals("include")) return;
            if(qName.equals("substitute")) return;
            listener.endElement(qName);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void processingInstruction(String target, String data) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void skippedEntity(String name) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // TODO Auto-generated method stub
            
        }
        
        private void includeElement(Attributes atts) {
            String removePath = atts.getValue("removePath");
            if(removePath!=null) {
                if(!pathList.remove(removePath)) {
                    message("path " + removePath + " not in pathList",
                            MessageType.warning);
                }
            }
            String addPath = atts.getValue("addPath");
            if(addPath!=null) {
                pathList.add(0,addPath);
            }
            String href = atts.getValue("href");
            if(href==null) {
                if(removePath==null && addPath==null) {
                    message("no attribute was recognized",
                            MessageType.warning);
                }
                return;
            }
            if(pathList.size()>0) {
                href = pathList.get(0) + File.separator + href; 
            }
            iocReader.create(this,href);
            return;
        }
        
        private void substituteElement(Attributes atts) {
            
            String remove = atts.getValue("remove");
            if(remove!=null) {
                if(substituteMap.remove(remove)==null) {
                    message(remove + " not found",
                            MessageType.warning);
                }
            }
            String from = atts.getValue("from");
            if(from!=null) {
                String to = atts.getValue("to");
                if(to==null) {
                    message("from without corresonding to",
                            MessageType.warning);
                } else {
                    substituteMap.put(from,to);
                }
            }
            String fromTo = atts.getValue("fromTo");
            if(fromTo==null) {
                if(remove==null && from==null) {
                    message("no attribute was recognized",
                            MessageType.warning);
                }
                return;
            }
            String[] items = separatorPattern.split(fromTo);
            for(String item : items) {
                String[] parts = equalPattern.split(item);
                if(parts.length!=2) {
                    message(item + " is not a valid substitution",
                            MessageType.warning);
                } else {
                    substituteMap.put(parts[0],parts[1]);
                }
            }
        }
    }
}
