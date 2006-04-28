/**
 * 
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

        public boolean addLinkSupport(DBDLinkSupport linkSupport) {
            ListIterator<DBDLinkSupport> iter = linkSupportList.listIterator();
            while(iter.hasNext()) {
                DBDLinkSupport support = iter.next();
                int compare = support.getLinkSupportName().compareTo(
                        linkSupport.getLinkSupportName());
                if(compare==0) return false;
                if(compare>0) {
                    iter.previous();
                    iter.add(linkSupport);
                    return true;
                }
            }
            linkSupportList.add(linkSupport);
            return true;
        }
        
        public boolean addMenu(DBDMenu menuNew) {
            ListIterator<DBDMenu> iter = menuList.listIterator();
            while(iter.hasNext()) {
                DBDMenu menu = iter.next();
                int compare = menu.getName().compareTo(
                        menuNew.getName());
                if(compare==0) return false;
                if(compare>0) {
                    iter.previous();
                    iter.add(menuNew);
                    return true;
                }
            }
            menuList.add(menuNew);
            return true;
        }

        public boolean addRecordType(DBDRecordType recordType) {
            ListIterator<DBDRecordType> iter = recordTypeList.listIterator();
            while(iter.hasNext()) {
                DBDRecordType struct = iter.next();
                int compare = struct.getStructureName().compareTo(
                        recordType.getStructureName());
                if(compare==0) return false;
                if(compare>0) {
                    iter.previous();
                    iter.add(recordType);
                    return true;
                }
            }
            recordTypeList.add(recordType);
            return true;
        }

        public boolean addStructure(DBDStructure structure) {
            ListIterator<DBDStructure> iter = structureList.listIterator();
            while(iter.hasNext()) {
                DBDStructure struct = iter.next();
                int compare = struct.getStructureName().compareTo(
                        structure.getStructureName());
                if(compare==0) return false;
                if(compare>0) {
                    iter.previous();
                    iter.add(structure);
                    return true;
                }
            }
            structureList.add(structure);
            return true;
        }
        public DBDRecordType getDBDRecordType(String recordTypeName) {
            for(DBDRecordType recordType : recordTypeList) {
                if(recordType.getStructureName().equals(recordTypeName))
                    return recordType;
            }
            return null;
        }
        public Collection<DBDRecordType> getDBDRecordTypeList() {
            return recordTypeList;
        }
        public DBDStructure getDBDStructure(String structureName) {
            for(DBDStructure structure : structureList) {
                if(structure.getStructureName().equals(structureName)) return structure;
            }
            return null;
        }
        public Collection<DBDStructure> getDBDStructureList() {
            return structureList;
        }
        public DBDLinkSupport getLinkSupport(String linkSupportName) {
            for(DBDLinkSupport support : linkSupportList) {
                if(support.getLinkSupportName().equals(linkSupportName)) return support;
            }
            return null;
        }
        public Collection<DBDLinkSupport> getLinkSupportList() {
            return linkSupportList;
        }
        public DBDMenu getMenu(String menuName) {
            for(DBDMenu menu : menuList) {
                if(menu.getName().equals(menuName)) return menu;
            }
            return null;
         }
        public Collection<DBDMenu> getMenuList() {
            return menuList;
        }
        public String getName() {
            return name;
        }
        DBDInstance(String name) {
            this.name = name;
            menuList = new LinkedList<DBDMenu>();
            structureList = new LinkedList<DBDStructure>();
            recordTypeList = new LinkedList<DBDRecordType>();
            linkSupportList = new LinkedList<DBDLinkSupport>();
        }
        
        private String name;
        private LinkedList<DBDMenu> menuList;
        private LinkedList<DBDStructure> structureList;
        private LinkedList<DBDRecordType> recordTypeList;
        private LinkedList<DBDLinkSupport> linkSupportList;
    }

}
