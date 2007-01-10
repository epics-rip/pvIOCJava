/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;
import java.util.*;

/**
 * Schedule a comman to be executed via a thread.
 * An IOCExecutor is created via IOCExecutorFactory.
 * @author mrk
 *
 */
public interface IOCExecutor {
    /**
     * Execute a command via a thread.
     * @param command The interface for the command.
     * @param priority The thread priority.
     */
    void execute(Runnable command,ScanPriority priority);
    /**
     * Execute a list of commands via a thread.
     * @param commands The interface for the command.
     * @param priority The thread priority.
     */
    void execute(List<Runnable> commands,ScanPriority priority);
}
