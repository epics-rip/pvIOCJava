/**
 * 
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
