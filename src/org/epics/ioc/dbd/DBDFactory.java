/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.epics.ioc.pv.BaseStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldAttribute;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;


/**
 * DBDFactory creates a Database Definition Database (DBD) and automatically creates the master DBD.
 * A DBD contains the description of Database Definitions:
 *  menu, structure, recordType, and support.
 * The masterDBD automatically creates a DBDStructure which has structureName = "null" and has 0 fields.
 * @author mrk
 * 
 */
public class DBDFactory {
    private static Convert convert = ConvertFactory.getConvert();
    
    private static DBDInstance masterDBD;
    
    static {
        masterDBD = new DBDInstance("master");
        FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        DBDStructure dbdStructure = masterDBD.createStructure(
            "null", new Field[0], fieldCreate.createFieldAttribute());
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
        private TreeMap<String,DBDStructure> structureMap = new TreeMap<String,DBDStructure>();
        private TreeMap<String,DBDCreate> createMap = new TreeMap<String,DBDCreate>();
        private TreeMap<String,DBDSupport> supportMap = new TreeMap<String,DBDSupport>();
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
                masterDBD.merge(structureMap,createMap,supportMap);
                structureMap.clear();
                createMap.clear();
                supportMap.clear();
            } finally {
                rwLock.writeLock().unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createCreate(java.lang.String, java.lang.String)
         */
        public DBDCreate createCreate(String createName, String factoryName) {
            return new CreateInstance(createName,factoryName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createSupport(java.lang.String, java.lang.String)
         */
        public DBDSupport createSupport(String supportName, String factoryName) {
            return new SupportInstance(supportName,factoryName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#createStructure(java.lang.String, org.epics.ioc.pv.Field[], org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public DBDStructure createStructure(String name, Field[] field,FieldAttribute fieldAttribute)
        {
            return new StructureInstance(name,field,fieldAttribute);
        }
        
        // merge allows master to be locked once
        private void merge(
                TreeMap<String,DBDStructure> structure,
                TreeMap<String,DBDCreate> create,
                TreeMap<String,DBDSupport> support)
        {
            Set<String> keys;
            rwLock.writeLock().lock();
            try {
                keys = structure.keySet();
                for(String key: keys) {
                    structureMap.put(key,structure.get(key));
                }
                keys = create.keySet();
                for(String key: keys) {
                    createMap.put(key,create.get(key));
                }
                keys = support.keySet();
                for(String key: keys) {
                    supportMap.put(key,support.get(key));
                }
            } finally {
                rwLock.writeLock().unlock();
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
        public DBDStructure[] getDBDStructures() {
            rwLock.readLock().lock();
            try {
                DBDStructure[] array = new DBDStructure[structureMap.size()];
                structureMap.values().toArray(array);
                return array;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#addCreate(org.epics.ioc.dbd.DBDCreate)
         */
        public boolean addCreate(DBDCreate create) {
            rwLock.writeLock().lock();
            try {
                String key = create.getCreateName();
                if(createMap.containsKey(key)) return false;
                if(this!=masterDBD && masterDBD.getCreate(key)!=null) return false;
                createMap.put(key,create);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getCreate(java.lang.String)
         */
        public DBDCreate getCreate(String createName) {
            rwLock.readLock().lock();
            try {
                DBDCreate dbdCreate = null;
                dbdCreate = createMap.get(createName);
                if(dbdCreate==null && this!=masterDBD) dbdCreate = masterDBD.getCreate(createName);
                return dbdCreate;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBD#getCreateMap()
         */
        public DBDCreate[] getDBDCreates() {
            rwLock.readLock().lock();
            try {
                DBDCreate[] array = new DBDCreate[createMap.size()];
                createMap.values().toArray(array);
                return array;
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
        public DBDSupport[] getDBDSupports() {
            rwLock.readLock().lock();
            try {
                DBDSupport[] array = new DBDSupport[supportMap.size()];
                supportMap.values().toArray(array);
                return array;
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
         * @see org.epics.ioc.dbd.DBD#createList(java.lang.String)
         */
        public String[] createList(String regularExpression) {
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
                Set<String> keys = createMap.keySet();
                for(String key: keys) {
                    DBDCreate dbdCreate = createMap.get(key);
                    String name = dbdCreate.getCreateName();
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
         * @see org.epics.ioc.dbd.DBD#createToString(java.lang.String)
         */
        public String createToString(String regularExpression) {
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
                Set<String> keys = createMap.keySet();
                for(String key: keys) {
                    DBDCreate dbdCreate = createMap.get(key);
                    String name = dbdCreate.getCreateName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + dbdCreate.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }
    
    static private class StructureInstance extends BaseStructure implements DBDStructure
    {   
        private StructureInstance(String name,Field[] field,FieldAttribute fieldAttribute)
        {
            super(name,name,field,fieldAttribute);
        }
    }

    static private class CreateInstance implements DBDCreate
    {
        private String createName;
        private String factoryName;

        private CreateInstance(String createName, String factoryName)
        {
            this.createName = createName;
            this.factoryName = factoryName;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDCreate#getCreateName()
         */
        public String getCreateName() {
            return createName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDCreate#getFactoryName()
         */
        public String getFactoryName() {
            return factoryName;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbd.DBDCreate#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            convert.newLine(builder,indentLevel);
            builder.append(String.format(
                    "createName %s factoryName %s",
                    createName,factoryName));
            return builder.toString();
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
            convert.newLine(builder,indentLevel);
            builder.append(String.format(
                    "supportName %s factoryName %s",
                    supportName,factoryName));
            return builder.toString();
        }
    }
}

