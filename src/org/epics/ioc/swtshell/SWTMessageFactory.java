/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.util.MessageNode;
import org.epics.ioc.util.MessageQueue;
import org.epics.ioc.util.MessageQueueFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * Factory that manages messages.
 * It saves messages on a queue and prints the messages via the main SWT thread.
 * @author mrk
 *
 */
public class SWTMessageFactory {

    /**
     * Create and return a Requester interrface.
     * @param name The name for the requester.
     * @param display The parent display.
     * @param consoleText The text window into which the messages are printed.
     * @return A Requester interface.
     */
    public static Requester create(String name,Display display,Text consoleText) {
        return new MessageImpl(name,display,consoleText);
    }

    private static class MessageImpl
    implements Requester, Runnable {

        public MessageImpl(String name,Display display,Text consoleText) {
            this.name = name;
            this.display = display;
            this.consoleText = consoleText;
        }

        private String name;  
        private Display display;
        private Text consoleText = null;

        private MessageQueue messageQueue = MessageQueueFactory.create(3);
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "swtshell " + name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(final String message, MessageType messageType) {
            boolean syncExec = false;
            messageQueue.lock();
            try {
                if(messageQueue.isEmpty()) syncExec = true;
                if(messageQueue.isFull()) {
                    messageQueue.replaceLast(message, messageType);
                } else {
                    messageQueue.put(message, messageType);
                }
            } finally {
                messageQueue.unlock();
            }
            if(syncExec) {
                display.syncExec(this);
            }
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while(true) {
                String message = null;
                int numOverrun = 0;
                messageQueue.lock();
                try {
                    MessageNode messageNode = messageQueue.get();
                    numOverrun = messageQueue.getClearOverrun();
                    if(messageNode==null && numOverrun==0) break;
                    message = messageNode.message;
                } finally {
                    messageQueue.unlock();
                }
                if(numOverrun>0) {
                    consoleText.append(String.format("%n%d missed messages&n", numOverrun));
                }
                if(message!=null) {
                    consoleText.append(String.format("%s%n",message));
                }
            }
        }
    }
}