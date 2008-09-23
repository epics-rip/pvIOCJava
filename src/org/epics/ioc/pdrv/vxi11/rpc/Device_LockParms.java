/*
 * Automatically generated by jrpcgen 1.0.7 on 9/3/08 7:17 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package org.epics.ioc.pdrv.vxi11.rpc;
import java.io.IOException;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrDecodingStream;
import org.acplt.oncrpc.XdrEncodingStream;

public class Device_LockParms implements XdrAble {
    public Device_Link lid;
    public Device_Flags flags;
    public int lock_timeout;

    public Device_LockParms() {
    }

    public Device_LockParms(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        lid.xdrEncode(xdr);
        flags.xdrEncode(xdr);
        xdr.xdrEncodeInt(lock_timeout);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        lid = new Device_Link(xdr);
        flags = new Device_Flags(xdr);
        lock_timeout = xdr.xdrDecodeInt();
    }

}
// End of Device_LockParms.java