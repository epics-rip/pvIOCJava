/**
 * 
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.Type;

/**
 * Factory for creating a DBDAttribute interface.
 * @author mrk
 *
 */
public class DBDAttributeFactory {
    /**
     * Create a DBDAttribute interface
     * @param dbd The database holding any needed menus or structures.
     * @param attributes an interface that supplies the attribute information.
     * A SAX application can implement DBDAttributeValues by just calling
     * SAX Attribute methods.
     * @return a DBDAttribute interface.
     * @throws <i>IllegalArgumentException</i> if a required attribute is missing.
     * The attributes are supplied by the DBDAttributeValues interface.
     * <i>name</i> and <i>type</i> must be available.
     * <i>type</i> must be one of:
     * <i>boolean byte short int long float double enum
     * menu array structure link</i>.
     * If <i>type</i> is <i>menu</i> then <i>MenuName</i> must be defined.
     * If <i>type</i> is <i>structure</i> then <i>structureName</i>
     * must be defined and that structure must exist
     * in the <i>dbd</i> database.
     * If <i>type</i> is <i>array</i> then <i>elementType</i>
     * must be defined as a valid type.
     * If <i>type</i> is <link> then structure named link must
     * exist in the <i>dbd</i> database.
     */
    static public DBDAttribute create(DBD dbd, DBDAttributeValues attributes) {
        return new Attribute(dbd, attributes);
    }
    
    private static class Attribute implements DBDAttribute {
        
        public String getName() {
            return name;
        }

        public int getAsl() {
            return asl;
        }
        
        public DBDMenu getDBDMenu() {
            return dbdMenu;
        }

        public DBDStructure getDBDStructure() {
            return dbdStructure;
        }

        public DBType getDBType() {
            return dbType;
        }

        public String getDefault() {
            return defaultValue;
        }

        public DBType getElementDBType() {
            return elementDBType;
        }

        public Type getElementType() {
            return elementType;
        }
        
        public Type getType() {
            return type;
        }

        public boolean isDesign() {
            return isDesign;
        }

        public boolean isLink() {
            return isLink;
        }

        public boolean isReadOnly() {
            return isReadOnly;
        }
        
        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format("DBType %s ",dbType.toString()));
            if(defaultValue!=null) {
                builder.append(String.format("default \"%s\"",defaultValue));
            }
            builder.append(String.format(
                    " asl %d design %b link %b readOnly %b",
                    asl,isDesign,isLink,isReadOnly));
            if(dbType==DBType.dbMenu) {
                if(dbdMenu!=null)
                    builder.append(String.format(
                        " menuName %s",dbdMenu.getName()));
            } else if(dbType==DBType.dbStructure) {
                if(dbdStructure!=null)
                    builder.append(String.format(
                        " structureName %s",dbdStructure.getName()));
            } else if(dbType==DBType.dbArray) {
                builder.append(String.format(
                    " elementType %s",elementDBType.toString()));
            }
            return builder.toString();
        }
        
        Attribute(DBD dbd,DBDAttributeValues attributes) {
            int number = attributes.getLength();
            Types types = new Types();
            for(int i=0; i< number; i++) {
                String name = attributes.getName(i);
                String value = attributes.getValue(i);
                if(name.equals("name")) {
                    this.name = value;
                    continue;
                }
                if(name.equals("type")) {
                    getType(value,types);
                    this.type = types.type;
                    if(type==Type.pvUnknown)
                        throw new IllegalStateException(
                            "not a valid type");
                    dbType = types.dbType;
                    if(dbType==DBType.dbLink) {
                        dbdStructure = dbd.getDBDStructure("link");
                        if(dbdStructure==null)
                            throw new IllegalStateException(
                                    "structure link not in database");
                    }
                    continue;
                }
                if(name.equals("default")) {
                    defaultValue = value;
                    continue;
                }
                if(name.equals("asl")) {
                    asl = Integer.parseInt(value);
                    continue;
                }
                if(name.equals("design")) {
                    isDesign = Boolean.getBoolean(value);
                    continue;
                }
                if(name.equals("readonly")) {
                    isReadOnly = Boolean.getBoolean(value);
                    continue;
                }
                if(name.equals("link")) {
                    isLink = Boolean.getBoolean(value);
                    continue;
                }
                if(name.equals("menuName")) {
                    dbdMenu = dbd.getMenu(value);
                    continue;
                }
                if(name.equals("structureName")) {
                    dbdStructure = dbd.getDBDStructure(value);
                    continue;
                }
                if(name.equals("elementType")) {
                    getType(value,types);
                    elementType = types.type;
                    if(elementType==Type.pvUnknown)
                        throw new IllegalStateException(
                            "elementType is not a valid type");
                    elementDBType = types.dbType;
                    continue;
                }
            }
            if(name==null || name.length()==0)
                throw new IllegalStateException(
                    "name not specified");
            if(type==Type.pvUnknown)
                throw new IllegalStateException(
                    "type incorrectly specified");
            if(dbType==DBType.dbMenu && dbdMenu==null)
                throw new IllegalStateException(
                    "menuName not specified or not in database");
            if(dbType==DBType.dbStructure && dbdStructure==null)
                throw new IllegalStateException(
                    "structureName not specified or not in database");
            if(dbType==DBType.dbArray && elementType==Type.pvUnknown)
                throw new IllegalStateException(
                    "elementType not specified");
        }
        
        private int asl = 1;
        private String name = null;
        private DBDMenu dbdMenu = null;
        private DBDStructure dbdStructure = null;
        private DBType dbType = DBType.dbPvType;
        private Type type = Type.pvUnknown;
        private String defaultValue = null;
        private DBType elementDBType = DBType.dbPvType;
        private Type elementType = Type.pvUnknown;
        boolean isDesign = true;
        boolean isReadOnly = false;
        boolean isLink = false;
        
        private class Types {
            Type type;
            DBType dbType;
        }
        
        private static void getType(String value,Types types) {
            if(value==null) {
                types.type = Type.pvUnknown;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("boolean")) {
                types.type = Type.pvBoolean;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("byte")) {
                types.type = Type.pvByte;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("short")) {
                types.type = Type.pvShort;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("int")) {
                types.type = Type.pvInt;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("long")) {
                types.type = Type.pvLong;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("float")) {
                types.type = Type.pvFloat;
                types.dbType = DBType.dbPvType;
                return;
            }if(value.equals("double")) {
                types.type = Type.pvDouble;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("string")) {
                types.type = Type.pvString;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("enum")) {
                types.type = Type.pvEnum;
                types.dbType = DBType.dbPvType;
                return;
            }
            if(value.equals("menu")) {
                types.type = Type.pvEnum;
                types.dbType = DBType.dbMenu;
                return;
            }
            if(value.equals("structure")) {
                types.type = Type.pvStructure;
                types.dbType = DBType.dbStructure;
                return;
            }
            if(value.equals("array")) {
                types.type = Type.pvArray;
                types.dbType = DBType.dbArray;
                return;
            }
            if(value.equals("link")) {
                types.type = Type.pvStructure;
                types.dbType = DBType.dbLink;
                return;
            }
            types.type = Type.pvUnknown;
            types.dbType = DBType.dbPvType;
        }
        
        static private void newLine(StringBuilder builder, int indentLevel) {
            builder.append("\n");
            for (int i=0; i < indentLevel; i++) builder.append(indentString);
        }
        static private String indentString = "    ";
        
    }
}
