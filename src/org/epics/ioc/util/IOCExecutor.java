/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;
import java.util.*;

/**
 * @author mrk
 *
 */
public interface IOCExecutor {
    void execute(Runnable command,ScanPriority priority);
    void execute(List<Runnable> commands,ScanPriority priority);
}
