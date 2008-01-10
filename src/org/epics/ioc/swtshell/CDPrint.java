/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

/**
 * Print contents of a CD.
 * @author mrk
 *
 */
public interface CDPrint {
    /**
     * Print all valuies in the CD that have been modified.
     */
    void print();
}
