/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.epics.pvdata.misc.MessageNode;
import org.epics.pvdata.misc.MessageQueue;
import org.epics.pvdata.misc.MessageQueueFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;

/**
 * Factory that manages messages.
 * It saves messages on a queue and prints the messages via the main SWT thread.
 * @author mrk
 *
 */
public class SWTMessageFactory {

    /**
     * Create and return a Requester interface.
     * @param name The name for the requester.
     * @param display The parent display.
     * @param consoleText The text window into which the messages are printed.
     * @return A Requester interface.
     */
    public static Requester create(String name,Display display,Text consoleText) {
        return new MessageImpl(name,display,consoleText);
    }

    private static class MessageImpl implements Requester, Runnable {

        public MessageImpl(String name,Display display,Text consoleText) {
            this.name = name;
            this.display = display;
            this.consoleText = consoleText;
        }

        private String name;  
        private Display display;
        private Text consoleText = null;

        private MessageQueue messageQueue = MessageQueueFactory.create(300);
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "swtshell " + name;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(final String message, MessageType messageType) {
            boolean syncExec = false;
            synchronized(messageQueue) {
                if(messageQueue.isEmpty()) syncExec = true;
                messageQueue.put(message, messageType,true);
            }
            if(consoleText.isDisposed()) return;
            if(display.isDisposed()) return;
            if(syncExec) {
                display.asyncExec(this);
            }
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while(true) {
                String message = null;
                int numOverrun = 0;
                synchronized(messageQueue) {
                    MessageNode messageNode = messageQueue.get();
                    numOverrun = messageQueue.getClearOverrun();
                    if(messageNode==null && numOverrun==0) break;
                    if(messageNode!=null) message = messageNode.message;
                }
                if(display.isDisposed()) break;
                if(consoleText.isDisposed()) break;
                if(numOverrun>0) {
                    consoleText.append(String.format("%n%d missed messages%n", numOverrun));
                }
                if(message!=null) {
                    consoleText.append(String.format("%n%s%n",message));
                }
            }
        }
    }
}
