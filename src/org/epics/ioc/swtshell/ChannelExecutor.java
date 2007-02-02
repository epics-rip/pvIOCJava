/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.epics.ioc.ca.*;
import org.epics.ioc.util.*;
import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class ChannelExecutor implements Runnable, ChannelGetRequestor, ChannelFieldGroupListener {
    private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell");
    private Requestor requestor;
    private Channel channel;
    private ChannelGet channelGet;
    private ChannelField channelField;
    private Runnable runnable;
    private ScanPriority scanPriority = ScanPriority.lower;
    
    public ChannelExecutor(Requestor requestor,Channel channel) {
        this.requestor = requestor;
        this.channel = channel;
        ChannelFindFieldResult result = channel.findField("scan.priority");
        if(result==ChannelFindFieldResult.thisChannel) {
            channelField = channel.getChannelField();
            channelGet = channel.createChannelGet(this, false);
            iocExecutor.execute(this, scanPriority);
        }
    }
    
    public void request(Runnable runnable) {
        this.runnable = runnable;
        iocExecutor.execute(this, scanPriority);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if(runnable!=null) {
            runnable.run();
            runnable = null;
            return;
        }
        ChannelFieldGroup channelFieldGroup = channel.createFieldGroup(this);
        channelFieldGroup.addChannelField(channelField);
        boolean result = channelGet.get(channelFieldGroup);
        if(!result) {
            requestor.message("channelGet scan.priority failed", MessageType.error);
        }
        channel = null;
        channelField = null;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequestor#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        // nothing to do
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pv.PVData)
     */
    public boolean nextDelayedGetData(PVData data) {
        // nothing to do
        return false;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVData)
     */
    public boolean nextGetData(ChannelField field, PVData data) {
        PVMenu priorityMenu = (PVMenu)data;
        if(ScanFieldFactory.isPriorityMenu(priorityMenu)) {
            String[] choices = priorityMenu.getChoices();
            scanPriority = ScanPriority.valueOf(choices[priorityMenu.getIndex()]);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requestor#getRequestorName()
     */
    public String getRequestorName() {
        return requestor.getRequestorName();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        requestor.message(message, messageType);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
     */
    public void accessRightsChange(Channel channel, ChannelField channelField) {
        // nothing to do;
    }
}
