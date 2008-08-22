/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;

/**
 * Abstract class for implementing ChannelField.
 * @author mrk
 *
 */
public abstract class AbstractChannelField implements ChannelField{
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();
    private PVField pvField;

    /**
     * Constructor
     * @param pvField The pvField for the channelField.
     */
    protected AbstractChannelField(PVField pvField) {
        this.pvField = pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getPVField()
     */
    public PVField getPVField() {
        return pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#postPut()
     */
    public abstract void postPut();
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getPropertyNames()
     */
    public String[] getPropertyNames() {
        return pvProperty.getPropertyNames(pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#findProperty(java.lang.String)
     */
    public abstract ChannelField findProperty(String propertyName);
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#createChannelField(java.lang.String)
     */
    public abstract ChannelField createChannelField(String fieldName);
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getEnumerated()
     */
    public PVEnumerated getEnumerated() {
        return pvField.getPVEnumerated();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getAccessRights()
     */
    public AccessRights getAccessRights() {
        // OK until access security is implemented
        if (pvField.isMutable()) {
            return AccessRights.readWrite;
        } else {
            return AccessRights.read;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getField()
     */
    public Field getField() {
        return pvField.getField();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return pvField.getField().toString();
    }
}
