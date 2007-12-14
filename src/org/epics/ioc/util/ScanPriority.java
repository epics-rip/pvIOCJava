/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Type;

/**
 * The scan priorities for a record instance that is event or periodically scanned.
 * @author mrk
 *
 */
public enum ScanPriority {
    /**
     * Lowest prority.
     */
    lowest,
    /**
     * Lower priority.
     */
    lower,
    /**
     * Low priority.
     */
    low,
    /**
     * Middle priority.
     */
    middle,
    /**
     * High priority. 
     */
    high,
    /**
     * Higher priority.
     */
    higher,
    /**
     * Highest priority.
     */
    highest;
    
    /**
     * Get the java priority corresponding to each ScanPriority.
     */
    public static final int[] javaPriority = {
        Thread.MIN_PRIORITY,
        Thread.MIN_PRIORITY + 1,
        Thread.NORM_PRIORITY - 1,
        Thread.NORM_PRIORITY,
        Thread.NORM_PRIORITY + 1,
        Thread.MAX_PRIORITY - 1,
        Thread.MAX_PRIORITY};
    
    /**
     * Get the Java priority for this ScanPriority.
     * @return The java priority.
     */
    public int getJavaPriority() {
        return javaPriority[ordinal()];
    }
    /**
     * Get the javaPriority for a given scanPriority.
     * @param scanPriority The scanPriority.
     * @return The java priority.
     */
    public static int getJavaPriority(ScanPriority scanPriority) {
        return scanPriority.getJavaPriority();
    }
    
    private static final String[] scanPriorityChoices = {
        "lowest","lower","low","middle","high","higher","highest"
    };
    /**
     * Convenience method for scan code.
     * @param dbField A field which is potentially a scanPriority structure.
     * @return The Enumerated interface only if dbField has an Enumerated interface and defines
     * the scanPriority values.
     */
    public static Enumerated getScanPriority(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not a scanPriority structure", MessageType.error);
            return null;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        Create create = dbStructure.getCreate();
        if(create==null || !(create instanceof Enumerated)) {
            pvField.message("interface Enumerated not found", MessageType.error);
            return null;
        }
        Enumerated enumerated = (Enumerated)create;
        PVStringArray pvChoices = enumerated.getChoicesField();
        int len = pvChoices.getLength();
        if(len!=scanPriorityChoices.length) {
            pvField.message("not an scanPriority structure", MessageType.error);
            return null;
        }
        StringArrayData data = new StringArrayData();
        pvChoices.get(0, len, data);
        String[] choices = data.data;
        for (int i=0; i<len; i++) {
            if(!choices[i].equals(scanPriorityChoices[i])) {
                pvField.message("not an scanPriority structure", MessageType.error);
                return null;
            }
        }
        pvChoices.setMutable(false);
        return enumerated;
    }
}
