/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution
 */
package org.epics.pvioc.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;

/**
 * Factory that looks for and calls factories that replace the default implementation of a field.
 * @author mrk
 *
 */
public class PVReplaceFactory {
    /**
     * Look at every field of every record in the database and see if field implementation should be replaced.
     * @param pvDatabase The database.
     */
    public static void replace(PVDatabase pvDatabase) {
        for(PVRecord pvRecord: pvDatabase.getRecords()) {
            replace(pvDatabase,pvRecord,pvRecord.getPVRecordStructure());

        }
    }
    
    /**
     * Look at every field of pvStructure and see if the field implementation should be replaced.
     * @param pvDatabase The database to look for pvReplaceFactorys
     * @param pvRecordStructure The pvRecordStructure
     */
    public static void replace(PVDatabase pvDatabase,PVRecord pvRecord,PVRecordStructure pvRecordStructure) {
       replace(pvDatabase,pvRecord,pvRecordStructure.getPVRecordFields());
    }
    
    /**
     * Look at the field and see if the field implementation should be replaced.
     * If it is a structure field also look at the subfields.
     * @param pvDatabase The database to look for pvReplaceFactorys.
     * @param pvRecordField The field.
     */
    public static void replace(PVDatabase pvDatabase,PVRecord pvRecord,PVRecordField pvRecordField) {
    	PVField pvField = pvRecordField.getPVField();
        PVAuxInfo pvAuxInfo = pvField.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("pvReplaceFactory");
        while(pvScalar!=null) {
            if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
                pvField.message(pvRecordField.getFullName() + " PVReplaceFactory: pvScalar " + pvScalar.toString() + " is not a string", MessageType.error);
                break;
            }
            String factoryName = ((PVString)pvScalar).get();
            PVStructure factory = pvDatabase.findStructure(factoryName);
            if(factory==null) {
                pvField.message(pvRecordField.getFullName() + " PVReplaceFactory: factory " + factoryName + " not found", MessageType.error);
                break;
            }
            if(!replace(pvRecord,pvField,factory)) {
                pvField.message(pvRecordField.getFullName() + " PVReplaceFactory: replace failed", MessageType.error);
            }
            break;
        }
        if(pvField.getField().getType()==org.epics.pvdata.pv.Type.structure) {
            PVRecordStructure pvRecordStructure = (PVRecordStructure)pvRecordField;
            replace(pvDatabase,pvRecord,pvRecordStructure.getPVRecordFields());
        }
    }
    
    private static void replace(PVDatabase pvDatabase,PVRecord pvRecord,PVRecordField[] pvRecordFields) {
        for(PVRecordField pvRecordField : pvRecordFields) {
            replace(pvDatabase,pvRecord,pvRecordField);
        }
    }
    
    private static boolean replace(PVRecord pvRecord,PVField pvField,PVStructure factory) {
        PVString pvString = factory.getStringField("pvReplaceFactory");
        if(pvString==null) {
            pvField.message("PVReplaceFactory structure " + factory.toString() + " is not a pvReplaceFactory", MessageType.error);
            return false;
        }
        String factoryName = pvString.get();
        Class supportClass;
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
           pvField.message("PVReplaceFactory ClassNotFoundException factory " + factoryName 
            + " " + e.getLocalizedMessage(),MessageType.error);
           return false;
        }
        Class argumentClass;
        boolean isPVData;
        if(factoryName.startsWith("org.epics.pvdata")) {
        	try {
        		argumentClass = Class.forName("org.epics.pvdata.pv.PVField");
        		isPVData = true;
        	} catch (ClassNotFoundException e) {
        		pvField.message("PVReplaceFactory ClassNotFoundException factory " + factoryName 
        				+ " " + e.getLocalizedMessage(),MessageType.error);
        		return false;
        	}
        } else if(factoryName.startsWith("org.epics.pvioc")) {
        	try {
        		argumentClass = Class.forName("org.epics.pvioc.database.PVRecordField");
        		isPVData = false;
        	} catch (ClassNotFoundException e) {
        		pvField.message("PVReplaceFactory ClassNotFoundException factory " + factoryName 
        				+ " " + e.getLocalizedMessage(),MessageType.error);
        		return false;
        	}
        } else {
        	pvField.message("PVReplaceFactory unknown factoryName " + factoryName, MessageType.error);
        	return false;
        }
        try {
            method = supportClass.getDeclaredMethod("replacePVField",argumentClass);
        } catch (NoSuchMethodException e) {
            pvField.message("PVReplaceFactory NoSuchMethodException factory " + factoryName 
                    + " " + e.getLocalizedMessage(),MessageType.error);
                    return false;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            pvField.message("PVReplaceFactory factory " + factoryName 
            + " create is not a static method ",MessageType.error);
            return false;
        }
        try {
        	if(isPVData) {
            method.invoke(null,pvField);
        	} else {
        		PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
        		method.invoke(null, pvRecordField);
        	}
            return true;
        } catch(IllegalAccessException e) {
            pvField.message("PVReplaceFactory IllegalAccessException factory " + factoryName 
            + " " + e.getLocalizedMessage(),MessageType.error);
            return false;
        } catch(IllegalArgumentException e) {
            pvField.message("PVReplaceFactory IllegalArgumentException factory " + factoryName 
            + " " + e.getLocalizedMessage(),MessageType.error);
            return false;
        } catch(InvocationTargetException e) {
            pvField.message("PVReplaceFactory InvocationTargetException factory " + factoryName 
            + " " + e.getLocalizedMessage(),MessageType.error);
        }
        return false;
    }
}
