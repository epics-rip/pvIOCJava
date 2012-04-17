/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.install;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListArray;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.misc.ThreadPriority;

/**
 * @author mrk
 *
 */
public class AfterStartFactory {
    public static  AfterStart create() {
        AfterStart afterStart = new AfterStartImpl();
        LinkedListNode<NASRNode>[] listNodes = null;
        int length = 0;
        synchronized(newAfterStartRequesterList) {
            newAfterStartRequestersArray.setNodes(newAfterStartRequesterList);
            listNodes = newAfterStartRequestersArray.getNodes();
            length = newAfterStartRequestersArray.getLength();
        }
        for(int i=0; i<length; i++) {
            NASRNode node = listNodes[i].getObject();
            node.requester.callback(afterStart);
        }
        return afterStart;
    }
    
    public static AfterStartNode allocNode(AfterStartRequester requester) {
        return new ASRNode(requester);
    }
    public static void newAfterStartRegister(NewAfterStartRequester requester) {
        NASRNode node = new NASRNode(requester);
        synchronized(newAfterStartRequesterList) {
            newAfterStartRequesterList.addTail(node.listNode);
        }
    }
    
    public static void newAfterStartUnregister(NewAfterStartRequester requester) {
        synchronized(newAfterStartRequesterList) {
            LinkedListNode<NASRNode> node = newAfterStartRequesterList.getHead();
            while(node!=null) {
                if(node.getObject().requester == requester) {
                    newAfterStartRequesterList.remove(node);
                    return;
                }
                node = newAfterStartRequesterList.getNext(node);
            }
            throw new IllegalStateException("not on list");
        }
    }
    
    // NASR means NewAfterStartRequester
    private static LinkedListCreate<NASRNode> nasrListCreate = new LinkedListCreate<NASRNode>();
    private static final LinkedList<NASRNode> newAfterStartRequesterList = nasrListCreate.create();
    private static final LinkedListArray<NASRNode> newAfterStartRequestersArray = nasrListCreate.createArray();
    
    private static class NASRNode {
        private LinkedListNode<NASRNode> listNode = null;
        private NewAfterStartRequester requester = null;
        private NASRNode(NewAfterStartRequester requester) {
            this.requester = requester;
            listNode = nasrListCreate.createNode(this);
        }
    }

    //ASR means AfterStartRequester
    private static LinkedListCreate<ASRNode> asrListCreate = new LinkedListCreate<ASRNode>();
    private static class AfterStartImpl implements AfterStart {
        private static LinkedListArray<ASRNode> asrListArray = asrListCreate.createArray();
        private static final int[] javaPrioritys = ThreadPriority.javaPriority;
        private LinkedList<ASRNode>[] beforeMergeLists = new LinkedList[javaPrioritys.length];
        private LinkedList<ASRNode>[] afterMergeLists = new LinkedList[javaPrioritys.length];
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreLeft = lock.newCondition();
        private boolean gotAll = true;
        private int listNow = 0;
        private boolean afterMerge = false;

        private AfterStartImpl() {
            for(int i=0; i<javaPrioritys.length; i++) {
                beforeMergeLists[i] = null;
                afterMergeLists[i] = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStart#callRequesters(boolean)
         */
        public void callRequesters(boolean afterMerge) {
            this.afterMerge = afterMerge;
            LinkedList<ASRNode>[] lists = (afterMerge ? afterMergeLists : beforeMergeLists);
            for(listNow = lists.length-1; listNow>=0; listNow --) {
                gotAll = false;
                LinkedList<ASRNode> list = lists[listNow];
                if(list==null) continue;
                LinkedListNode<ASRNode>[] nodes = null;
                int length = 0;
                synchronized(list) {
                    asrListArray.setNodes(list);
                    nodes = asrListArray.getNodes();
                    length = asrListArray.getLength();
                }
                for(int i=0; i<length; i++) {
                	LinkedListNode<ASRNode> node = nodes[i];
                	ASRNode nodeImpl = (ASRNode)node.getObject();
                	if(!nodeImpl.isActive) {
                		nodeImpl.isActive = true;
                		AfterStartRequester requester = nodeImpl.requester;
                		requester.callback(nodeImpl);
                	}
                }
                while(true) {
                    lock.lock();
                    try {
                        if(gotAll) break;
                        moreLeft.await(60,TimeUnit.SECONDS);
                    } catch(InterruptedException e) {}
                    finally {
                        lock.unlock();
                    }
                }
                if(!gotAll) {
                    System.err.println("AfterStart: following AfterStartRequesters did not call done");
                    synchronized(list) {
                        asrListArray.setNodes(list);
                        nodes = asrListArray.getNodes();
                        length = asrListArray.getLength();
                        for(int i=0; i<length; i++) {
                            LinkedListNode<ASRNode> node = nodes[i];
                            list.remove(node);
                            ASRNode nodeImpl = (ASRNode)node.getObject();
                            AfterStartRequester requester = nodeImpl.requester;
                            System.out.println("requester " + requester.toString());
                        }
                    }
                }
            }
            if(afterMerge) asrListArray.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStart#requestCallback(org.epics.pvioc.install.AfterStartNode, boolean, org.epics.pvdata.misc.ThreadPriority)
         */
        public void requestCallback(AfterStartNode node,boolean afterMerge, ThreadPriority priority) {
            LinkedList<ASRNode>[] lists = (afterMerge ? afterMergeLists : beforeMergeLists);
            int index = priority.ordinal();
            if(lists[index]==null) lists[index] = asrListCreate.create();
            ASRNode nodeImpl = (ASRNode)node;
            LinkedList<ASRNode> list = lists[index];
            lists[index].addTail(nodeImpl.listNode);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStart#done(org.epics.pvioc.install.AfterStartNode)
         */
        public void done(AfterStartNode node) {
            LinkedList<ASRNode>[] lists = (afterMerge ? afterMergeLists : beforeMergeLists);
            ASRNode nodeImpl = (ASRNode)node;
            LinkedList<ASRNode> list = lists[listNow];
            boolean isEmpty = false;
            synchronized(list) {
                list.remove(nodeImpl.listNode);
                nodeImpl.isActive = false;
                isEmpty = list.isEmpty();
            }
            if(isEmpty) {
                lock.lock();
                try {
                    gotAll = true;
                    moreLeft.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStart#doneAndRequest(org.epics.pvioc.install.AfterStartNode, boolean, org.epics.pvdata.misc.ThreadPriority)
         */
        public void doneAndRequest(AfterStartNode node, boolean after,ThreadPriority priority) {
            LinkedList<ASRNode>[] lists = (afterMerge ? afterMergeLists : beforeMergeLists);
            ASRNode nodeImpl = (ASRNode)node;
            LinkedList<ASRNode> list = lists[listNow];
            boolean isEmpty = false;
            synchronized(list) {
                list.remove(nodeImpl.listNode);
                isEmpty = list.isEmpty();
            }
            if(after!=afterMerge) {
                if(afterMerge) {
                    throw new IllegalStateException("request for expired queue");
                }
            } else {
                int index = priority.ordinal();
                if(index<=listNow) {
                    throw new IllegalStateException("request for expired queue");
                }
            }
            requestCallback(node,after,priority);
            if(isEmpty) {
                lock.lock();
                try {
                    gotAll = true;
                    moreLeft.signal();
                } finally {
                    lock.unlock();
                }
            }
        }

        
    }
    
    private static class ASRNode implements AfterStartNode {
        private LinkedListNode<ASRNode> listNode = asrListCreate.createNode(this);
        private AfterStartRequester requester = null;
        private boolean isActive = false;
        
        private ASRNode(AfterStartRequester requester) {
            this.requester = requester;
        }
    }
    
}
