/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.pv.*;


/**
 * DBDFactory creates a Database Definition Database (DBD) and automatically creates the master DBD.
 * A DBD contains the description of Database Definitions:
 *  menu, structure, recordType, and support.
 * The masterDBD automatically creates a DBDStructure which has structureName = "null: and has 0 fields.
 * @author mrk
 * 
 */
public class DBDFactory {
    private static DBDInstance masterDBD;
    
    static {
        masterDBD = new DBDInstance("master");
        FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        DBDStructure dbdStructure = masterDBD.createStructure(
            "null", new Field[0],new Property[0], fieldCreate.createFieldAttribute());
        masterDBD.addStructure(dbdStructure);
    }
    /**
     * Creates and returns a DBD.
     * @param name The name for the new DBD.
     * @return The new DBD.
     */
    public static DBD create(String name) {
        if(name.equals("master")) return masterDBD;
        return new DBDInstance(name);
    }
    /**
     * Get the master DBD.
     * @return The master DBD.
     */
    public static DBD getMasterDBD() {
        return masterDBD;
    }
    
    private static class DBDInstance implements DBD {
        private String name;
        private TreeMap<String,DBDMenu> menuMap = new TreeMap<String,DBDMenu>();
        private TreeMap<String,DBDStructure> structureMap = new TreeMap<String,DBDStructure>();
        private TreeMap<String,DBDRecordType> recordTypeMap = new TreeMap<String,DBDRecordType>();
        private TreeMap<String,DBDSupport> supportMap = new TreeMap<String,DBDSupport>();
        private TreeMap<String,DBDLinkSupport> linkSupportMap = new TreeMap<String,DBDLinkSupport>();
        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        
        private DBDInstance(String name) {
            this.name = name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getName()
         */
        public String getName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getMasterDBD()
         */
        public DBD getMasterDBD() {
            return masterDBD;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#mergeIntoMaster()
         */
        public void mergeIntoMaster() {
            if(getMasterDBD()==this) return;
            rwLock.writeLock().lock();
            try {
                masterDBD.merge(menuMap,structureMap,recordTypeMap,supportMap,linkSupportMap);
                menuMap.clear();
                structureMap.clear();
                recordTypeMap.clear();
                supportMap.clear();
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createLinkSupport(java.lang.String, java.lang.String, java.lang.String)
         */
        public DBDLinkSupport createLinkSupport(String supportName, String factoryName,
                String configurationStructureName)
        {
            return new LinkSupportInstance(supportName,factoryName,configurationStructureName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createMenu(java.lang.String, java.lang.String[])
         */
        public DBDMenu createMenu(String menuName, String[] choices) {
            return new MenuInstance(menuName,choices);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createRecordType(java.lang.String, org.epics.ioc.pv.Field[], org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public DBDRecordType createRecordType(String name, Field[] field,
            Property[] property, FieldAttribute fieldAttribute)
        {
            return new RecordTypeInstance(name,field,property,fieldAttribute);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createStructure(java.lang.String, org.epics.ioc.pv.Field[], org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public DBDStructure createStructure(String name, Field[] field,
            Property[] property, FieldAttribute fieldAttribute)
        {
            return new StructureInstance(name,field,property,fieldAttribute);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createSupport(java.lang.String, java.lang.String)
         */
        public DBDSupport createSupport(String supportName, String factoryName) {
            return new SupportInstance(supportName,factoryName);
        }
        // merge allows master to be locked once
        private void merge(TreeMap<String,DBDMenu> menu,
                TreeMap<String,DBDStructure> structure,
                TreeMap<String,DBDRecordType> recordType,
                TreeMap<String,DBDSupport> support,
                TreeMap<String,DBDLinkSupport> linkSupport)
        {
            Set<String> keys;
            rwLock.writeLock().lock();
            try {
                keys = menu.keySet();
                for(String key: keys) {
                    menuMap.put(key,menu.get(key));
                }
                keys = structure.keySet();
                for(String key: keys) {
                    structureMap.put(key,structure.get(key));
                }
                keys = recordType.keySet();
                for(String key: keys) {
                    recordTypeMap.put(key,recordType.get(key));
                }
                keys = support.keySet();
                for(String key: keys) {
                    supportMap.put(key,support.get(key));
                }
                keys = linkSupport.keySet();
                for(String key: keys) {
                    linkSupportMap.put(key,linkSupport.get(key));
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#addMenu(org.epics.ioc.dbDefinition.DBDMenu)
         */
        public boolean addMenu(DBDMenu menu) {
            rwLock.writeLock().lock();
            try {
                String key = menu.getMenuName();
                if(menuMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getMenu(key)!=null) return false;
                menuMap.put(key,menu);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getMenu(java.lang.String)
         */
        public DBDMenu getMenu(String menuName) {
            rwLock.readLock().lock();
            try {
                DBDMenu dbdMenu = null;
                dbdMenu = menuMap.get(menuName);
                if(dbdMenu==null && this!=masterDBD) dbdMenu = masterDBD.getMenu(menuName);
                return dbdMenu;
            } finally {
                rwLock.readLock().unlock();
            }
         }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getMenuMap()
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
         * @see org.epics.ioc.dbd.DBD#addStructure(org.epics.ioc.dbDefinition.DBDStructure)
         */
        public boolean addStructure(DBDStructure structure) {
            rwLock.writeLock().lock();
            try {
                String key = structure.getFieldName();
                if(structureMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getStructure(key)!=null) return false;
                structureMap.put(key,structure);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getStructure(java.lang.String)
         */
        public DBDStructure getStructure(String structureName) {
            rwLock.readLock().lock();
            try {
                DBDStructure dbdStructure = null;
                dbdStructure = structureMap.get(structureName);
                if(dbdStructure==null && this!=masterDBD) dbdStructure = masterDBD.getStructure(structureName);
                return dbdStructure;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getStructureMap()
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
         * @see org.epics.ioc.dbd.DBD#addRecordType(org.epics.ioc.dbDefinition.DBDRecordType)
         */
        public boolean addRecordType(DBDRecordType recordType) {
            rwLock.writeLock().lock();
            try {
                String key = recordType.getFieldName();
                if(recordTypeMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getRecordType(key)!=null) return false;
                recordTypeMap.put(key,recordType);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getRecordType(java.lang.String)
         */
        public DBDRecordType getRecordType(String recordTypeName) {
            rwLock.readLock().lock();
            try {
                DBDRecordType dbdRecordType = null;
                dbdRecordType = recordTypeMap.get(recordTypeName);
                if(dbdRecordType==null && this!=masterDBD) dbdRecordType = masterDBD.getRecordType(recordTypeName);
                return dbdRecordType;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getRecordTypeMap()
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
         * @see org.epics.ioc.dbd.DBD#getSupport(java.lang.String)
         */
        public DBDSupport getSupport(String supportName) {
            rwLock.readLock().lock();
            try {
                DBDSupport dbdSupport = null;
                dbdSupport = supportMap.get(supportName);
                if(dbdSupport==null && this!=masterDBD) dbdSupport = masterDBD.getSupport(supportName);
                return dbdSupport;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#addSupport(org.epics.ioc.dbDefinition.DBDSupport)
         */
        public boolean addSupport(DBDSupport support) {
            rwLock.writeLock().lock();
            try {
                String key = support.getSupportName();
                if(supportMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getSupport(key)!=null) return false;
                supportMap.put(key,support);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getSupportMap()
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
         * @see org.epics.ioc.dbd.DBD#getLinkSupport(java.lang.String)
         */
        public DBDLinkSupport getLinkSupport(String supportName) {
            rwLock.readLock().lock();
            try {
                DBDLinkSupport dbdLinkSupport = null;
                dbdLinkSupport = linkSupportMap.get(supportName);
                if(dbdLinkSupport==null && this!=masterDBD) dbdLinkSupport = masterDBD.getLinkSupport(supportName);
                return dbdLinkSupport;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#addLinkSupport(org.epics.ioc.dbDefinition.DBDLinkSupport)
         */
        public boolean addLinkSupport(DBDLinkSupport linkSupport) {
            rwLock.writeLock().lock();
            try {
                String key = linkSupport.getSupportName();
                if(linkSupportMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getLinkSupport(key)!=null) return false;
                linkSupportMap.put(key,linkSupport);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getLinkSupportMap()
         */
        public Map<String,DBDLinkSupport> getLinkSupportMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBDLinkSupport>)linkSupportMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#menuList(java.lang.String)
         */
        public String[] menuList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = menuMap.keySet();
                for(String key: keys) {
                    DBDMenu dbdMenu = menuMap.get(key);
                    String name = dbdMenu.getMenuName();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#menuToString(java.lang.String)
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
                    String name = dbdMenu.getMenuName();
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
         * @see org.epics.ioc.dbd.DBD#structureList(java.lang.String)
         */
        public String[] structureList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = structureMap.keySet();
                for(String key: keys) {
                    DBDStructure dbdStructure = structureMap.get(key);
                    String name = dbdStructure.getFieldName();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#structureToString(java.lang.String)
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
                    String name = dbdStructure.getFieldName();
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
         * @see org.epics.ioc.dbd.DBD#recordTypeList(java.lang.String)
         */
        public String[] recordTypeList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = recordTypeMap.keySet();
                for(String key: keys) {
                    DBDRecordType dbdRecordType = recordTypeMap.get(key);
                    String name = dbdRecordType.getFieldName();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#recordTypeToString(java.lang.String)
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
                    String name = dbdRecordType.getFieldName();
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
         * @see org.epics.ioc.dbd.DBD#supportList(java.lang.String)
         */
        public String[] supportList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = supportMap.keySet();
                for(String key: keys) {
                    DBDSupport dbdSupport = supportMap.get(key);
                    String name = dbdSupport.getSupportName();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#supportToString(java.lang.String)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#linkSupportList(java.lang.String)
         */
        public String[] linkSupportList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = linkSupportMap.keySet();
                for(String key: keys) {
                    DBDLinkSupport dbdLinkSupport = linkSupportMap.get(key);
                    String name = dbdLinkSupport.getSupportName();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#linkSupportToString(java.lang.String)
         */
        public String linkSupportToString(String regularExpression) {
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
                Set<String> keys = linkSupportMap.keySet();
                for(String key: keys) {
                    DBDLinkSupport dbdLinkSupport = linkSupportMap.get(key);
                    String name = dbdLinkSupport.getSupportName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdLinkSupport.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }
    static private class MenuInstance implements DBDMenu
    {

        private String menuName;
        private String[] choices;

        private MenuInstance(String menuName, String[] choices)
        {
            this.menuName = menuName;
            this.choices = choices;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#getChoices()
         */
        public String[] getChoices() {
            return choices;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#getName()
         */
        public String getMenuName() {
            return menuName;
        }        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            FieldBase.newLine(builder,indentLevel);
            builder.append(String.format("menu %s { ",menuName));
            for(String value: choices) {
                builder.append(String.format("\"%s\" ",value));
            }
            builder.append("}");
            return builder.toString();
        }
    }
    
    static private class StructureInstance extends StructureBase implements DBDStructure
    {   
        private StructureInstance(String name,
            Field[] field,Property[] property,FieldAttribute fieldAttribute)
        {
            super(name,name,field,property,fieldAttribute);
        }
    }

    static private class RecordTypeInstance extends StructureInstance implements DBDRecordType
    {   
        private RecordTypeInstance(String name,
            Field[] field,Property[] property,FieldAttribute fieldAttribute)
        {
            super(name,field,property,fieldAttribute);
        }
    }
    
    static private class SupportInstance implements DBDSupport
    {
        private String supportName;
        private String factoryName;

        private SupportInstance(String supportName, String factoryName)
        {
            this.supportName = supportName;
            this.factoryName = factoryName;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDSupport#getSupportName()
         */
        public String getSupportName() {
            return supportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDSupport#getFactoryName()
         */
        public String getFactoryName() {
            return factoryName;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDSupport#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            FieldBase.newLine(builder,indentLevel);
            builder.append(String.format(
                    "supportName %s factoryName %s",
                    supportName,factoryName));
            return builder.toString();
        }
    }
    
    static private class LinkSupportInstance extends SupportInstance implements DBDLinkSupport {
        private String configurationStructureName;
        
        private LinkSupportInstance(String supportName, String factoryName,
                String configurationStructureName)
        {
            super(supportName,factoryName);
            this.configurationStructureName = configurationStructureName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDLinkSupport#getConfigurationStructureName()
         */
        public String getConfigurationStructureName() {
            return configurationStructureName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDSupport#toString(int)
         */
        public String toString(int indentLevel) {
            return super.getString(indentLevel) +
                " configurationStructureName " + configurationStructureName;
        }
    }
}
