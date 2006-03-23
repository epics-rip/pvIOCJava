package org.epics.ioc.dbDefinition;

/**
 * Accesses Field Attributes
 * @author mrk
 *
 */
public interface DBDAttribute {
    /**
     * get the defauly value for the field as a String
     * @return the default
     */
    String getDefault();
    /**
     * specify the default value
     * @param value the default
     */
    void setDefault(String value);
    /**
     * is the field readonly 
     * @return (false,true) if it (is not, is) readonly
     */
    boolean isReadonly();
    /**
     * specify if the field is readonly
     * @param value (false,true) if it (is not, is) readonly
     */
    void setReadOnly(boolean value);
    /**
     * can this field be configurable by Database Configuration Tools
     * @return (false,true) if it (can't, can) be configured
     */
    boolean isDesign();
    /**
     * specify if this field be configurable by Database Configuration Tools
     * @param value (false,true) if it (can't, can) be configured
     */
    void setDesign(boolean value);
    /**
     * Is this field is a link to another record
     * @return (false,true) if it (is not, is) a link to another record
     */
    boolean isLink();
    /**
     *  specify if this field is a link to another record
     * @param value (false,true) if it (is not, is) a link to another record
     */
    void setLink(boolean value);
    /**
     * get the Access Security Level for this field
     * @return the level
     */
    int getAsl();
    /**
     * set the Access Security Level for this field
     * @param value the level
     */
    void setAsl(int value);
}
