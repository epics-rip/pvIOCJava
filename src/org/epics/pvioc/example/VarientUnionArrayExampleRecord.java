/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.example;

import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.BasePVRecord;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.support.*;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessFactory;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.basic.GenericFactory;
import org.epics.pvioc.util.RequestResult;

/**
 * channelRPC support for returning the PVStructure for a PVRecord or a PVStructure from a database.
 * It accepts an NTNameValue structure and returns the PVStructure. The names supported are: "database", "record", and "structure".
 * Exactly one of "record" and "structure" must be specified.
 * @author mrk
 *
 */
public class VarientUnionArrayExampleRecord {
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
  
    public static void start(String recordName) {
        Field[] fields = new Field[1];
        String[] fieldNames = new String[1];
        fields[0] = fieldCreate.createVariantUnionArray();
        fieldNames[0] = "value";
        PVStructure pvStructure = PVDataFactory.getPVDataCreate().createPVStructure(
                fieldCreate.createStructure(fieldNames, fields));
        PVRecord pvRecord = new BasePVRecord(recordName,pvStructure);
        Support support = GenericFactory.create(pvRecord.getPVRecordStructure());
        pvRecord.getPVRecordStructure().setSupport(support);
        RecordProcess recordProcess = RecordProcessFactory.createRecordProcess(pvRecord);
        recordProcess.initialize();
        recordProcess.start(null);
        PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
        pvDatabase.addRecord(pvRecord);
    }
    
}
