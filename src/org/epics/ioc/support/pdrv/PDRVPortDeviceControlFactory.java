/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;


import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

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
        if(supportName.equals(supportName)) return new PortDeviceControl(supportName,dbStructure);
        dbStructure.getPVStructure().message("support name is not " + supportName,MessageType.fatalError);
        return null;
    }
    
    private static final String supportName = "portDeviceControl";
     
    private static class PortDeviceControl extends AbstractSupport
    implements ProcessCallbackRequester,ProcessContinueRequester
    {
        private static final String emptyMessage = "";
        private User user = Factory.createUser(null);
        private RecordProcess recordProcess = null;
        private DBRecord dbRecord = null;
        private DBField dbMessage = null;
        private PVString pvMessage = null;
        
        private PVString portDeviceNamePVString = null;
        
        private PVBoolean connectValuePVBoolean = null;
        private DBField connectValueDBField = null;
        private PVBoolean connectDesiredValuePVBoolean = null;
        private PVBoolean connectSetValuePVBoolean = null;
        private DBField connectSetValueDBField = null;
        
        private PVBoolean enableValuePVBoolean = null;
        private DBField enableValueDBField = null;
        private PVBoolean enableDesiredValuePVBoolean = null;
        private PVBoolean enableSetValuePVBoolean = null;
        private DBField enableSetValueDBField = null;
        
        private PVBoolean autoConnectValuePVBoolean = null;
        private DBField autoConnectValueDBField = null;
        private PVBoolean autoConnectDesiredValuePVBoolean = null;
        private PVBoolean autoConnectSetValuePVBoolean = null;
        private DBField autoConnectSetValueDBField = null;
        
        private PVInt traceMaskValuePVInt = null;
        private DBField traceMaskValueDBField = null;
        private PVInt traceMaskDesiredValuePVInt = null;
        private PVBoolean traceMaskSetValuePVBoolean = null;
        private DBField traceMaskSetValueDBField = null;
        

        private PVInt traceIOMaskValuePVInt = null;
        private DBField traceIOMaskValueDBField = null;
        private PVInt traceIOMaskDesiredValuePVInt = null;
        private PVBoolean traceIOMaskSetValuePVBoolean = null;
        private DBField traceIOMaskSetValueDBField = null;
        

        private PVInt traceIOTruncateSizeValuePVInt = null;
        private DBField traceIOTruncateSizeValueDBField = null;
        private PVInt traceIOTruncateSizeDesiredValuePVInt = null;
        private PVBoolean traceIOTruncateSizeSetValuePVBoolean = null;
        private DBField traceIOTruncateSizeSetValueDBField = null;
        
        private PVBoolean reportPVBoolean = null;
        private DBField reportDBField = null;
        private PVInt reportDetailsPVInt = null;
        
        private Port port = null;
        private Device device = null;
        private Trace trace = null;
        
        private SupportProcessRequester supportProcessRequester = null;
        private String message = emptyMessage;
        
        private String portDeviceNamePrevious = null;
        private String portDeviceNameDesired = null;
        
        private boolean connectDesiredValue = false;
        private boolean connectSetValue = false;
        private boolean connectNewValue = false;
        private boolean enableDesiredValue = false;
        private boolean enableSetValue = false;
        private boolean enableNewValue = false;
        private boolean autoConnectDesiredValue = false;
        private boolean autoConnectSetValue = false;
        private boolean autoConnectNewValue = false;
        private int traceMaskDesiredValue = 0;
        private boolean traceMaskSetValue = false;
        private int traceMaskNewValue = 0;
        private int traceIOMaskDesiredValue = 0;
        private boolean traceIOMaskSetValue = false;
        private int traceIOMaskNewValue = 0;
        private int traceIOTruncateSizeDesiredValue = 0;
        private boolean traceIOTruncateSizeSetValue = false;
        private int traceIOTruncateSizeNewValue = 0;
        private boolean report = false;
        private int reportDetails = 0;
        
        
        private PortDeviceControl(String supportName,DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getDBRecord();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            PVStructure pvStructure = dbRecord.getPVRecord();
            
            pvMessage = pvStructure.getStringField("message");
            if(pvMessage==null) return;
            dbMessage = dbRecord.findDBField(pvMessage);
            
            portDeviceNamePVString = pvStructure.getStringField("portDevice");
            if(portDeviceNamePVString==null) return;
            
            PVStructure connectPVStructure = pvStructure.getStructureField("connect", "booleanState");
            if(connectPVStructure==null) return;
            connectValuePVBoolean = connectPVStructure.getBooleanField("value");
            if(connectValuePVBoolean==null) return;
            connectValueDBField = dbRecord.findDBField(connectValuePVBoolean);
            connectDesiredValuePVBoolean = connectPVStructure.getBooleanField("desiredValue");
            if(connectDesiredValuePVBoolean==null) return;
            connectSetValuePVBoolean = connectPVStructure.getBooleanField("setValue");
            if(connectSetValuePVBoolean==null) return;
            connectSetValueDBField = dbRecord.findDBField(connectSetValuePVBoolean);

            
            PVStructure enablePVStructure = pvStructure.getStructureField("enable", "booleanState");
            if(enablePVStructure==null) return;
            enableValuePVBoolean = enablePVStructure.getBooleanField("value");
            if(enableValuePVBoolean==null) return;
            enableValueDBField = dbRecord.findDBField(enableValuePVBoolean);
            enableDesiredValuePVBoolean = enablePVStructure.getBooleanField("desiredValue");
            if(enableDesiredValuePVBoolean==null) return;
            enableSetValuePVBoolean = enablePVStructure.getBooleanField("setValue");
            if(enableSetValuePVBoolean==null) return;
            enableSetValueDBField = dbRecord.findDBField(enableSetValuePVBoolean);
            
            PVStructure autoConnectPVStructure = pvStructure.getStructureField("autoConnect", "booleanState");
            if(autoConnectPVStructure==null) return;
            autoConnectValuePVBoolean = autoConnectPVStructure.getBooleanField("value");
            if(autoConnectValuePVBoolean==null) return;
            autoConnectValueDBField = dbRecord.findDBField(autoConnectValuePVBoolean);
            autoConnectDesiredValuePVBoolean = autoConnectPVStructure.getBooleanField("desiredValue");
            if(autoConnectDesiredValuePVBoolean==null) return;
            autoConnectSetValuePVBoolean = autoConnectPVStructure.getBooleanField("setValue");
            if(autoConnectSetValuePVBoolean==null) return;
            autoConnectSetValueDBField = dbRecord.findDBField(autoConnectSetValuePVBoolean);
            
            PVStructure traceMaskPVStructure = pvStructure.getStructureField("traceMask", "intState");
            if(traceMaskPVStructure==null) return;
            traceMaskValuePVInt = traceMaskPVStructure.getIntField("value");
            if(traceMaskValuePVInt==null) return;
            traceMaskValueDBField = dbRecord.findDBField(traceMaskValuePVInt);
            traceMaskDesiredValuePVInt = traceMaskPVStructure.getIntField("desiredValue");
            if(traceMaskDesiredValuePVInt==null) return;
            traceMaskSetValuePVBoolean = traceMaskPVStructure.getBooleanField("setValue");
            if(traceMaskSetValuePVBoolean==null) return;
            traceMaskSetValueDBField = dbRecord.findDBField(traceMaskSetValuePVBoolean);
            
            PVStructure traceIOMaskPVStructure = pvStructure.getStructureField("traceIOMask", "intState");
            if(traceIOMaskPVStructure==null) return;
            traceIOMaskValuePVInt = traceIOMaskPVStructure.getIntField("value");
            if(traceIOMaskValuePVInt==null) return;
            traceIOMaskValueDBField = dbRecord.findDBField(traceIOMaskValuePVInt);
            traceIOMaskDesiredValuePVInt = traceIOMaskPVStructure.getIntField("desiredValue");
            if(traceIOMaskDesiredValuePVInt==null) return;
            traceIOMaskSetValuePVBoolean = traceIOMaskPVStructure.getBooleanField("setValue");
            if(traceIOMaskSetValuePVBoolean==null) return;
            traceIOMaskSetValueDBField = dbRecord.findDBField(traceIOMaskSetValuePVBoolean);
            
            PVStructure traceIOTruncateSizePVStructure = pvStructure.getStructureField("traceIOTruncateSize", "intState");
            if(traceIOTruncateSizePVStructure==null) return;
            traceIOTruncateSizeValuePVInt = traceIOTruncateSizePVStructure.getIntField("value");
            if(traceIOTruncateSizeValuePVInt==null) return;
            traceIOTruncateSizeValueDBField = dbRecord.findDBField(traceIOTruncateSizeValuePVInt);
            traceIOTruncateSizeDesiredValuePVInt = traceIOTruncateSizePVStructure.getIntField("desiredValue");
            if(traceIOTruncateSizeDesiredValuePVInt==null) return;
            traceIOTruncateSizeSetValuePVBoolean = traceIOTruncateSizePVStructure.getBooleanField("setValue");
            if(traceIOTruncateSizeSetValuePVBoolean==null) return;
            traceIOTruncateSizeSetValueDBField = dbRecord.findDBField(traceIOTruncateSizeSetValuePVBoolean);
            
            reportPVBoolean = pvStructure.getBooleanField("report");
            if(reportPVBoolean==null) return;
            reportDBField = dbRecord.findDBField(reportPVBoolean);
            reportDetailsPVInt = pvStructure.getIntField("reportDetails");
            super.initialize();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            message = emptyMessage;
            this.supportProcessRequester = supportProcessRequester;
            portDeviceNameDesired = portDeviceNamePVString.get();
            connectDesiredValue = connectDesiredValuePVBoolean.get();
            connectSetValue = connectSetValuePVBoolean.get();
            enableDesiredValue = enableDesiredValuePVBoolean.get();
            enableSetValue = enableSetValuePVBoolean.get();
            autoConnectDesiredValue = autoConnectDesiredValuePVBoolean.get();
            autoConnectSetValue = autoConnectSetValuePVBoolean.get();
            traceMaskDesiredValue = traceMaskDesiredValuePVInt.get();
            traceMaskSetValue = traceMaskSetValuePVBoolean.get();
            traceIOMaskDesiredValue = traceIOMaskDesiredValuePVInt.get();
            traceIOMaskSetValue = traceIOMaskSetValuePVBoolean.get();
            traceIOTruncateSizeDesiredValue = traceIOTruncateSizeDesiredValuePVInt.get();
            traceIOTruncateSizeSetValue = traceIOTruncateSizeSetValuePVBoolean.get();
            report = reportPVBoolean.get();
            reportDetails = reportDetailsPVInt.get();
            recordProcess.requestProcessCallback(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            connectPortDevice();
            if(port==null) {
                message = "not connected to port";
                recordProcess.processContinue(this);
                return;
            }
            connect();
            enable();
            autoConnect();
            traceMask();
            traceIOMask();
            traceIOTruncateSize();
            report();
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(message!=emptyMessage) {
                pvMessage.put(message);
                dbMessage.postPut();
            }
            if(connectSetValue) {
                connectSetValuePVBoolean.put(false);
                connectSetValueDBField.postPut();
            }
            boolean prevBoolean = connectValuePVBoolean.get();
            if(prevBoolean!=connectNewValue) {
                connectValuePVBoolean.put(connectNewValue);
                connectValueDBField.postPut();
            }
            if(enableSetValue) {
                enableSetValuePVBoolean.put(false);
                enableSetValueDBField.postPut();
            }
            prevBoolean = enableValuePVBoolean.get();
            if(prevBoolean!=enableNewValue) {
                enableValuePVBoolean.put(enableNewValue);
                enableValueDBField.postPut();
            }
            if(autoConnectSetValue) {
                autoConnectSetValuePVBoolean.put(false);
                autoConnectSetValueDBField.postPut();
            }
            prevBoolean = autoConnectValuePVBoolean.get();
            if(prevBoolean!=autoConnectNewValue) {
                autoConnectValuePVBoolean.put(autoConnectNewValue);
                autoConnectValueDBField.postPut();
            }
            if(traceMaskSetValue) {
                traceMaskSetValuePVBoolean.put(false);
                traceMaskSetValueDBField.postPut();
            }
            int prevInt = traceMaskValuePVInt.get();
            if(prevInt!=traceMaskNewValue) {
                traceMaskValuePVInt.put(traceMaskNewValue);
                traceMaskValueDBField.postPut();
            }
            if(traceIOMaskSetValue) {
                traceIOMaskSetValuePVBoolean.put(false);
                traceIOMaskSetValueDBField.postPut();
            }
            prevInt = traceIOMaskValuePVInt.get();
            if(prevInt!=traceIOMaskNewValue) {
                traceIOMaskValuePVInt.put(traceIOMaskNewValue);
                traceIOMaskValueDBField.postPut();
            }
            if(traceIOTruncateSizeSetValue) {
                traceIOTruncateSizeSetValuePVBoolean.put(false);
                traceIOTruncateSizeSetValueDBField.postPut();
            }
            prevInt = traceIOTruncateSizeValuePVInt.get();
            if(prevInt!=traceIOTruncateSizeNewValue) {
                traceIOTruncateSizeValuePVInt.put(traceIOTruncateSizeNewValue);
                traceIOTruncateSizeValueDBField.postPut();
            }
            if(report) {
                reportPVBoolean.put(false);
                reportDBField.postPut();
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
        private void connectPortDevice() {
            if(!portDeviceNameDesired.equals(portDeviceNamePrevious) ) {
                user.disconnectPort();
                portDeviceNamePrevious = portDeviceNameDesired;
                String portName = null;
                boolean portOnly = false;
                int addr = 0;
                int index = portDeviceNamePrevious.indexOf('[');
                if(index<=0) {
                    portOnly = true;
                    portName = portDeviceNamePrevious;
                } else {
                    portOnly = false;
                    portName = portDeviceNamePrevious.substring(0, index);
                    int indexEnd = portDeviceNamePrevious.indexOf(']');
                    if(index<=0) {
                        message =portDeviceNamePrevious + " is illegal value for portDevice";
                        return;
                    }
                    String addrString = portDeviceNamePrevious.substring(index+1,indexEnd);
                    addr = Integer.parseInt(addrString);
                }
                port = user.connectPort(portName);
                if(port==null) {
                    message = "could not connect to port " + portName;
                    return;
                }
                if(portOnly) {
                    device = null;
                    trace = port.getTrace();
                } else {
                    device = user.connectDevice(addr);
                    if(device==null) {
                        message = "could not connect to addr " + addr + " of port " + portName;
                        user.disconnectPort();
                        port = null;
                    }
                    trace = device.getTrace();
                }
            }
        }

        private void connect() {
            Status status = null;
            if(port==null) return;
            if(connectSetValue) {
                if(connectDesiredValue==false) {
                    if(device!=null) {
                        if(device.isConnected()) {
                            status = device.disconnect(user);
                            if(status!=Status.success) message = user.getMessage();
                        }
                    } else {
                        if(port.isConnected()) {
                            status = port.disconnect(user);
                            if(status!=Status.success) message = user.getMessage();
                        }
                    }
                } else {
                    if(device!=null) {
                        if(!device.isConnected()) {
                            status = device.connect(user);
                            if(status!=Status.success) message = user.getMessage();
                        }
                    } else {
                        if(!port.isConnected()) {
                            status = port.connect(user);
                            if(status!=Status.success) message = user.getMessage();
                        }
                    }
                }
            }
            if(device!=null) {
                connectNewValue = device.isConnected();
            } else {
                connectNewValue = port.isConnected();
            }
        }
    
        private void enable() {
            if(port==null) return;
            if(enableSetValue) {
                if(device!=null) {
                    boolean oldValue = device.isEnabled();
                    if(enableDesiredValue!=oldValue) {
                        device.enable(enableDesiredValue);
                    }
                } else {
                    boolean oldValue = port.isEnabled();
                    if(enableDesiredValue!=oldValue) {
                        port.enable(enableDesiredValue);
                    }
                }
            }
            if(device!=null) {
                enableNewValue = device.isConnected();
            } else {
                enableNewValue = port.isConnected();
            }
        }
        
        private void autoConnect() {
            if(port==null) return;
            if(autoConnectSetValue) {
                if(device!=null) {
                    boolean oldValue = device.isEnabled();
                    if(autoConnectDesiredValue!=oldValue) {
                        device.autoConnect(autoConnectDesiredValue);
                    }
                } else {
                    boolean oldValue = port.isEnabled();
                    if(autoConnectDesiredValue!=oldValue) {
                        port.autoConnect(autoConnectDesiredValue);
                    }
                }
            }
            if(device!=null) {
                autoConnectNewValue = device.isConnected();
            } else {
                autoConnectNewValue = port.isConnected();
            }
        }
        
        private void traceMask() {
            if(trace==null) return;
            if(traceMaskSetValue) {
                int oldValue = trace.getMask();
                if(traceMaskDesiredValue!=oldValue) {
                    trace.setMask(traceMaskDesiredValue);
                }
            }
            traceMaskNewValue = trace.getMask();
        }
        
        private void traceIOMask() {
            if(trace==null) return;
            if(traceIOMaskSetValue) {
                int oldValue = trace.getIOMask();
                if(traceIOMaskDesiredValue!=oldValue) {
                    trace.setIOMask(traceIOMaskDesiredValue);
                }
            }
            traceIOMaskNewValue = trace.getIOMask();
        }
        
        private void traceIOTruncateSize() {
            if(trace==null) return;
            if(traceIOTruncateSizeSetValue) {
                int oldValue = trace.getIOTruncateSize();
                if(traceIOTruncateSizeDesiredValue!=oldValue) {
                    trace.setIOTruncateSize(traceIOTruncateSizeDesiredValue);
                }
            }
            traceIOTruncateSizeNewValue = trace.getIOTruncateSize();
        }
        
        private void report() {
            if(port==null) return;
            if(!report) return;
            if(device!=null) {
                message = device.report(reportDetails);
            } else {
                message = port.report(true, reportDetails);
            }
        }
    }
}
