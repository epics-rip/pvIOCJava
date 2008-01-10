/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

/**
 * Shell to select the name of a local JavaIOC record.
 * @author mrk
 *
 */
public interface SelectLocalRecord {
    /**
     * Prompt for and return the name of a local javaIOOC record.
     * @return The name of the record.
     */
    public String getRecordName();
}
