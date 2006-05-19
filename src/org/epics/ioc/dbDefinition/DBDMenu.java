/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

/**
 * reflection interface for menu fields.
 * @author mrk
 *
 */
public interface DBDMenu {
    /**
     * get the menu name.
     * @return the name.
     */
    String getName();
    /**
     * get the menu choices.
     * @return array of choices.
     */
    String[] getChoices();
}
