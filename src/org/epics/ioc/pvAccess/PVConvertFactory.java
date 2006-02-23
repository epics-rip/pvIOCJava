/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * @author mrk
 *
 */
public final class PVConvertFactory {
    public static PVConvert getPVConvert()
    {
    	return Convert.getConvert();
    }
}
