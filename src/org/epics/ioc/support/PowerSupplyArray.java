/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
/**
 * @author mrk
 *
 */
public class PowerSupplyArray implements RecordSupport {
    private static String supportName = "PowerSupplyArray";
    private DBRecord dbRecord = null;
    
    public PowerSupplyArray(DBRecord dbRecord) {
        this.dbRecord = dbRecord;
    }

    public String getName() {
        return supportName;
    }
    
    public void destroy() {
        System.out.printf("%s.destroy entered\n",supportName);
        dbRecord = null;
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

    public ProcessReturn process(RecordProcess recordProcess) {
        System.out.printf("%s.process entered\n",supportName);
        return ProcessReturn.noop;
    }

}
