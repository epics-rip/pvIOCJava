/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.channelAccess.*;

/**
 * @author mrk
 *
 */
public class ProcessLinkArray {
    public static RecordSupport createSupport(DBArray array) {
        return new ProcessLinkArraySupport(array);
    }
    
    private static class ProcessLinkArraySupport implements RecordSupport {
        private static String supportName = "ProcessLinkArray";
        private DBArray dbArray;
        private DBLink[] dbLinks;
        private ProcessLink[] processLinks;
        
        ProcessLinkArraySupport(DBArray array) {
            dbArray = array;
        }

        public void linkSupportDone(LinkReturn result) {
            // TODO Auto-generated method stub
            
        }

        public ProcessReturn process(RecordProcess recordProcess) {
            // TODO Auto-generated method stub
            return null;
        }

        public void destroy() {
            // TODO Auto-generated method stub
            
        }

        public String getName() {
            return supportName;
        }

        public void initialize() {
            // TODO Auto-generated method stub
            
        }

        public void start() {
            // TODO Auto-generated method stub
            
        }

        public void stop() {
            // TODO Auto-generated method stub
            
        }
    }
}
