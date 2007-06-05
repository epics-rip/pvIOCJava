/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Interface;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
/**
 * Interface for communication options to a driver.
 * @author mrk
 *
 */
public interface Option extends Interface{
    /**
     * Set an option.
     * @param user The user.
     * @param key The option key.
     * @param value The option value.
     * @return Status of the request.
     * If not Status.success user.getMessage() contains the reason.
     */
    Status setOption(User user,String key, String value);
    /**
     * Get the option value for key.
     * @param user The user.
     * @param key The option key.
     * @return The value or null if there is not value for the key.
     * If nulll then user.getMessage() contains the reason.
     */
    String getOption(User user,String key);
}
