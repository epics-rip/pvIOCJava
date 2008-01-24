/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;
import java.util.List;

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
     */
    void execute(Runnable command);
    /**
     * Execute a list of commands via a thread.
     * @param commands The interface for the command.
     */
    void execute(List<Runnable> commands);
}
