/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.pvioc.channelAccess;

import junit.framework.TestCase;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelAccessFactory;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class ChannelCreateDestoryTest extends TestCase {
	
	private static class ChannelRequesterImpl implements ChannelRequester {
		
		Channel channel;
		
		@Override
		public void message(String message, MessageType messageType) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public String getRequesterName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void channelStateChange(org.epics.pvaccess.client.Channel c,
				ConnectionState isConnected) {
			// TODO Auto-generated method stub
		}
				
		@Override
		public synchronized void channelCreated(Status status, org.epics.pvaccess.client.Channel channel) {
			this.channel = channel;
			this.notifyAll();
		}
	};
	
	final static long TIMEOUT_MS = 3000;
	
	private Channel syncCreateChannel(String name)
	{
		ChannelRequesterImpl cr = new ChannelRequesterImpl();
		synchronized (cr) {
			provider.createChannel(name, cr, ChannelProvider.PRIORITY_DEFAULT);
			if (cr.channel == null)
			{
				try {
					cr.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			assertNotNull("failed to create channel", cr.channel);
			return cr.channel;
		}
	}

	private ChannelProvider provider;

    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return ChannelCreateDestoryTest.class.getName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }

    private static final Install install = InstallFactory.get();

    static
	{
		// start javaIOC
        Requester iocRequester = new Listener();
        try {
            install.installStructures("xml/structures.xml",iocRequester);
            install.installRecords("example/exampleDB.xml",iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
		
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		provider = ChannelAccessFactory.getChannelAccess().getProvider("local");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
	}

	/**
	 * Here is no memory leak...
	 */
	/*
	public void testAllocation()
	{
		final int COUNT = 1000000;
		for (int i = 0; i <= COUNT; i++)
		{
			ByteBuffer bb = ByteBuffer.allocate(16*1024);
			if (bb.isDirect())	// do something with it...
				System.out.println("is direct");
			if ((i % 1000)==0) 
			{
				System.gc();
				System.out.println(i+" : used by VM " +Runtime.getRuntime().totalMemory() + ", free:" + Runtime.getRuntime().freeMemory());
			}
		}
	}
	*/

	/**
	 * But here it is!!!
	 */
	public void testConnectDisconnect()
	{
		final int COUNT = 1000000;
		for (int i = 0; i <= COUNT; i++)
		{
			Channel channel = syncCreateChannel("valueOnly");
			channel.destroy();
			if ((i % 1000)==0) 
			{
				System.gc();
				System.out.println(i+" : used by VM " +Runtime.getRuntime().totalMemory() + ", free:" + Runtime.getRuntime().freeMemory());
			}
		}
	}
}
