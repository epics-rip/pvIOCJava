/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * The reader for IOC XML processing.
 * 
 * The implementation will supply filenames and line and character positions.
 * @author mrk
 *
 */
public interface IOCXMLReader {
    /**
     * Parse an XML file.
     * Include and macro substitution are done before the listener is called.
     * If a file is already being parsed the listener will be given an error message and parse will just return.
     * All errors result in a call to listener.errorMessage.
     * @param rootElementName The root element tag name.
     * The root file and any included files must have the same rootElementName.
     * @param fileName The file.
     * @param listener The callback listener.
     */
    void parse(String rootElementName,String fileName, IOCXMLListener listener);
    /**
     * Warning message.
     * The current location in the xml files together with the massage are given to listener.errorMessage.
     * @param message The message.
     */
    void warningMessage(String message);
    /**
     * Error message.
     * The current location in the xml files together with the massage are given to listener.errorMessage.
     * @param message The message.
     */
    void errorMessage(String message);
    /**
     * Fatal error message.
     * The current location in the xml files together with the massage are given to listener.errorMessage.
     * @param message The message.
     */
    void fatalMessage(String message);
}
