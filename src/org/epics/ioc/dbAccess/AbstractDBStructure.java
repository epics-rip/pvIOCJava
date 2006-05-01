/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public abstract class AbstractDBStructure extends AbstractDBData
    implements DBStructure
{

    public int getFieldDBDataIndex(String fieldName) {
        return dbdStructure.getDBDFieldIndex(fieldName);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVStructure#getFieldPVDatas()
     */
    public PVData[] getFieldPVDatas() {
        return pvData;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBStructure#getFieldDBDatas()
     */
    public DBData[] getFieldDBDatas() {
        return dbData;
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
        builder.append("structure " + structure.getStructureName() + "{");
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
     * constructor that derived classes must call.
     * @param dbdStructureField the reflection interface for the DBStructure data.
     */
    AbstractDBStructure(DBDStructureField dbdStructureField) {
        super(dbdStructureField);
        dbdStructure = dbdField.getAttribute().getStructure();
        DBDField[] dbdFields = dbdStructure.getDBDFields();
        dbData = new DBData[dbdFields.length];
        pvData = new PVData[dbData.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(dbdFields[i]);
            pvData[i] = dbData[i];
        }
        
    }
    
    /**
     * constructor for record instance classes.
     * @param dbdField the reflection interface for the record type.
     * @param dbdFields array of reflection interfaces for the fields of the record type.
     */
    AbstractDBStructure(DBDField dbdField,DBDField[] dbdFields) {
        super(dbdField);
        dbdStructure = (DBDStructure)dbdField;
        dbData = new DBData[dbdFields.length];
        pvData = new PVData[dbData.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(dbdFields[i]);
            pvData[i] = dbData[i];
        }
        
    }
    
    /**
     * reflection interface.
     */
    protected DBDStructure dbdStructure;
    /**
     * array of pvData interfaces for the fields.
     */
    protected PVData[] pvData;
    /**
     * array of DBData interfaces for the fields.
     */
    protected DBData[] dbData;
    /**
     * reference to the implementation of Convert.
     */
    protected static Convert convert = ConvertFactory.getConvert();
    /**
     * for use in implementing toString.
     * @param builder the StringBuilder.
     * @param indentLevel indention level.
     */
    protected static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    private static String indentString = "    ";
    
}
