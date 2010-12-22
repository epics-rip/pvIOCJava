/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.PVEnumerated;
import org.epics.pvData.property.PVEnumeratedFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Structure;


/**
 * Record Support for starting a port driver.
 * @author mrk
 *
 */
public class PortCreateFactory {
    /**
     * Create the record support for creating a port driver.
     * @param pvRecordStructure The structure for a port record.
     * @return The record support.
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return portCreate(portCreate,pvRecordStructure);
    }
    
    private static final String portCreate = "org.epics.ioc.portCreate";
    
    private static Support portCreate(String supportName,PVRecordStructure pvRecordStructure) {
    	PVStructure pvStructure = pvRecordStructure.getPVStructure();
        PVString pvString = pvStructure.getStringField("factoryName");
        if(pvString==null) return null;
        String factoryName = pvString.get();
        pvString = pvStructure.getStringField("portName");
        if(pvString==null) return null;
        String portName = pvString.get();
        PVBoolean pvBoolean = pvStructure.getBooleanField("autoConnect");
        if(pvBoolean==null) return null;
        boolean autoConnect = pvBoolean.get();
        String priority = getChoiceField(pvStructure,"priority");
        if(priority==null) return null;
        ThreadPriority scanPriority = ThreadPriority.valueOf(priority);
        PVStructure driverParameters = pvStructure.getStructureField("driverParameters");
        if(driverParameters==null) return null;
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
            parameterClasses[2] = Class.forName("org.epics.pvData.misc.ThreadPriority");
            parameterClasses[3] = Class.forName("org.epics.pvData.pv.PVStructure");
            
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
                " create IllegalAccessException "+ e.getMessage());
        } catch(IllegalArgumentException e) {
            throw new IllegalStateException(
                " create IllegalArgumentException " + e.getMessage());
        } catch(InvocationTargetException e) {
            throw new IllegalStateException(
                " create InvocationTargetException " + e.getMessage());
        }
        return new PortCreate(pvRecordStructure,supportName);
    }
    
    private static class PortCreate extends AbstractSupport {
        
        private PortCreate(PVRecordStructure pvRecordStructure,String supportName) {
            super(supportName,pvRecordStructure);
                
        }
        // nothing to do
    }
    
    private static String getChoiceField(PVStructure pvStructure,String fieldName) {
        Structure structure = pvStructure.getStructure();
        PVField[] pvFields = pvStructure.getPVFields();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            pvStructure.message("field " + fieldName + " does not exist", MessageType.error);
            return null;
        }
        PVField pvField = pvFields[index];
        PVEnumerated enumerated = PVEnumeratedFactory.create();
        if(!enumerated.attach(pvField)) {
            pvField.message(fieldName + " is not an enumerated structure", MessageType.error);
            return null;
        }
        return enumerated.getChoice();
    }
}
