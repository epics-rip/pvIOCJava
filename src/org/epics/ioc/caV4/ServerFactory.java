/*
 * Copyright (c) 2007 by Cosylab
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

package org.epics.ioc.caV4;

import java.util.ArrayList;

import org.epics.ca.CAException;
import org.epics.ca.CAStatus;
import org.epics.ca.CAStatusException;
import org.epics.ca.core.impl.server.ServerContextImpl;
import org.epics.ca.core.impl.server.plugins.DefaultBeaconServerDataProvider;
import org.epics.ca.server.ReadCallback;
import org.epics.ca.server.plugins.IntrospectionSearchProvider;
import org.epics.ca.util.WildcharMatcher;
import org.epics.ioc.channelAccess.ChannelAccessFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadReady;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.ScalarType;

public class ServerFactory {
    /**
     * This starts the Channel Access Server.
     */
    public static void start() {
        new ThreadInstance();
    }
    
    private static final ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    
    private static class JavaIOCIntrospectionSearchProvider implements IntrospectionSearchProvider
    {
    	/**
    	 * Server context to query.
    	 */
    	protected ServerContextImpl context;
    	
    	/**
    	 * Constructor.
    	 * @param context server context to be monitored.
    	 */
    	public JavaIOCIntrospectionSearchProvider(ServerContextImpl context) {
    		this.context = context;
    	}

		/* (non-Javadoc)
		 * @see org.epics.ca.server.plugins.IntrospectionSearchProvider#introspectionSearch(org.epics.ca.server.ProcessVariableReadCallback, org.epics.pvData.pv.PVField)
		 */
		public void introspectionSearch(ReadCallback callback, PVField searchData) throws CAException
		{
			// data check
			if (!(searchData instanceof PVString))
				throw new CAStatusException(CAStatus.BADTYPE);
			
			final String name = ((PVString)searchData).get();
			
			// simple name wildchar test (no tags)
			ArrayList<String> result = new ArrayList<String>();
			String[] recordNames = PVDatabaseFactory.getMaster().getRecordNames();
			for (String recordName : recordNames)
				if (WildcharMatcher.match(name, recordName))
					result.add(recordName);
			
			Array field = FieldFactory.getFieldCreate().createArray("value", ScalarType.pvString);
			PVStringArray pvStringArray = (PVStringArray)PVDataFactory.getPVDataCreate().createPVArray(null, field);
			pvStringArray.put(0, result.size(), result.toArray(new String[result.size()]), 0);
			
			callback.readCompleted(pvStringArray, CAStatus.NORMAL);
		}
    	
    }
    
    private static class ThreadInstance implements RunnableReady {

        private ThreadInstance() {
            threadCreate.create("caV4Server", 3, this);
        }
        
    	/**
         * JCA server context.
         */
        private ServerContextImpl context = null;
        
        /**
         * Initialize JCA context.
         * @throws CAException	throws on any failure.
         */
        private void initialize() throws CAException {
            
    		// Create a context with default configuration values.
    		context = new ServerContextImpl();
    		context.setBeaconServerStatusProvider(new DefaultBeaconServerDataProvider(context));
    		context.setIntrospectionSearachProvider(new JavaIOCIntrospectionSearchProvider(context));
    		
    		context.initialize(ChannelAccessFactory.getChannelAccess());

    		// Display basic information about the context.
            System.out.println(context.getVersion().getVersionString());
            context.printInfo(); System.out.println();
        }

        /**
         * Destroy JCA server  context.
         */
        private void destroy() {
            
            try {

                // Destroy the context, check if never initialized.
                if (context != null)
                    context.destroy();
                
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            try {
                // initialize context
                initialize();
                threadReady.ready();
                System.out.println("Running server...");
                // run server 
                context.run(0);
                System.out.println("Done.");
            } catch (Throwable th) {
                th.printStackTrace();
            }
            finally {
                // always finalize
                destroy();
            }
        }
    }
}