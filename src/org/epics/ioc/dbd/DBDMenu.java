/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

/**
 * reflection interface for menu fields.
 * @author mrk
 *
 */
public interface DBDMenu {
    /**
     * Get the menu name.
     * @return The name.
     */
    String getMenuName();
    /**
     * Get the menu choices.
     * @return The array of choices.
     */
    String[] getChoices();
    /**
     * Convert to a string
     * @param indentLevel Indentation level
     * @return The DBDMenu as a string
     */
    String toString(int indentLevel);
}
