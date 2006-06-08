/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.util.*;


/**
 * DBDFactory creates, finds and removes a DBD.
 * A DBD contains the description of Database Definitions:
 *  menu, structure, recordType, and link support.
 * 
 * @author mrk
 * 
 */
public class DBDFactory {

    /**
     * creates and returns a DBD.
     * If a DBD with the specified name already exists
     * the request fails and null is returned.
     * @param name the name for the new DBD.
     * @return the new DBD or null if a DBD with this name already exists.
     */
    public static DBD create(String name) {
        if(find(name)!=null) return null;
        DBD dbd = new DBDInstance(name);
        DBDCreateFactory.createLinkDBDStructure(dbd);
        dbdList.addLast(dbd);
        return dbd;
    }
    
    /**
     * find a DBD with the specified name.
     * @param name of the DBD.
     * @return the DBD or null if ir does not exist.
     */
    public static DBD find(String name) {
        ListIterator<DBD> iter = dbdList.listIterator();
        while(iter.hasNext()) {
            DBD dbd = iter.next();
            if(name.equals(dbd.getName())) return dbd;
        }
        return null;
    }
    
    /**
     * get the complete list of DBDs.
     * @return the Collection.
     */
    public static Collection<DBD> getDBDList() {
        return dbdList;
    }

    /**
     * remove the DBD from the list.
     * @param dbd the BBD to remove.
     */
    public static void remove(DBD dbd) {
        dbdList.remove(dbd);
    }
    
    private static LinkedList<DBD> dbdList;
    static {
        dbdList = new LinkedList<DBD>();
    }

    private static class DBDInstance implements DBD {
        
        private String name;
        private Map<String,DBDMenu> menuMap;
        private Map<String,DBDStructure> structureMap;
        private Map<String,DBDRecordType> recordTypeMap;
        private Map<String,DBDLinkSupport> linkSupportMap;
        
        DBDInstance(String name) {
            this.name = name;
            menuMap = new TreeMap<String,DBDMenu>();
            structureMap = new TreeMap<String,DBDStructure>();
            recordTypeMap = new TreeMap<String,DBDRecordType>();
            linkSupportMap = new TreeMap<String,DBDLinkSupport>();
        }
        
        public String getName() {
            return name;
        }
        
        public boolean addMenu(DBDMenu menuNew) {
            String key = menuNew.getName();
            if(menuMap.containsKey(key)) return false;
            menuMap.put(key,menuNew);
            return true;
        }
        public DBDMenu getMenu(String menuName) {
            return menuMap.get(menuName);
         }
        public Map<String, DBDMenu> getMenuMap() {
            return menuMap;
        }
        
        public boolean addStructure(DBDStructure structure) {
            String key = structure.getStructureName();
            if(structureMap.containsKey(key)) return false;
            structureMap.put(key,structure);
            return true;
        }
        public DBDStructure getStructure(String structureName) {
            return structureMap.get(structureName);
        }
        public Map<String,DBDStructure> getStructureMap() {
            return structureMap;
        }
        
        public boolean addRecordType(DBDRecordType recordType) {
            String key = recordType.getStructureName();
            if(recordTypeMap.containsKey(key)) return false;
            recordTypeMap.put(key,recordType);
            return true;
        }

        public DBDRecordType getRecordType(String recordTypeName) {
            return recordTypeMap.get(recordTypeName);
        }
        public Map<String,DBDRecordType> getRecordTypeMap() {
            return recordTypeMap;
        }
        
        public DBDLinkSupport getLinkSupport(String linkSupportName) {
            return linkSupportMap.get(linkSupportName);
        }
        public boolean addLinkSupport(DBDLinkSupport linkSupport) {
            String key = linkSupport.getLinkSupportName();
            if(linkSupportMap.containsKey(key)) return false;
            linkSupportMap.put(key,linkSupport);
            return true;
        }
        public Map<String,DBDLinkSupport> getLinkSupportMap() {
            return linkSupportMap;
        }
        
    }

}
