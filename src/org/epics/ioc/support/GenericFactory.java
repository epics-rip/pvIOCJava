/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.util.RequestResult;

/**
 * Support for a record type that has an arbitrary set of fields.
 * Fields scan, alarm, and timeStamp are ignored.
 * The support for all other fields that have support are called in the order the fields
 * appear in the record. For process each support must complete before the support for the next
 * field is called.
 * @author mrk
 *
 */
public class GenericFactory {
    /**
     * Create the support for the record or structure.
     * @param dbStructure The struvture or record for which to create support.
     * @return The support instance.
     */
    public static Support create(DBStructure dbStructure) {
        return new GenericImpl(dbStructure);
    }
    
    
    static private class GenericImpl extends AbstractSupport
    implements SupportProcessRequester
    {
        private static String supportName = "generic";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        
        private AlarmSupport alarmSupport = null;
        private int numberSupports = 0;
        private Support[] supports = null;
        private int nextSupport = 0;
        
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult finalResult = RequestResult.success;
        
        
        private GenericImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvStructure.getField();
            DBField[] dbFields = dbStructure.getDBFields();
            Field[] fields = structure.getFields();
            numberSupports = 0;
            for(int i=0; i<dbFields.length; i++) {
                String fieldName = fields[i].getFieldName();
                if(fieldName.equals("scan")) continue;
                if(fieldName.equals("timeStamp")) continue;                   
                if(fieldName.equals("alarm")) {
                    Support support = dbFields[i].getSupport();
                    if(support!=null && support instanceof AlarmSupport) {
                        alarmSupport = (AlarmSupport)support;
                    }
                    continue;
                }
                if(dbFields[i].getSupport()==null) continue;
                numberSupports++;
            }
            supports = new Support[numberSupports];
            int next = 0;
            for(int i=0; i<dbFields.length; i++) {
                String fieldName = fields[i].getFieldName();
                if(fieldName.equals("scan")) continue;
                if(fieldName.equals("alarm")) continue;
                if(fieldName.equals("timeStamp")) continue;
                Support support = dbFields[i].getSupport();
                if(support==null) continue;
                supports[next++] = support;
            }
            if(alarmSupport!=null) {
                alarmSupport.initialize();
                if(alarmSupport.getSupportState()!=SupportState.readyForStart) {
                    return;
                }
            }
            for(int i=0; i<supports.length; i++) {
                Support support = supports[i];
                support.initialize();
                supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    for(int j=0; j<i; j++) {
                        supports[j].uninitialize();
                    }
                    supports = null;
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            if(alarmSupport!=null) {
                alarmSupport.start();
                supportState = alarmSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            if(supports.length==0) {
                setSupportState(supportState);
                return;
            }
            for(int i=0; i<supports.length; i++) {
                Support support = supports[i];
                support.start();
                supportState = support.getSupportState();
                if(supportState!=SupportState.ready) {
                    for(int j=0; j<i; j++) {
                        supports[j].stop();
                    }
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(alarmSupport!=null) alarmSupport.stop();
            if(supports.length==0) return;
            for(int i=0; i<supports.length; i++) {
                supports[i].stop();
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(alarmSupport!=null) alarmSupport.uninitialize();
            if(supports.length==0) return;
            for(int i=0; i<supports.length; i++) {
                supports[i].uninitialize();
            }
            supports = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            if(alarmSupport!=null) alarmSupport.beginProcess();
            finalResult = RequestResult.success;
            if(supports.length==0) {
                if(alarmSupport!=null) alarmSupport.endProcess();
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
            nextSupport = 0;
            supports[nextSupport].process(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult.compareTo(finalResult)>0) {
                finalResult = requestResult;
            }
            nextSupport++;
            if(nextSupport>=numberSupports || requestResult!=RequestResult.success) {
                if(alarmSupport!=null) alarmSupport.endProcess();
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
            supports[nextSupport].process(this);
        }
    }
}
