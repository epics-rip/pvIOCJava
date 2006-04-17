package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.Field;
import org.epics.ioc.pvAccess.Structure;

public class AbstractDBRecord extends AbstractDBStructure implements DBRecord {

    public String getRecordName() {
        return recordName;
    }
    
    public String toString() { return getString(0);}

    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Structure structure = (Structure)this.getField();
        builder.append("record " + recordName + " recordType " + structure.getStructureName() + "{");
        for(int i=0, n= dbData.length; i < n; i++) {
            newLine(builder,indentLevel + 1);
            Field field = pvData[i].getField();
            builder.append(field.getName() + " = ");
            DBDField dbdField = dbData[i].getDBDField();
            switch(dbdField.getDBType()) {
            case dbPvType:
                builder.append(convert.getString(
                    dbData[i],indentLevel + 1));
                break;
            case dbMenu:
                builder.append(dbData[i].toString(
                    indentLevel + 1));
                break;
            case dbStructure:
                builder.append(dbData[i].toString(
                    indentLevel + 1));
                break;
            case dbArray:
                builder.append(dbData[i].toString(
                    indentLevel + 1));
                break;
            case dbLink:
                builder.append(dbData[i].toString(
                    indentLevel + 1));
                 break;
            }
            
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    
    AbstractDBRecord(String recordName,DBDRecordType dbdRecordType)
    {
        super(dbdRecordType,dbdRecordType.getDBDFields());
        this.recordName = recordName;
        
    }

    String recordName;
}
