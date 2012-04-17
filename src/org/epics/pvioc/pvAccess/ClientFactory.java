/*
 * Copyright (c) 2009 by Cosylab
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

package org.epics.pvioc.pvAccess;



import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.Timer.TimerCallback;
import org.epics.pvdata.misc.Timer.TimerNode;
import org.epics.pvdata.misc.TimerFactory;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.install.AfterStartFactory;
import org.epics.pvioc.install.AfterStartNode;
import org.epics.pvioc.install.AfterStartRequester;
import org.epics.pvioc.install.NewAfterStartRequester;

public class ClientFactory {
    static private boolean isRegistered = false;
    /**
     * This initializes the Channel Access client and invokes AfterStart to wait for 2 seconds after database initialization.
     */
    public static synchronized void start() {
        if(isRegistered) return;
        isRegistered = true;
        AfterStartDelay afterStartDelay = new AfterStartDelay();
        afterStartDelay.start();
        org.epics.pvaccess.ClientFactory.start();
    }
    
    // afterStartDelay ensures that no run method gets called until after 2 seconds after
    // the last record has started. This allows time to connect to servers.
    private static class AfterStartDelay implements NewAfterStartRequester,AfterStartRequester,TimerCallback {
        private AfterStartNode afterStartNode = null;
        private AfterStart afterStart = null;
        private Timer timer = null;
        private TimerNode timerNode = null;

        private AfterStartDelay() {}

        private void start() {
        	timer = TimerFactory.create("pvAccessDelay", ThreadPriority.low);
        	timerNode = TimerFactory.createNode(this);
            afterStartNode = AfterStartFactory.allocNode(this);
            AfterStartFactory.newAfterStartRegister(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.NewAfterStartRequester#callback(org.epics.pvioc.install.AfterStart)
         */
        @Override
        public void callback(AfterStart afterStart) {
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode, false, ThreadPriority.middle);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStartRequester#callback(org.epics.pvioc.install.AfterStartNode)
         */
        @Override
        public void callback(AfterStartNode node) {
        	timer.scheduleAfterDelay(timerNode, 2.0);
        }
        @Override
		public void callback() {
        	afterStart.done(afterStartNode);
            afterStart = null;
		}
		@Override
		public void timerStopped() {
			afterStart.done(afterStartNode);
            afterStart = null;
		}
    }
}
