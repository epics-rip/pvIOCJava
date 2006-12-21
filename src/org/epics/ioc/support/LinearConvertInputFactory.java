/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;


/**
 * @author mrk
 *
 */
public class LinearConvertInputFactory {
    public static Support create(PVStructure pvStructure) {
        Support support = null;
        String supportName = pvStructure.getSupportName();
        if(supportName.equals(supportName)) {
            support = new LinearConvert(pvStructure);
        }
        return support;
    }
    
    private static final String supportName = "linearConvert";
    
    private static class LinearConvert extends AbstractSupport {
        
        private PVStructure pvStructure = null;
        
        public LinearConvert(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
            this.pvStructure = pvStructure;
        }

        public String getRequestorName() {
            return supportName;
        }

        public void initialize() {
            System.out.printf("%s.initialize entered\n",supportName);
        }
        public void uninitialize() {
            System.out.printf("%s.uninitialize entered\n",supportName);
        }

        public void start() {
            System.out.printf("%s.start entered\n",supportName);
        }

        public void stop() {
            System.out.printf("%s.stop entered\n",supportName);
        }

        public void process(SupportProcessRequestor supportProcessRequestor) {
            System.out.printf("%s.process entered\n",supportName);
            supportProcessRequestor.supportProcessDone(RequestResult.failure);
        }

        public void processContinue() {
        }
    }
}
