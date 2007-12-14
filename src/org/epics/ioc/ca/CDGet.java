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
public interface CDGet {
    /**
     * Destroy the cdGet.
     */
    void destroy();
    /**
     * Get the target data and put it into the CD.
     * If the request fails then CDGetRequester.getDone is called before get returns..
     * @param cd The CD into which the data should be put.
     */
    void get(CD cd);
}
