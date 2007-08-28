/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.lang.reflect.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Type;
import org.epics.ioc.db.*;
import org.epics.ioc.util.*;

/**
 * Record Support for starting a port driver.
 * @author mrk
 *
 */
public class PDRVPortCreateFactory {
    /**
     * Create the record support for creating a port driver.
     * @param dbStructure The structure for a port record.
     * @return The record support.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getPVStructure().getSupportName();
        if(supportName.equals(portCreate)) return portCreate(portCreate,dbStructure);
        return null;
    }
    
    private static final String portCreate = "portCreate";
    
    private static Support portCreate(String supportName,DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        PVField[] pvFields = pvStructure.getFieldPVFields();
        Structure structure = (Structure)pvStructure.getField();
        int index = structure.getFieldIndex("factoryName");
        if(index<0) {
            throw new IllegalStateException("structure does not have field factoryName");
        }
        PVField pvField = pvFields[index];
        org.epics.ioc.pv.Field field = pvField.getField();
        if(field.getType()!=org.epics.ioc.pv.Type.pvString) {
            throw new IllegalStateException("field factoryName is not type string");
        }
        PVString pvString = (PVString)pvField;
        String factoryName = pvString.get();
        index = structure.getFieldIndex("portName");
        if(index<0) {
            throw new IllegalStateException("structure does not have field portName");
        }
        pvField = pvFields[index];
        field = pvField.getField();
        if(field.getType()!=org.epics.ioc.pv.Type.pvString) {
            throw new IllegalStateException("field portName is not type string");
        }
        pvString= (PVString)pvField;
        String portName = pvString.get();
        index = structure.getFieldIndex("autoConnect");
        if(index<0) {
            throw new IllegalStateException("structure does not have field autoConnect");
        }
        pvField = pvFields[index];
        field = pvField.getField();
        if(field.getType()!=org.epics.ioc.pv.Type.pvBoolean) {
            throw new IllegalStateException("field portName is not type boolean");
        }
        PVBoolean pvBoolean= (PVBoolean)pvField;
        boolean autoConnect = pvBoolean.get();
        PVString pvPriority = getChoiceField(pvStructure,"priority");
        if(pvPriority==null) return null;
        ScanPriority scanPriority = ScanPriority.valueOf(pvPriority.get());
        index = structure.getFieldIndex("driverParameters");
        if(index<0) {
            throw new IllegalStateException("structure does not have field driverParameters");
        }
        pvField = pvFields[index];
        field = pvField.getField();
        if(field.getType()!=org.epics.ioc.pv.Type.pvStructure) {
            throw new IllegalStateException("field portName is not type structure");
        }
        PVStructure driverParameters= (PVStructure)pvField;
        Object[] parameters = new Object[4];
        parameters[0] = portName;
        parameters[1] = autoConnect;
        parameters[2] = scanPriority;
        parameters[3] = driverParameters;
        Class supportClass;
        Class[] parameterClasses = new Class[4];
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
            parameterClasses[0] = Class.forName("java.lang.String");
            parameterClasses[1] = boolean.class;
            parameterClasses[2] = Class.forName("org.epics.ioc.util.ScanPriority");
            parameterClasses[3] = Class.forName("org.epics.ioc.pv.PVStructure");
            
        }catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                " factory " + e.getLocalizedMessage() + " class not found");
        }
        try {
            method = supportClass.getDeclaredMethod("create",parameterClasses);
        } catch (NoSuchMethodException e) {
            pvStructure.message(" no factory method "
                    + e.getLocalizedMessage(), MessageType.error);
            return null;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(
                factoryName + " create is not a static method ");
        }
        try {
            method.invoke(null,parameters);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException(
                " create IllegalAccessException "+ e.getLocalizedMessage());
        } catch(IllegalArgumentException e) {
            throw new IllegalStateException(
                " create IllegalArgumentException " + e.getLocalizedMessage());
        } catch(InvocationTargetException e) {
            throw new IllegalStateException(
                " create InvocationTargetException " + e.getLocalizedMessage());
        }
        return new PortCreate(dbStructure,supportName);
    }
    
    private static class PortCreate extends AbstractSupport {
        
        private PortCreate(DBStructure dbStructure,String supportName) {
            super(supportName,dbStructure);
                
        }
        // nothing to do
    }
    
    private static PVString getChoiceField(PVStructure pvStructure,String fieldName) {
        Structure structure = pvStructure.getStructure();
        PVField[] pvFields = pvStructure.getFieldPVFields();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            pvStructure.message("field " + fieldName + " does not exist", MessageType.error);
            return null;
        }
        PVField pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not a structure", MessageType.error);
            return null;
        }
        pvStructure = (PVStructure)pvField;
        pvFields = pvStructure.getFieldPVFields();
        structure = pvStructure.getStructure();
        index = structure.getFieldIndex("choice");
        if(index<0) {
            pvStructure.message("field index does not exist", MessageType.error);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvString) {
            pvField.message("field is not a string", MessageType.error);
            return null;
        }
        return (PVString)pvField;
    }
}
