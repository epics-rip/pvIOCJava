/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.v3a;


/**
 * @author mrk
 *
 */
public class AsynOctet {
    private long cPvt;
    
    public AsynOctet(AsynLink asynLink) {
        cPvt = init(asynLink);
        System.out.println("after init");
    }
    
    public void readIt() {
        read(cPvt,null);
    }
    
    private native long init(AsynLink asynLink);
    
    private native int read(long cPvt,AsynUser asynUser);
}
