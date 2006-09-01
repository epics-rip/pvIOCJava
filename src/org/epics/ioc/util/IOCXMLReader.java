/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * The reader for IOC XML processing.
 * For now this is just to report errors.
 * The implementation will supply filenames and line and character positions.
 * @author mrk
 *
 */
public interface IOCXMLReader {
    /**
     * Warning message.
     * @param message The message.
     */
    void warningMessage(String message);
    /**
     * Error message.
     * @param message The message.
     */
    void errorMessage(String message);
    /**
     * Fatal error message.
     * @param message The message.
     */
    void fatalMessage(String message);
}
