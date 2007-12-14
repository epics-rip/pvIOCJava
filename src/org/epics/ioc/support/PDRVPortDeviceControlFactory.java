/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;


import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Field;
import org.epics.ioc.db.*;
import org.epics.ioc.util.*;
import org.epics.ioc.pdrv.*;

/**
 * Record Support for starting a port driver.
 * @author mrk
 *
 */
public class PDRVPortDeviceControlFactory {
    /**
     * Create the record support for creating a port driver.
     * @param dbStructure The structure for a port record.
     * @return The record support.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getPVStructure().getSupportName();
        if(supportName.equals(portDeviceControl)) return portDeviceControl(portDeviceControl,dbStructure);
        return null;
    }
    
    private static final String portDeviceControl = "portDeviceControl";
    
    private static Support portDeviceControl(String supportName,DBStructure dbStructure) {
        PortDeviceControl support = new PortDeviceControl(supportName,dbStructure);
        DBField[] dbFields = dbStructure.getDBFields();
        DBField dbField = null;
        PVStructure pvStructure = dbStructure.getPVStructure();
        PVField[] pvFields = pvStructure.getPVFields();
        PVField pvField = null;
        Field field = null;
        org.epics.ioc.pv.Type type = null;
        Structure structure = (Structure)pvStructure.getField();
        int index = structure.getFieldIndex("message");
        if(index<0) {
            pvStructure.message("structure does not have field message",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(type!=org.epics.ioc.pv.Type.pvString) {
            pvStructure.message("field message is not a string",MessageType.fatalError);
            return null;
        }
        support.dbMessage = dbField;
        support.pvMessage = (PVString)pvField;
        
        index = structure.getFieldIndex("portDevice");
        if(index<0) {
            pvStructure.message("structure does not have field portDevice",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(type!=org.epics.ioc.pv.Type.pvString) {
            pvStructure.message("field portDevice is not a string",MessageType.fatalError);
            return null;
        }
        PVString pvString = (PVString)pvField;
        String portDevice = pvString.get();        
        PVString pvPortDevice = new PortDeviceData(pvStructure,field,support,dbField);        
        dbField.replacePVField(pvPortDevice);
        if(portDevice!=null) pvPortDevice.put(portDevice);
        index = structure.getFieldIndex("connect");
        if(index<0) {
            pvStructure.message("structure does not have field connect",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(type!=org.epics.ioc.pv.Type.pvBoolean) {
            pvStructure.message("field connect is not a boolean",MessageType.fatalError);
            return null;
        }
        PVBoolean pvConnectDisconnectData = new ConnectDisconnectData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvConnectDisconnectData);       
        index = structure.getFieldIndex("enable");
        if(index<0) {
            pvStructure.message("structure does not have field enable",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(type!=org.epics.ioc.pv.Type.pvBoolean) {
            pvStructure.message("field enable is not a boolean",MessageType.fatalError);
            return null;
        }        
        PVBoolean pvEnableDisableData = new EnableDisableData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvEnableDisableData);       
        index = structure.getFieldIndex("autoConnect");
        if(index<0) {
            pvStructure.message("structure does not have field autoConnect",MessageType.fatalError);
            return null;
        }        
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(type!=org.epics.ioc.pv.Type.pvBoolean) {
            pvStructure.message("field autoConnect is not boolean",MessageType.fatalError);
            return null;
        }
        PVBoolean pvBoolean = (PVBoolean)pvField;
        boolean autoConnect = pvBoolean.get();
        PVBoolean pvAutoConnectData = new AutoConnectData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvAutoConnectData);
        pvAutoConnectData.put(autoConnect);
        
        index = structure.getFieldIndex("traceMask");
        if(index<0) {
            pvStructure.message("structure does not have field traceMask",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        pvField = pvFields[index];
        if(type!=org.epics.ioc.pv.Type.pvInt) {
            pvStructure.message("field traceMask is not an int",MessageType.fatalError);
            return null;
        }
        PVInt pvInt = (PVInt)pvField;
        int traceMask = pvInt.get();
        PVInt pvTraceMaskData = new TraceMaskData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvTraceMaskData);
        pvTraceMaskData.put(traceMask);
        
        index = structure.getFieldIndex("traceIOMask");
        if(index<0) {
            pvStructure.message("structure does not have field traceIOMask",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(pvField.getField().getType()!=org.epics.ioc.pv.Type.pvInt) {
            pvStructure.message("field traceIOMask is not an int",MessageType.fatalError);
            return null;
        }
        pvInt = (PVInt)pvField;
        int traceIOMask = pvInt.get();
        PVInt pvTraceIOMaskData = new TraceIOMaskData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvTraceIOMaskData);
        pvTraceIOMaskData.put(traceIOMask);
        
        index = structure.getFieldIndex("report");
        if(index<0) {
            pvStructure.message("structure does not have field report",MessageType.fatalError);
            return null;
        }
        dbField = dbFields[index];
        pvField = pvFields[index];
        field = pvField.getField();
        type = field.getType();
        if(pvField.getField().getType()!=org.epics.ioc.pv.Type.pvInt) {
            pvStructure.message("field report is not an int",MessageType.fatalError);
            return null;
        }
        PVInt pvReportData = new ReportData(pvStructure,field,support,dbField);
        dbField.replacePVField(pvReportData);
        return support;
    }
    private static class PortDeviceControl extends AbstractSupport {
        User user = Factory.createUser(null);
        DBField dbMessage = null;
        PVString pvMessage = null;
        
        Port port = null;
        Device device = null;
        
        PortDeviceControl(String supportName,DBStructure dbStructure) {
            super(supportName,dbStructure);
                
        }
        
        void message(String message) {
            pvMessage.put(message);
            dbMessage.postPut();
        }
        
        boolean connectPortDevice(boolean portOnly,String portName,int addr) {           
            user.disconnectPort();
            port = user.connectPort(portName);
            if(port==null) {
                message("could not connect to port " + portName);
                return false;
            }
            device = user.connectDevice(addr);
            if(device==null) {
                message("could not connect to addr " + addr + " of port " + portName);
                return false;
            }
            return true;            
        }
        
        boolean connect(boolean value) {
            Status status = null;
            if(port==null) return false;
            user.lockPort();
            try {
                if(value==false) {
                    if(device!=null) {
                        if(!device.isConnected()) return true;
                        status = device.disconnect(user);
                        if(status!=Status.success) {
                            message(user.getMessage());
                            return false;
                        }
                        return true;
                    }
                    if(!port.isConnected()) return true;
                    status = port.disconnect(user);
                    if(status!=Status.success) {
                        message(user.getMessage());
                        return false;
                    }
                    return true;
                }
                if(!port.isConnected()) {
                    status = port.connect(user);
                    if(status!=Status.success) {
                        message(user.getMessage());
                        return false;
                    }
                }
                if(device==null || device.isConnected()) return true;
                status = device.connect(user);
                if(status!=Status.success) {
                    message(user.getMessage());
                    return false;
                }
                return true;
            } finally {
                user.unlockPort();
            }
        }
        
        void enable(boolean value) {
            if(port==null) return;
            if(device!=null) {
                device.enable(value);
            } else {
                port.enable(value);
            }
        }
        
        void autoConnect(boolean value) {
            if(port==null) return;
            if(device!=null) {
                device.autoConnect(value);
            } else {
                port.autoConnect(value);
            }
        }
        
        void traceMask(int value) {
            if(port==null) return;
            Trace trace = null;
            if(device!=null) {
                trace = device.getTrace();
            } else {
                trace = port.getTrace();
            }
            trace.setMask(value);
        }
        
        void traceIOMask(int value) {
            if(port==null) return;
            Trace trace = null;
            if(device!=null) {
                trace = device.getTrace();
            } else {
                trace = port.getTrace();
            }
            trace.setIOMask(value);
        }
        
        void report(int details) {
            if(port==null) {
                message("not connected to port");
                return;
            }
            String report = null;
            if(device!=null) {
                report = device.report(details);
            } else {
                report = port.report(true, details);
            }
            message(report);
        }
    }
    
    private static class PortDeviceData extends AbstractPVField implements PVString {
        private PortDeviceControl portDeviceControl = null;
        private String portDevice = null;
        private DBField dbField;
        
        private PortDeviceData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            return portDevice;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            String portName = null;
            boolean portOnly = false;
            int addr = 0;
            int index = value.indexOf('[');
            if(index<=0) {
                portOnly = true;
                portName = value;
            } else {
                portOnly = false;
                portName = value.substring(0, index);
                int indexEnd = value.indexOf(']');
                if(index<=0) {
                    portDeviceControl.message(value + " is illegal value for portDevice");
                    return;
                }
                String addrString = value.substring(index+1,indexEnd);
                addr = Integer.parseInt(addrString);
            }
            boolean result = portDeviceControl.connectPortDevice(portOnly, portName, addr);
            if(result) {
                portDevice = value;
                dbField.postPut();
            }
        }
    }
    
    private static class ConnectDisconnectData extends AbstractPVField implements PVBoolean {
        private PortDeviceControl portDeviceControl;
        private DBField dbField;
        private boolean value;
        
        private ConnectDisconnectData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            boolean result =portDeviceControl.connect(value);
            if(result) {
                this.value = value;
                dbField.postPut();
            }
        }
    }
    
    private static class EnableDisableData extends AbstractPVField implements PVBoolean {
        private PortDeviceControl portDeviceControl = null;
        private DBField dbField;
        private boolean value = false;
        
        private EnableDisableData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            portDeviceControl.enable(value);
            this.value = value;
            dbField.postPut();
        }
    }
    
    private static class AutoConnectData extends AbstractPVField implements PVBoolean {
        private PortDeviceControl portDeviceControl = null;
        private DBField dbField;
        private boolean autoConnect = true;
        
        private AutoConnectData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return autoConnect;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            portDeviceControl.autoConnect(value);
            this.autoConnect = value;
            dbField.postPut();
        }
    }
    
    private static class TraceMaskData extends AbstractPVField implements PVInt {
        private PortDeviceControl portDeviceControl = null;
        private DBField dbField;
        int mask = 1;
        
        private TraceMaskData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            return mask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            portDeviceControl.traceMask(value);
            this.mask = value;
            dbField.postPut();
        }
    }
    
    private static class TraceIOMaskData extends AbstractPVField implements PVInt {
        private PortDeviceControl portDeviceControl = null;
        private DBField dbField;
        private int mask;
        
        private TraceIOMaskData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            return mask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            portDeviceControl.traceIOMask(value);
            this.mask = value;
            dbField.postPut();
        }
    }
    
    private static class ReportData extends AbstractPVField implements PVInt {
        private PortDeviceControl portDeviceControl = null;
        private DBField dbField;
        private int value;
        
        private ReportData(PVField parent,Field field,
            PortDeviceControl portDeviceControl,DBField dbField)
        {
            super(parent,field);
            this.portDeviceControl = portDeviceControl;
            this.dbField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            portDeviceControl.report(value);
            this.value = value;
            dbField.postPut();
        }
    }
}
