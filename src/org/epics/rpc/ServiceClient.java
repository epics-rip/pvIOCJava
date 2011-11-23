/**
 * 
 */
package org.epics.rpc;
import org.epics.pvData.pv.*;

/**
 * Interface that is called by a service client.
 * @author mrk
 *
 */
public interface ServiceClient {
	/**
	 * Called by client when the service is no longer required.
	 */
	void destroy();
	/**
	 * Called by client to wait for connection to the service.
	 * This call blocks until a connection is made or until a timeout occurs.
	 * A connection means that a channel connects and a putProcessGet has been created.
	 * @param timeout The time in seconds to wait for the connection.
	 */
	void waitConnect(double timeout);
	/**
	 * Send a request.
	 * @param pvArgument The argument for the rpc.
	 */
	void sendRequest(PVStructure pvArgument);
	/**
	 * Wait for the request to finish.
	 */
	void waitRequest();
}
