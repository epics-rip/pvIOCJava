/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;


import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;


/**
 * Record Support for starting a port driver.
 * @author mrk
 *
 */
public class PortDeviceControlFactory {
    /**
     * Create the record support for creating a port driver.
     * @param pvStructure The structure for a port record.
     * @return The record support.
     */
    public static Support create(PVStructure pvStructure) {
        return new PortDeviceControl(supportName,pvStructure);
    }
    
    private static final String supportName = "portDeviceControl";
     
    private static class PortDeviceControl extends AbstractSupport
    implements ProcessCallbackRequester,ProcessContinueRequester
    {
        private static final String emptyMessage = "";
        private User user = Factory.createUser(null);
        private RecordProcess recordProcess = null;
        private PVStructure pvStructure = null;
        private PVString pvMessage = null;
        
        private PVBoolean pvProcessAtStart = null;
        private PVString pvPortName = null;
        private PVString pvDeviceName = null;
        private String portName = null;
        private String deviceName = null;
        
        private PVBoolean pvConnect = null;
        private boolean connect = false;
        
        private PVBoolean pvEnable = null;
        private boolean enable = false;

        private PVBoolean pvAutoConnect = null;
        private boolean autoConnect = false;
        
        private PVInt pvTraceMask = null;
        private int traceMask = 0;
        
        private PVInt pvTraceIOMask = null;
        private int traceIOMask = 0;
        
        private PVInt pvTraceIOTruncateSize = null;
        private int traceIOTruncateSize = 0;
 
        private PVBoolean pvReport = null;
        private boolean report = false;
        private PVInt pvReportDetails = null;
        private int reportDetails = 0;
        
        private boolean processAtStart = false;
        private Port port = null;
        private Device device = null;
        private Trace trace = null;
        
        private SupportProcessRequester supportProcessRequester = null;
        private boolean isConnected = true;
        private boolean justConnected = false;
        private String message = emptyMessage;
        
        private PortDeviceControl(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
            this.pvStructure = pvStructure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcess = recordSupport.getRecordProcess();
            pvProcessAtStart = pvStructure.getBooleanField("processAtStart");
            if(pvProcessAtStart==null) return;
            pvMessage = pvStructure.getStringField("message");
            if(pvMessage==null) return;
            pvPortName = pvStructure.getStringField("portName");
            if(pvPortName==null) return;
            pvDeviceName = pvStructure.getStringField("deviceName");
            if(pvDeviceName==null) return;
            pvConnect = pvStructure.getBooleanField("connect");
            if(pvConnect==null) return;
            pvEnable = pvStructure.getBooleanField("enable");
            if(pvEnable==null) return;
            pvAutoConnect = pvStructure.getBooleanField("autoConnect");
            if(pvAutoConnect==null) return;
            pvTraceMask = pvStructure.getIntField("traceMask");
            if(pvTraceMask==null) return;
            pvTraceIOMask = pvStructure.getIntField("traceIOMask");
            if(pvTraceIOMask==null) return;
            pvTraceIOTruncateSize = pvStructure.getIntField("traceIOTruncateSize");
            if(pvTraceIOTruncateSize==null) return;
            pvReport = pvStructure.getBooleanField("report");
            pvReportDetails = pvStructure.getIntField("reportDetails");
            super.initialize(recordSupport);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#start()
         */
        @Override
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            processAtStart = pvProcessAtStart.get();
            if(!processAtStart) return;
            getPVs();
            process();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#stop()
         */
        @Override
        public void stop() {
            if(port!=null) {
                port.disconnect(user);
                port = null;
            }
            super.stop();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            this.supportProcessRequester = supportProcessRequester;
            getPVs();
            recordProcess.requestProcessCallback(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            process();
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(message!=emptyMessage) {
                pvMessage.put(message);
                pvMessage.postPut();
                message = emptyMessage;
            }
            if(connect!=pvConnect.get()) {
                pvConnect.put(connect);
                pvConnect.postPut();
            }
            if(enable!=pvEnable.get()) {
                pvEnable.put(enable);
                pvEnable.postPut();
            }
            if(autoConnect!=pvAutoConnect.get()) {
                pvAutoConnect.put(autoConnect);
                pvAutoConnect.postPut();
            }
            if(traceMask!=pvTraceMask.get()) {
                pvTraceMask.put(traceMask);
                pvTraceMask.postPut();
            }
            if(traceIOMask!=pvTraceIOMask.get()) {
                pvTraceIOMask.put(traceIOMask);
                pvTraceIOMask.postPut();
            }
            if(traceIOTruncateSize!=pvTraceIOTruncateSize.get()) {
                pvTraceIOTruncateSize.put(traceIOTruncateSize);
                pvTraceIOTruncateSize.postPut();
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
        private void getPVs() {
            message = emptyMessage;
            portName = pvPortName.get();
            deviceName = pvDeviceName.get();
            connect = pvConnect.get();
            enable = pvEnable.get();
            autoConnect = pvAutoConnect.get();
            traceMask = pvTraceMask.get();
            traceIOMask = pvTraceIOMask.get();
            traceIOTruncateSize = pvTraceIOTruncateSize.get();
            report = pvReport.get();
            reportDetails = pvReportDetails.get();
        }
        
        private void process() {
            connectPortDevice();
            if(isConnected) {
                // order is important
                autoConnect();
                traceMask();
                traceIOMask();
                traceIOTruncateSize();
                enable();
                connect();
                report();
            }
        }
        
        private void connectPortDevice() {
            justConnected = false;
            isConnected = false;
            boolean portOnly = false;
            if(deviceName==null || deviceName.length()<=0) {
                portOnly = true;
            }
            if(port!=null) {
                if(portName.equals(port.getPortName())) {
                    if(device!=null) {
                        if(deviceName.equals(device.getDeviceName())){
                            isConnected = true;
                            return;
                        }
                    } else if(portOnly) {
                        isConnected = true;
                        return;
                    }
                }
                user.disconnectPort();
                port = null;
            }
            port = user.connectPort(portName);
            if(port==null) {
                message = "could not connect to port " + portName;
                port = null;
                return;
            }
            if(portOnly) {
                device = null;
                trace = port.getTrace();
            } else {
                device = user.connectDevice(deviceName);
                if(device==null) {
                    message = "could not connect to " + portName +"[" + deviceName + "]";
                    user.disconnectPort();
                    port = null;
                    return;
                }
                trace = device.getTrace();
            }
            isConnected = true;
            if(processAtStart) {
                // act like not justConnected for processAtStart.
                processAtStart = false;
                return;
            }
            justConnected = true;
        }

        private void connect() {
            Status status = null;
            if(device!=null) {
                if(justConnected) {
                    connect = device.isConnected(); return;
                }
                if(connect==device.isConnected()) return;
                if(connect) {
                    status = user.lockPortForConnect();
                    if(status==Status.success) {
                        try {
                            status = device.connect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                } else {
                    status = user.lockPort();
                    if(status==Status.success) {
                        try {
                            status = device.disconnect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                }
                if(status!=Status.success) message = user.getMessage();
                connect = device.isConnected();
            } else {
                if(justConnected) {
                    connect = port.isConnected(); return;
                }
                if(connect==port.isConnected()) return;
                if(connect) {
                    status = user.lockPortForConnect();
                    if(status==Status.success) {
                        try {
                            status = port.connect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                } else {
                    status = user.lockPort();
                    if(status==Status.success) {
                        try {
                            status = port.disconnect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                }
                if(status!=Status.success) message = user.getMessage();
                connect = port.isConnected();
            }       
        }
    
        private void enable() {
            if(device!=null) {
                if(justConnected) {
                    enable = device.isEnabled(); return;
                }
                if(enable==device.isEnabled()) return;
                device.enable(enable);
            } else {
                if(justConnected) {
                    enable = port.isEnabled(); return;
                }
                if(enable==port.isEnabled()) return;
                port.enable(enable);
            }       
        }
        
        private void autoConnect() {
            if(device!=null) {
                if(justConnected) {
                    autoConnect = device.isAutoConnect(); return;
                }
                if(autoConnect==device.isAutoConnect()) return;
                device.autoConnect(autoConnect);
            } else {
                if(justConnected) {
                    autoConnect = port.isAutoConnect(); return;
                }
                if(autoConnect==port.isAutoConnect()) return;
                port.autoConnect(autoConnect);
            }       
        }
        
        private void traceMask() {
            if(justConnected) {
                traceMask = trace.getMask();
            }
            if(traceMask==trace.getMask()) return;
            trace.setMask(traceMask);
        }
        
        private void traceIOMask() {
            if(justConnected) {
                traceIOMask = trace.getIOMask();
            }
            if(traceIOMask==trace.getIOMask()) return;
            trace.setIOMask(traceIOMask);
        }
        
        private void traceIOTruncateSize() {
            if(justConnected) {
                traceIOTruncateSize = trace.getIOTruncateSize();
            }
            if(traceIOTruncateSize==trace.getIOTruncateSize()) return;
            trace.setIOTruncateSize(traceIOTruncateSize);
        }
        
        private void report() {
            if(!report) return;
            if(message!=emptyMessage) return;
            if(device!=null) {
                message = device.report(reportDetails);
            } else {
                message = port.report(true, reportDetails);
            }
        }
    }
}
