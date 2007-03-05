/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.RequestResult;

/**
 * This is a support that does nothing except act like it connects, etc.
 * @author mrk
 *
 */
public class NoopFactory {
    /**
     * Factory creation method.
     * @param dbStructure The structure to support.
     * @return The Support interface.
     */
    public static Support create(DBStructure dbStructure) {
        return new Noop(dbStructure);
    }
    
    public static Support create(DBField dbField) {
        return new Noop(dbField);
    }
    
    public static LinkSupport create(DBLink dbLink) {
        return new Noop(dbLink);
    }
    
    private static class Noop extends AbstractSupport implements LinkSupport {
        private static String supportName = "noop";
        
        private Noop(DBField dbField) {
            super(supportName,dbField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#getRequestorName()
         */
        public String getRequestorName() {
            return supportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            super.setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            super.setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            super.setSupportState(SupportState.readyForStart);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            // nothing to do
        }
    }
}
