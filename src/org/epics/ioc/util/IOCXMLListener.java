/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.Map;

/**
 * Callbacks for a listener for IOC XML processing.
 * @author mrk
 *
 */
public interface IOCXMLListener {
    /**
     * No more input. 
     */
    void endDocument();
    /**
     * Start of a new element.
     * @param name The element tag name.
     * @param attributes Attributes for the element.
     */
    void startElement(String name, Map<String,String> attributes);
    /**
     * Some characters for the element.
     * @param ch The array of characters.
     * @param start The index of the first character.
     * @param length The number of characters.
     */
    void characters(char[] ch, int start, int length);
    /**
     * The end of the element.
     * @param name The tag name.
     */
    void endElement(String name);
}
