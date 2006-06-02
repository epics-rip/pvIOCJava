/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * This is a class that only DBData and DBRecord access.
 * This class and all its fields have default visibility.
 * @author mrk
 *
 */
class DBListenerPvt {
    /**
     * The actual listener
     */
    DBListener listener;
    /**
     * are synchronous puts active?
     * DBRecord sets this true when beginSynchronous is called
     * and false when stopSynchronous is called.
     * If sentSynchronous is true when stopSynchronous is called,
     * DBRecord calls listener.stopSynchronous.
     */
    boolean isSynchronous;
    /**
     * has a the dbListener been sent a beginSynchronous message?
     * When DBData.postData(dbData) is called it calls listener.beginSynchronous()
     * if isSynchronous is true and sentSynchronous is false.
     */
    boolean sentSynchronous;
}
