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
public class CounterRecord implements RecordSupport {
    private DBRecord dbRecord;
    private DBDouble dbMin = null;
    private DBDouble dbMax = null;
    private DBDouble dbInc = null;
    private DBDouble dbValue = null;
    
    public CounterRecord(DBRecord record) {
        dbRecord = record;
        DBStructure dbStructure = (DBStructure)record;
        DBData[] dbData = dbStructure.getFieldDBDatas();
        int index;
        index = dbStructure.getFieldDBDataIndex("min");
        if(index<0) throw new IllegalStateException("field min does not exist");
        dbMin = (DBDouble)dbData[index];
        index = dbStructure.getFieldDBDataIndex("max");
        if(index<0) throw new IllegalStateException("field max does not exist");
        dbMax = (DBDouble)dbData[index];
        index = dbStructure.getFieldDBDataIndex("inc");
        if(index<0) throw new IllegalStateException("field inc does not exist");
        dbInc = (DBDouble)dbData[index];
        index = dbStructure.getFieldDBDataIndex("value");
        if(index<0) throw new IllegalStateException("field value does not exist");
        dbValue = (DBDouble)dbData[index];
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
    
    public void linkSupportDone(LinkReturn result) {
        // TODO Auto-generated method stub
        
    }

    public ProcessReturn process(RecordProcess recordProcess) {
        double min = dbMin.get();
        double max = dbMax.get();
        double inc = dbInc.get();
        double value = dbValue.get();
        value += inc;
        if(value>max) value = min;
        dbValue.put(value);
//System.out.printf("CounterRecord new value %f\n",value);
        return ProcessReturn.done;
    }
}
