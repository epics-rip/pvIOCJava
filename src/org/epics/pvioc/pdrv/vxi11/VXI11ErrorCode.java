/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;

public enum VXI11ErrorCode {
    noError,
    syntaxError,
    deviceNotAccessable,
    invalidLinkIdentifier,
    parameterError,
    channelNotEstablished,
    operationNotSupported,
    outOfResources,
    deviceLockedByAnotherLink,
    noLockHeldByThisLink,
    IOTimeout,
    IOError,
    invalidAddress,
    abort,
    channelAlreadyEstablished,
    unknownHostException,
    IOException,
    RPCException,
    unknown;
}
