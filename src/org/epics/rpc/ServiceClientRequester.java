/**
 * 
 */
package org.epics.rpc;

import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.Status;


/**
 * The interface implemented by a service requester.
 * @author mrk
 *
 */
public interface ServiceClientRequester extends Requester{
	/**
	 * The connection request result.
	 * @param status The status. Unless status.isOK is true then the connection failed.
	 */
	void connectResult(Status status);
	/**
	 * The result returned for a sendRequest.
	 * @param status The status. Unless status.isOK is true then the request failed.
	 * @param pvResult A pvStructure that hold the result of a service request.
	 */
	void requestResult(Status status,PVStructure pvResult);
}
