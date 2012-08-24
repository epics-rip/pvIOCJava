/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */

package org.epics.pvioc.pvCopy;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;

/**
 * @author mrk
 *
 */
public class RequesterForTesting implements Requester {
	private String requesterName = null;
	
	public RequesterForTesting(String requesterName) {
		this.requesterName = requesterName;
	}
	/* (non-Javadoc)
     * @see org.epics.pvioc.util.Requester#getRequestorName()
     */
    public String getRequesterName() {
        return requesterName;
    }

    /* (non-Javadoc)
     * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        System.out.println(message);
        
    }
}
