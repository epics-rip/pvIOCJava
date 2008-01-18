/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;
import org.epics.ioc.ca.ChannelProvider;
import gov.aps.jca.Context;
/**
 * A V3 ChannelProvider.
 * @author mrk
 *
 */
public interface V3ChannelProvider extends ChannelProvider{
    /**
     * Get the JCA Context.
     * @return The interface.
     */
    Context getContext();
}
