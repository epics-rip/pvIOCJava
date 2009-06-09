/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVField;

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
    public Enumerated getEnumerated() {
        return EnumeratedFactory.getEnumerated(pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelField#getAccessRights()
     */
    public AccessRights getAccessRights() {
        // OK until access security is implemented
        if (pvField.isImmutable()) {
            return AccessRights.read;
        } else {
            return AccessRights.readWrite;
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
