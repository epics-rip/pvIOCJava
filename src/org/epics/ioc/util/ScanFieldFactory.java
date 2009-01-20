/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;



/**
 * 
 * A factory to create a ScanField interface.
 * @author mrk
 *
 */
public class ScanFieldFactory {
    /**
     * Create a ScanField.
     * This is called by RecordProcessFactory.
     * If the record instance does not have a field named scan then null is returned.
     * If it does the field must be a scan structure.
     * ScanFieldFactory does no locking so code that uses it must be thread safe.
     * In general this means that the record instance must be locked when any method is called. 
     * @param pvRecord The record instance.
     * @return The ScanField interface or null of the record instance does not have
     * a valid pvType field.
     */
    public static ScanField create(PVRecord pvRecord) {
        PVStructure pvScan = pvRecord.getStructureField("scan");
        if(pvScan==null) {
            pvRecord.message("scan not found or is not a structure", MessageType.fatalError);
            return null;
        }
        PVStructure priority = pvScan.getStructureField("priority");
        if(priority==null) {
            pvScan.message("priority not found or is not a structure", MessageType.fatalError);
            return null;
        }
        Enumerated enumerated = EnumeratedFactory.getEnumerated(priority);
        if(enumerated==null) {
            priority.message("priority is not enumerated", MessageType.fatalError);
            return null;
        }
        PVInt pvPriority = enumerated.getIndex();
        
        PVStructure type = pvScan.getStructureField("type");
        if(priority==null) {
            pvScan.message("type not found or is not a structure", MessageType.fatalError);
            return null;
        }
        enumerated = EnumeratedFactory.getEnumerated(type);
        if(enumerated==null) {
            type.message("type is not enumerated", MessageType.fatalError);
            return null;
        }
        PVInt pvType = enumerated.getIndex();
        
        PVDouble pvRate = pvScan.getDoubleField("rate");
        if(pvRate==null) {
            pvScan.message("rate field not found or is not a double", MessageType.fatalError);
        }
        
        PVString pvEventName = pvScan.getStringField("eventName");
        if(pvRate==null) {
            pvScan.message("eventName not found or is not a string", MessageType.fatalError);
        }
        
        PVBoolean pvProcessSelf = pvScan.getBooleanField("processSelf");
        if(pvProcessSelf==null) {
            pvScan.message("processSelf not found or is not a boolean", MessageType.fatalError);
        }
        PVBoolean pvProcessAfterStart = pvScan.getBooleanField("processAfterStart");
        if(pvProcessAfterStart==null) {
            pvScan.message("processAfterStart not found or is not a boolean", MessageType.fatalError);
        }
        return new ScanFieldInstance(pvScan,pvPriority,pvType,pvRate,pvEventName,pvProcessSelf,pvProcessAfterStart);
    }
    
    
    private static class ScanFieldInstance implements ScanField{
        private PVInt pvPriority;
        private PVInt pvType;
        private PVDouble pvRate;
        private PVString pvEventName;
        private PVBoolean pvProcessSelf;
        private PVBoolean pvProcessAfterStart;
        
        private ScanFieldInstance(PVField scanField,PVInt pvPriority, PVInt pvType,
            PVDouble pvRate, PVString pvEventName, PVBoolean pvProcessSelfField, PVBoolean pvProcessAfterStart)
        {
            super();
            this.pvPriority = pvPriority;
            this.pvType = pvType;
            this.pvRate = pvRate;
            this.pvEventName = pvEventName;
            this.pvProcessSelf = pvProcessSelfField;
            this.pvProcessAfterStart = pvProcessAfterStart;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getEventName()
         */
        public String getEventName() {
            return pvEventName.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getPriority()
         */
        public ThreadPriority getPriority() {
            return ThreadPriority.values()[pvPriority.get()];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getRate()
         */
        public double getRate() {
            return pvRate.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getScanType()
         */
        public ScanType getScanType() {
            return ScanType.values()[pvType.get()];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessSelf()
         */
        public boolean getProcessSelf() {
            return pvProcessSelf.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getEventNamePV()
         */
        public PVString getEventNamePV() {
            return pvEventName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getPriorityPV()
         */
        public PVInt getPriorityIndexPV() {
            return pvPriority;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessSelfPV()
         */
        public PVBoolean getProcessSelfPV() {
            return pvProcessSelf;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getRatePV()
         */
        public PVDouble getRatePV() {
            return pvRate;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getScanTypePV()
         */
        public PVInt getScanTypeIndexPV() {
            return pvType;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessAfterStart()
         */
        public boolean getProcessAfterStart() {
            return pvProcessAfterStart.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ScanField#getProcessAfterStartPV()
         */
        public PVBoolean getProcessAfterStartPV() {
            return pvProcessAfterStart;
        }        
    }
}
