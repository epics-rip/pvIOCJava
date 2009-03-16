/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Base class for octet interpose implementations.
 * It implements all octet methods by calling the lower level octet methods..
 * Thus an interpose implementation only needs to implement methods it wants to modify.
 * @author mrk
 *
 */
public abstract class OctetInterposeBase implements Octet
{
    private Octet octet;
    
    /**
     * The constructor
     * @param octet The interface to the lower level implementation.
     */
    protected OctetInterposeBase(Octet octet) {
        this.octet = octet;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.OctetInterruptListener)
     */
    public Status addInterruptUser(User user, OctetInterruptListener octetInterruptListener) {
        return octet.addInterruptUser(user, octetInterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#flush(org.epics.ioc.pdrv.User)
     */
    public Status flush(User user) {
        return octet.flush(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#getInputEos(org.epics.ioc.pdrv.User, byte[])
     */
    public Status getInputEos(User user, byte[] eos) {
        return octet.getInputEos(user, eos);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#getOutputEos(org.epics.ioc.pdrv.User, byte[])
     */
    public Status getOutputEos(User user, byte[] eos) {
        return octet.getOutputEos(user, eos);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#read(org.epics.ioc.pdrv.User, byte[], int)
     */
    public Status read(User user, byte[] data, int nbytes) {
        return octet.read(user, data, nbytes);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.OctetInterruptListener)
     */
    public Status removeInterruptUser(User user, OctetInterruptListener octetInterruptListener) {
        return octet.removeInterruptUser(user, octetInterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#setInputEos(org.epics.ioc.pdrv.User, byte[], int)
     */
    public Status setInputEos(User user, byte[] eos, int eosLen) {
        return octet.setInputEos(user, eos, eosLen);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#setOutputEos(org.epics.ioc.pdrv.User, byte[], int)
     */
    public Status setOutputEos(User user, byte[] eos, int eosLen) {
        return octet.setOutputEos(user, eos, eosLen);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Octet#write(org.epics.ioc.pdrv.User, byte[], int)
     */
    public Status write(User user, byte[] data, int nbytes) {
        return octet.write(user, data, nbytes);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return octet.getInterfaceName();
    }
}
