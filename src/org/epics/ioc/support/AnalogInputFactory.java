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
 * This is not implemented.
 * @author mrk
 *
 */
public class AnalogInputFactory {
    /**
     * Factory creation method.
     * @param pvStructure The structure to support.
     * @return The Support interface.
     */
    public static Support create(PVStructure pvStructure) {
        Support support = null;
        String supportName = pvStructure.getSupportName();
        if(supportName.equals(supportName)) {
            support = new Ai(pvStructure);
        }
        return support;
    }
    
    private static final String supportName = "ai";
    
    private static class Ai extends AbstractSupport {
        private DBRecord dbRecord = null;
        
        private Ai(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
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
            System.out.printf("%s.initialize entered\n",supportName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            System.out.printf("%s.uninitialize entered\n",supportName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            System.out.printf("%s.start entered\n",supportName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            System.out.printf("%s.stop entered\n",supportName);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            System.out.printf("%s.process entered\n",supportName);
            supportProcessRequestor.supportProcessDone(RequestResult.failure);
        }
    }
}
