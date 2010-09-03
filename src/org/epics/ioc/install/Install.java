/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.install;

import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;

/**
 * Install structures and records into the master database.
 * @author mrk
 *
 */
public interface Install {
    /**
     * Install structures into the master PVDatabase.
     * A PVDatabase named beingInstalled is created.
     * The xmlFile is parsed and the new structures put into beingInstalled.
     * If no parse errors occur then the new structures are merged into master if
     * 1) no record instances are found in beingInstalled, and 2) none of the structures in beingInstalled are already in the master database.
     * @param xmlFile An xml file defining structures.
     * @param requester The requester.
     * @return (false,true) if the new structures (were not, were) merged into master.
     */
    boolean installStructures(String xmlFile,Requester requester);
    /**
     * Install structures into the master PVDatabase.
     * The new structures are merged into master if
     * 1) no record instances are found in pvDatabase, and 2) none of the structures in pvDatabase are already in the master database.
     * @param pvDatabase The PVDatabase containing the structures to merge.
     * @param requester The requester.
     * @return (false,true) if the new structures (were not, were) merged into master.
     */
    boolean installStructures(PVDatabase pvDatabase,Requester requester);
    /**
     * Install a PVStructure into the master database.
     * It is installed only if the master database does not already have a structure named structureName.
     * @param pvStructure The structure to install.
     * @param requester The requester.
     * @return false,true) if the new structure (was not, was) merged into master.
     */
    boolean installStructure(PVStructure pvStructure,Requester requester);
    /**
     * Install records into the master PVDatabase.
     * A PVDatabase named beingInstalled is created.
     * The xmlFile is parsed and the new records put into beingInstalled.
     * If no parse errors occur then the new records are merged into master if
     * 1) no structures are found in beingInstalled, and 2) none of the records in beingInstalled are already in the master database.
     * @param xmlFile An xml file defining records.
     * @param requester The requester.
     * @return (false,true) if the new records (were not, were) merged into master.
     */
    boolean installRecords(String xmlFile,Requester requester);
    /**
     * Install records into the master PVDatabase.
     * The new records are merged into master if
     * 1) no structures are found in pvDatabase, and 2) none of the records in pvDatabase are already in the master database.
     * @param pvDatabase The PVDatabase containing the records to merge.
     * @param requester The requester.
     * @return (false,true) if the new records (were not, were) merged into master.
     */
    boolean installRecords(PVDatabase pvDatabase,Requester requester);
    /**
     * Install a PVRecord into the master database.
     * It is installed only if the master database does not already have a record named recordName.
     * @param pvRecord The record to install.
     * @param requester The requester.
     * @return false,true) if the new record (was not, was) merged into master.
     */
    boolean installRecord(PVRecord pvRecord,Requester requester);
    
}
