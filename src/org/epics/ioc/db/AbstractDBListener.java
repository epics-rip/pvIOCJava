/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

/**
 * Implements all methods of DBListener as a noop.
 * @author mrk
 *
 */
public abstract class AbstractDBListener implements DBListener{
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#beginProcess()
     */
    public void beginProcess() {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
     */
    public void beginPut(DBStructure dbStructure) {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
     */
    public void dataPut(DBField requested, DBField dbField) {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
     */
    public void dataPut(DBField dbField) {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#endProcess()
     */
    public void endProcess() {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
     */
    public void endPut(DBStructure dbStructure) {}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
     */
    public void unlisten(RecordListener listener) {}
}
