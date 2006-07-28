/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.channelAccess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class AnalogInputFactory {
    public static Support create(DBStructure dbStructure) {
        Support support = null;
        String supportName = dbStructure.getStructureSupportName();
        if(supportName.equals("ai")) {
            support = new Ai(dbStructure);
        }
        return support;
    }
    
    private static class Ai implements RecordSupport {
        private static String supportName = "Ai";
        private DBRecord dbRecord = null;
        private DBStructure dbStructure = null;
        
        public Ai(DBStructure dbStructure) {
            this.dbStructure = dbStructure;
        }

        public String getName() {
            return supportName;
        }
        
        public void destroy() {
            System.out.printf("%s.destroy entered\n",supportName);
            dbRecord = null;
            dbStructure = null;
        }

        public void start() {
            System.out.printf("%s.start entered\n",supportName);
        }

        public void stop() {
            System.out.printf("%s.stop entered\n",supportName);
        }

        public void initialize() {
            System.out.printf("%s.initialize entered\n",supportName);
        }

        public ProcessReturn process(ProcessListener listener) {
            System.out.printf("%s.process entered\n",supportName);
            return ProcessReturn.noop;
        }
        public void linkSupportDone(LinkReturn result) {
            // TODO Auto-generated method stub
            
        }
    }
}
