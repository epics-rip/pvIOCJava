/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbProcess.*;

/**
 * @author mrk
 *
 */
public class ProcessLinkArrayFactory {
    public static Support create(DBArray dbArray) {
        Support support = null;
        String supportName = dbArray.getArraySupportName();
        if(supportName.equals("processLinkArray")) {
            support = new ProcessLinkArray(dbArray);
        }
        return support;
    }
    
    private static class ProcessLinkArray implements RecordSupport, LinkListener {
        private static String supportName = "ProcessLinkArray";
        private DBLinkArray dbLinkArray;
        private LinkArrayData linkArrayData = new LinkArrayData();
        private DBLink[] dbLinks = null;
        private LinkSupport[] processLinks = null;
        
        private ProcessListener listener;
        private int nextLink;
        private int numberWait;
        private ProcessReturn finalReturn = ProcessReturn.done;
       
        ProcessLinkArray(DBArray array) {
            dbLinkArray = (DBLinkArray)array;
        }
        public String getName() {
            return supportName;
        }

        public void initialize() {
            int n = dbLinkArray.getLength();
            dbLinks = new DBLink[n];
            processLinks = new LinkSupport[n];
            linkArrayData.data = dbLinks;
            linkArrayData.offset = 0;
            int nget = dbLinkArray.get(0,n,linkArrayData);
            for(int i=nget; i <n; i++) dbLinks[i] = null;
            for(int i=0; i< n; i++) {
                processLinks[i] = null;
                DBLink dbLink = dbLinks[i];
                if(dbLink==null) continue;
                processLinks[i] = (LinkSupport)dbLink.getLinkSupport();
                processLinks[i].initialize();
            }
        }
        public void start() {
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
                if(processLink==null) continue;
                processLink.start();
            }
        }

        public void stop() {
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
                if(processLink==null) continue;
                processLink.stop();
            }
        }
        public void destroy() {
            if(processLinks==null) return;
            for(LinkSupport processLink: processLinks) {
                if(processLink==null) continue;
                processLink.destroy();
            }
        }
        
        public ProcessReturn process(ProcessListener listener) {
            finalReturn = ProcessReturn.done;
            while(nextLink < processLinks.length) {
                LinkSupport processLink = processLinks[nextLink];
                if(processLink!=null) {
                    LinkReturn linkReturn = processLink.process(this);
                    switch(linkReturn) {
                    case noop: break;
                    case done: break;
                    case failure:
                        finalReturn = ProcessReturn.abort;
                        break;
                    case active:
                        numberWait++;
                        if(finalReturn==ProcessReturn.done) finalReturn = ProcessReturn.active;
                    }
                }
                nextLink++;
            }
            return finalReturn;
        }
        
        public void processComplete(LinkReturn result) {
            if(result==LinkReturn.active) return;
            if(result==LinkReturn.failure) finalReturn = ProcessReturn.abort;
            numberWait--;
            if(numberWait!=0) return;
            if(finalReturn==ProcessReturn.active) finalReturn = ProcessReturn.done;
            listener.processComplete(finalReturn);
        }
    }
}
