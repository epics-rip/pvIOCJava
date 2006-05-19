/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.Field;
import org.epics.ioc.pvAccess.Structure;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for a record instance.
 * @author mrk
 *
 */
public class AbstractDBRecord extends AbstractDBStructure implements DBRecord {
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Structure structure = (Structure)this.getField();
        DBData[] dbData = super.getFieldDBDatas();
        PVData[] pvData = super.getFieldPVDatas();
        builder.append("record " + recordName + " recordType " + structure.getStructureName() + "{");
        for(int i=0, n= dbData.length; i < n; i++) {
            newLine(builder,indentLevel + 1);
            Field field = pvData[i].getField();
            builder.append(field.getName() + " = ");
            DBDField dbdField = dbData[i].getDBDField();
            switch(dbdField.getDBType()) {
            case dbPvType:
                builder.append(convert.getString(
                    dbData[i],indentLevel + 2));
                break;
            case dbMenu:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbStructure:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbArray:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbLink:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                 break;
            }
            
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * constructor that derived clases must call.
     * @param recordName the name of the record.
     * @param dbdRecordType the introspection interface for the record.
     */
    protected AbstractDBRecord(String recordName,DBDRecordType dbdRecordType)
    {
        super(dbdRecordType);
        this.recordName = recordName;
        super.setRecord(this);
        super.createFields(this);
    }

    private String recordName;
    private static Convert convert = ConvertFactory.getConvert();
}
