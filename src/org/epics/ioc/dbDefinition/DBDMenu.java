/**
 * 
 */
package org.epics.ioc.dbDefinition;

/**
 * menu database definition
 * @author mrk
 *
 */
public interface DBDMenu {
    /**
     * get the menu name
     * @return the name
     */
    String getName();
    /**
     * get the menu choices
     * @return array of choices
     */
    String[] getChoices();
}
