/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbAccess.*;
import java.util.*;
import java.util.regex.*;

/**
 * @author mrk
 *
 */
public class IOCChannelFactory {
    public Channel createChannel(String name) {
        String fieldName = null;
        String[]fields =  periodPattern.split(name,2);
        DBRecord dbRecord = iocdb.findRecord(fields[0]);
        if(dbRecord==null) {
            // when available call remote channel access
            return null;
        }
        Channel channel = ChannelAccessLocal.createChannel(iocdb,dbRecord);
        if(fields.length==2) {
            if(!channel.setField(fields[1])) {
                channel.destroy();
                return null;
            }
        }
        return channel;
    }
    IOCChannelFactory(IOCDB iocdb) {
        this.iocdb = iocdb;
    }
    IOCDB iocdb;
    static private Pattern periodPattern = Pattern.compile("[.]");
}
