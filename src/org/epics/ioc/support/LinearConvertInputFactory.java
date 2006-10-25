/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.util.IOCMessageType;


/**
 * @author mrk
 *
 */
public class LinearConvertInputFactory {
    public static Support create(DBStructure dbStructure) {
        Support support = null;
        String supportName = dbStructure.getSupportName();
        if(supportName.equals("linearConvert")) {
            support = new LinearConvert(dbStructure);
        }
        return support;
    }
    
    private static class LinearConvert extends AbstractSupport {
        private static String supportName = "LinearConvert";
        private DBStructure dbStructure = null;
        
        public LinearConvert(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
        }

        public String getName() {
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

        public RequestResult process(SupportProcessRequestor supportProcessRequestor) {
            System.out.printf("%s.process entered\n",supportName);
            return RequestResult.failure;
        }

        public void processContinue() {
        }
    }
}
