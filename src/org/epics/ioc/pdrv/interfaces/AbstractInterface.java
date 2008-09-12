/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Trace;

/**
 * @author mrk
 *
 */
public abstract class AbstractInterface implements Interface {
    protected Device device;
    protected String interfaceName; 
    protected Trace trace;
    
	/**
	 * Constructor
	 * @param device The device
	 * @param interfaceName The interfaceName.
	 */
	protected AbstractInterface(Device device,String interfaceName) {
		this.device = device;
		this.interfaceName = interfaceName;
		trace = device.getTrace();
		device.registerInterface(this);
	}
	/**
     * Generate a trace message.
     * @param reason One of ERROR|SUPPORT|INTERPOSE|DRIVER|FLOW.
     * @param message The message to print
     */
    protected void print(int reason,String message) {
    	if((reason&trace.getMask())==0) return;
        trace.print(reason,
        	"port " + device.getPort().getPortName()
        	+ ":" + device.getDeviceName() + " "+ message);
    }
    /**
     * Generate a trace message.
     * @param reason One of ERROR|SUPPORT|INTERPOSE|DRIVER|FLOW.
     * @param format A format.
     * @param args The data associated with the format.
     */
    protected void print(int reason,String format, Object... args) {
    	if((reason&trace.getMask())==0) return;
    	trace.print(reason,format,
            	"port " + device.getPort().getPortName()
            	+ ":" + device.getDeviceName() + " " + args);
    }
	/* (non-Javadoc)
	 * @see org.epics.ioc.pdrv.interfaces.Interface#getDevice()
	 */
	public Device getDevice() {
		return device;
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.pdrv.interfaces.Interface#getInterfaceName()
	 */
	public String getInterfaceName() {
		return interfaceName;
	}
}
