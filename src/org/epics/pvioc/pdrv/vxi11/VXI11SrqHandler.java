/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;

public interface VXI11SrqHandler {
    /**
     * This method is invoked when the controller sends a Service Request message.
     * The callback method will be invoked from the thread created by the
     * when the {@link VXI11Controller#registerSrqHandler registerSrqHandler}
     * method was invoked.
     * @param controller The IEEE488 controller object which received the service request message.
     */
    public void srqHandler(VXI11Controller controller);

}
