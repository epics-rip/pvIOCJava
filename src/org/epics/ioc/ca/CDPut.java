/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Channel put using a CD (Channel Data) to hold the data..
 * @author mrk
 *
 */
public interface CDPut {
    /**
     * Destroy the cdPut.
     */
    void destroy();
    /**
     * Get the latest value of the target data. The record will NOT be processed before the data is read.
     * If the request fails then CDPutRequester.getDone is called before get returns.
     * @param cd The CD into which the data should be put.
     */
    void get(CD cd);
    /**
     * Get the latest value of the target data.
     * If the request fails then CDPutRequester.putDone is called before put returns.
     * @param cd The CD from which to get the data.
     */
    void put(CD cd);
}
