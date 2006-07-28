/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.support.*;

/**
 * @author mrk
 *
 */
public class CounterRecord implements RecordSupport, ProcessListener {
    private DBRecord dbRecord;
    private DBDouble dbMin = null;
    private DBDouble dbMax = null;
    private DBDouble dbInc = null;
    private DBDouble dbValue = null;
    private DBArray dbProcess = null;
    
    private RecordSupport processLinkArray = null;
    private ProcessListener listener = null;
    
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
        index = dbStructure.getFieldDBDataIndex("process");
        if(index<0) throw new IllegalStateException("field process does not exist");
        dbProcess = (DBArray)dbData[index];
    }
    public void destroy() {
        processLinkArray.destroy();
        processLinkArray = null;
    }

    public String getName() {
        return dbRecord.getRecordName();
    }

    public void initialize() {
        processLinkArray = ProcessLinkArray.createSupport(dbProcess);
        processLinkArray.initialize();
    }

    public void start() {
        processLinkArray.start();
    }

    public void stop() {
        processLinkArray.stop();
    }
    

    public ProcessReturn process(ProcessListener listener) {
        double min = dbMin.get();
        double max = dbMax.get();
        double inc = dbInc.get();
        double value = dbValue.get();
        value += inc;
        if(value>max) value = min;
        dbValue.put(value);
System.out.printf("CounterRecord new value %f\n",value);
        this.listener = listener;
        ProcessReturn processReturn = processLinkArray.process(this);
        return processReturn;
    }
    public void processComplete(ProcessReturn result) {
        if(result==ProcessReturn.active) return;
        if(listener!=null) listener.processComplete(result);
    }
}
