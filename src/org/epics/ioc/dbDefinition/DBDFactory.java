/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.locks.*;


/**
 * DBDFactory creates, finds and removes a DBD.
 * A DBD contains the description of Database Definitions:
 *  menu, structure, recordType, and support.
 * 
 * @author mrk
 * 
 */
public class DBDFactory {
    private static TreeMap<String,DBD> dbdMap = new TreeMap<String,DBD>();
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /**
     * Creates and returns a DBD.
     * If a DBD with the specified name already exists
     * the request fails and null is returned.
     * @param name The name for the new DBD.
     * @param masterDBD The masterDBD or null if no master or this is the master.
     * @return The new DBD or null if a DBD with this name already exists.
     */
    public static DBD create(String name, DBD masterDBD) {
        rwLock.writeLock().lock();
        try {
            if(dbdMap.get(name)!=null) return null;
            DBD dbd = new DBDInstance(name,masterDBD);
            dbdMap.put(name,dbd);
            return dbd;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    /**
     * Remove the DBD from the list.
     * @param dbd The BBD to remove.factoryName
     */
    public static void remove(DBD dbd) {
        rwLock.writeLock().lock();
        try {
            dbdMap.remove(dbd.getName());
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    /**
     * Find a DBD with the specified name.
     * @param name The DBD name.
     * @return The DBD or null if it does not exist.
     */
    public static DBD find(String name) {
        rwLock.readLock().lock();
        try {
            return dbdMap.get(name);
        } finally {
            rwLock.readLock().unlock();
        }
    }    
    /**
     * Get a map of the DBDs.
     * @return the Collection.
     */
    public static Map<String,DBD> getDBDList() {
        rwLock.readLock().lock();
        try {
            return (Map<String,DBD>)dbdMap.clone();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * List each DBD that has a name that matches the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the list.
     */
    public static String list(String regularExpression) {
        StringBuilder result = new StringBuilder();
        if(regularExpression==null) regularExpression = ".*";
        Pattern pattern;
        try {
            pattern = Pattern.compile(regularExpression);
        } catch (PatternSyntaxException e) {
            return "PatternSyntaxException: " + e;
        }
        rwLock.readLock().lock();
        try {
            Set<String> keys = dbdMap.keySet();
            for(String key: keys) {
                DBD dbd = dbdMap.get(key);
                String name = dbd.getName();
                if(pattern.matcher(name).matches()) {
                    result.append(" " + name);
                }
            }
            return result.toString();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private static class DBDInstance implements DBD {
        
        private String name;
        private DBDInstance masterDBD = null;
        private TreeMap<String,DBDMenu> menuMap = new TreeMap<String,DBDMenu>();
        private TreeMap<String,DBDStructure> structureMap = new TreeMap<String,DBDStructure>();
        private TreeMap<String,DBDRecordType> recordTypeMap = new TreeMap<String,DBDRecordType>();
        private TreeMap<String,DBDSupport> supportMap = new TreeMap<String,DBDSupport>();
        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        
        private DBDInstance(String name,DBD masterDBD) {
            this.name = name;
            this.masterDBD = (DBDInstance)masterDBD;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getName()
         */
        public String getName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getMasterDBD()
         */
        public DBD getMasterDBD() {
            return masterDBD;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#mergeIntoMaster()
         */
        public void mergeIntoMaster() {
            if(masterDBD==null) return;
            Set<String> keys;
            rwLock.writeLock().lock();
            try {
                keys = menuMap.keySet();
                for(String key: keys) {
                    masterDBD.addMenu(menuMap.get(key));
                }
                menuMap.clear();
                keys = structureMap.keySet();
                for(String key: keys) {
                    masterDBD.addStructure(structureMap.get(key));
                }
                structureMap.clear();
                keys = recordTypeMap.keySet();
                for(String key: keys) {
                    masterDBD.addRecordType(recordTypeMap.get(key));
                }
                recordTypeMap.clear();
                keys = supportMap.keySet();
                for(String key: keys) {
                    masterDBD.addSupport(supportMap.get(key));
                }
                supportMap.clear();
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#addMenu(org.epics.ioc.dbDefinition.DBDMenu)
         */
        public boolean addMenu(DBDMenu menu) {
            rwLock.writeLock().lock();
            try {
                String key = menu.getName();
                if(menuMap.containsKey(key)) return false;
                if(masterDBD!=null && masterDBD.getMenu(key)!=null) return false;
                menuMap.put(key,menu);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getMenu(java.lang.String)
         */
        public DBDMenu getMenu(String menuName) {
            rwLock.readLock().lock();
            try {
                DBDMenu dbdMenu = null;
                dbdMenu = menuMap.get(menuName);
                if(dbdMenu==null && masterDBD!=null) dbdMenu = masterDBD.getMenu(menuName);
                return dbdMenu;
            } finally {
                rwLock.readLock().unlock();
            }
         }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getMenuMap()
         */
        public Map<String, DBDMenu> getMenuMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBDMenu>)menuMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#addStructure(org.epics.ioc.dbDefinition.DBDStructure)
         */
        public boolean addStructure(DBDStructure structure) {
            rwLock.writeLock().lock();
            try {
                String key = structure.getName();
                if(structureMap.containsKey(key)) return false;
                if(masterDBD!=null && masterDBD.getStructure(key)!=null) return false;
                structureMap.put(key,structure);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getStructure(java.lang.String)
         */
        public DBDStructure getStructure(String structureName) {
            rwLock.readLock().lock();
            try {
                DBDStructure dbdStructure = null;
                dbdStructure = structureMap.get(structureName);
                if(dbdStructure==null && masterDBD!=null) dbdStructure = masterDBD.getStructure(structureName);
                return dbdStructure;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getStructureMap()
         */
        public Map<String,DBDStructure> getStructureMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBDStructure>)structureMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#addRecordType(org.epics.ioc.dbDefinition.DBDRecordType)
         */
        public boolean addRecordType(DBDRecordType recordType) {
            rwLock.writeLock().lock();
            try {
                String key = recordType.getName();
                if(recordTypeMap.containsKey(key)) return false;
                if(masterDBD!=null && masterDBD.getRecordType(key)!=null) return false;
                recordTypeMap.put(key,recordType);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getRecordType(java.lang.String)
         */
        public DBDRecordType getRecordType(String recordTypeName) {
            rwLock.readLock().lock();
            try {
                DBDRecordType dbdRecordType = null;
                dbdRecordType = recordTypeMap.get(recordTypeName);
                if(dbdRecordType==null && masterDBD!=null) dbdRecordType = masterDBD.getRecordType(recordTypeName);
                return dbdRecordType;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getRecordTypeMap()
         */
        public Map<String,DBDRecordType> getRecordTypeMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBDRecordType>)recordTypeMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getSupport(java.lang.String)
         */
        public DBDSupport getSupport(String supportName) {
            rwLock.readLock().lock();
            try {
                DBDSupport dbdSupport = null;
                dbdSupport = supportMap.get(supportName);
                if(dbdSupport==null && masterDBD!=null) dbdSupport = masterDBD.getSupport(supportName);
                return dbdSupport;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#addSupport(org.epics.ioc.dbDefinition.DBDSupport)
         */
        public boolean addSupport(DBDSupport support) {
            rwLock.writeLock().lock();
            try {
                String key = support.getSupportName();
                if(supportMap.containsKey(key)) return false;
                if(masterDBD!=null && masterDBD.getSupport(key)!=null) return false;
                supportMap.put(key,support);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#getSupportMap()
         */
        public Map<String,DBDSupport> getSupportMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBDSupport>)supportMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#menuList(java.lang.String)
         */
        public String menuList(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = menuMap.keySet();
                for(String key: keys) {
                    DBDMenu dbdMenu = menuMap.get(key);
                    String name = dbdMenu.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + name);
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#menuToString(java.lang.String)
         */
        public String menuToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = menuMap.keySet();
                for(String key: keys) {
                    DBDMenu dbdMenu = menuMap.get(key);
                    String name = dbdMenu.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdMenu.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#structureList(java.lang.String)
         */
        public String structureList(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = structureMap.keySet();
                for(String key: keys) {
                    DBDStructure dbdStructure = structureMap.get(key);
                    String name = dbdStructure.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + name);
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#structureToString(java.lang.String)
         */
        public String structureToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = structureMap.keySet();
                for(String key: keys) {
                    DBDStructure dbdStructure = structureMap.get(key);
                    String name = dbdStructure.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdStructure.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#recordTypeList(java.lang.String)
         */
        public String recordTypeList(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = recordTypeMap.keySet();
                for(String key: keys) {
                    DBDRecordType dbdRecordType = recordTypeMap.get(key);
                    String name = dbdRecordType.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + name);
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#recordTypeToString(java.lang.String)
         */
        public String recordTypeToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = recordTypeMap.keySet();
                for(String key: keys) {
                    DBDRecordType dbdRecordType = recordTypeMap.get(key);
                    String name = dbdRecordType.getName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdRecordType.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#supportList(java.lang.String)
         */
        public String supportList(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = supportMap.keySet();
                for(String key: keys) {
                    DBDSupport dbdSupport = supportMap.get(key);
                    String name = dbdSupport.getSupportName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + name);
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBD#supportToString(java.lang.String)
         */
        public String supportToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = supportMap.keySet();
                for(String key: keys) {
                    DBDSupport dbdSupport = supportMap.get(key);
                    String name = dbdSupport.getSupportName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdSupport.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        
    }

}
