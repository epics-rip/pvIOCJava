/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.*;

import org.epics.ioc.util.*;

/**
 * Base class for a record instance.
 * @author mrk
 *
 */
public class BasePVRecord extends BasePVStructure implements PVRecord {
    private String recordName;
    private List<Requestor> requestorList = new ArrayList<Requestor>();
    
    /**
     * Constructor.
     * @param recordName The name of the record.
     * @param dbdRecordType The introspection interface for the record.
     */
    public BasePVRecord(String recordName,Structure structure)
    {
        super(null,structure);
        this.recordName = recordName;
        super.setRecord(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        if(message!=null && message.charAt(0)!='.') message = " " + message;
        message = recordName + message;
        for (Requestor requestor : requestorList) requestor.message(message, messageType);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#addRequestor(org.epics.ioc.util.Requestor)
     */
    public void addRequestor(Requestor requestor) {
        requestorList.add(requestor);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#removeRequestor(org.epics.ioc.util.Requestor)
     */
    public void removeRequestor(Requestor requestor) {
        requestorList.remove(requestor);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.BasePVStructure#toString(int)
     */
    public String toString(int indentLevel) {
        return super.toString(recordName + " recordType",indentLevel);
    }
}
