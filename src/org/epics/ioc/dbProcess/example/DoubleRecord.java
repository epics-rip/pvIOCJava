/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * @author mrk
 *
 */
public class DoubleRecord implements RecordSupport {
    private DBRecord dbRecord;

    public DoubleRecord(DBRecord record) {
        dbRecord = record;
        DBStructure dbStructure = (DBStructure)record;
        DBData[] dbData = dbStructure.getFieldDBDatas();
        int index;
    }
    
    public void destroy() {
        // nothing to do
    }

    public String getName() {
        return dbRecord.getRecordName();
    }

    public void initialize() {
        // must initialize link
    }

    public void start() {
        // must connect to link
    }

    public void stop() {
        // must disconnect from link
    }
    
    public ProcessReturn process(ProcessListener listener) {
System.out.printf("DoubleRecord\n");
        return ProcessReturn.done;
    }
}
