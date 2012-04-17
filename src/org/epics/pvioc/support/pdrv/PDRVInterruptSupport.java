/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv;



/**
 * Extension of PortDriverSupport
 * @author mrk
 *
 */
public interface PDRVInterruptSupport extends PortDriverSupport {
    /**
     * If the support is interrupt driver should it process the record.
     * The answer will be true only if:
     * <ol>
     *    <li>The constructor for AbstractPDRVSupport is called with interruptOK true</li>
     *    <li>pdrvLink.interrupt is true.</li>
     *    <li>The support is the record processor.</li>
     * </ol>
     * @return (false,true) if the record should be processed.
     */
    boolean isProcess();
}
