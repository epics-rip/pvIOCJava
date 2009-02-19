/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mrk
 *
 */
public class BaseChannelFieldGroup implements ChannelFieldGroup {
    private LinkedList<ChannelField> fieldList = 
        new LinkedList<ChannelField>();

    public BaseChannelFieldGroup(Channel channel,ChannelFieldGroupListener listener) {
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroup#addChannelField(org.epics.ioc.ca.ChannelField)
     */
    public synchronized void addChannelField(ChannelField channelField) {
        fieldList.add(channelField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroup#removeChannelField(org.epics.ioc.ca.ChannelField)
     */
    public synchronized void removeChannelField(ChannelField channelField) {
        fieldList.remove(channelField);
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroup#getList()
     */
    public synchronized List<ChannelField> getList() {
        return fieldList;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroup#getArray()
     */
    public synchronized ChannelField[] getArray() {
        int length = fieldList.size();
        ChannelField[] channelFields = new ChannelField[length];
        for(int i=0; i<length; i++) {
            channelFields[i] = fieldList.get(i);
        }
        return channelFields;
    }
}
