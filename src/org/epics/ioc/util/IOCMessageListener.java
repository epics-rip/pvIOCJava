/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * 
 * Listener to receive IOC messages.
 * @author mrk
 *
 */
public interface IOCMessageListener {
    /**
     * Message.
     * This is called to report a message.
     * @param message The message.
     * @param messageType The type of message.
     */
    void message(String message,IOCMessageType messageType);
}
