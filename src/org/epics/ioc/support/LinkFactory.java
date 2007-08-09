/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * This is a support for a structure named link which must have a field named link.
 * @author mrk
 *
 */
public class LinkFactory {
    /**
     * Create support for a structure
     * that has a single field which must be a named "link" and must be a link..
     * @param dbStructure The structure to support.
     * @return The Support interface.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        Structure structure = (Structure)pvStructure.getField();
        if(!structure.getStructureName().equals("link")) {
            pvStructure.message("structureName is not link", MessageType.error);
            return null;
        }
        return new LinkImpl(dbStructure);
    }    
    
    private static class LinkImpl extends AbstractLinkSupport implements SupportProcessRequester {
        private static String supportName = "link";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private boolean noop;
        private LinkSupport linkSupport;
        private DBField dbField = null;
        private SupportProcessRequester supportProcessRequester;
        
        
        private LinkImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            pvStructure = dbStructure.getPVStructure();
            DBField[] dbFields = dbStructure.getFieldDBFields();
            Structure structure = (Structure)pvStructure.getField();
            int index = structure.getFieldIndex("link");
            if(index<0) {
                pvStructure.message("field link does not exist", MessageType.error);
                return;
            }
            DBLink dbLink = (DBLink)dbFields[index];
            linkSupport = (LinkSupport)dbLink.getSupport();
            if(linkSupport==null) {
                noop = true;
                super.setSupportState(SupportState.readyForStart);
            }
            if(dbField!=null)linkSupport.setField(dbField);
            linkSupport.initialize();
            super.setSupportState(linkSupport.getSupportState());
            return;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(!noop) linkSupport.uninitialize();
            super.setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            if(noop) {
                super.setSupportState(SupportState.ready);
                return;
            }
            linkSupport.start();
            super.setSupportState(linkSupport.getSupportState());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(!noop) linkSupport.stop();
            super.setSupportState(SupportState.readyForStart);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(noop) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
            }
            this.supportProcessRequester = supportProcessRequester;
            linkSupport.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            this.dbField = dbField;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            supportProcessRequester.supportProcessDone(requestResult);
        }
    }
}
