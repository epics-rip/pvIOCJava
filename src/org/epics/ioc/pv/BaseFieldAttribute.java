package org.epics.ioc.pv;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BaseFieldAttribute implements FieldAttribute {
    private TreeMap<String,String> attributeMap = new TreeMap<String,String>();
    
    /**
     * Constructor for BaseFieldAttribute.
     */
    public  BaseFieldAttribute(){}

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.FieldAttribute#getAttribute(java.lang.String)
     */
    public synchronized String getAttribute(String key) {
        return attributeMap.get(key);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.FieldAttribute#getAttributes()
     */
    public synchronized Map<String, String> getAttributes() {
        return attributeMap;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.FieldAttribute#setAttribute(java.lang.String, java.lang.String)
     */
    public synchronized String setAttribute(String key, String value) {
        return attributeMap.put(key, value);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.FieldAttribute#setAttributes(java.util.Map, java.lang.String[])
     */
    public synchronized void setAttributes(Map<String, String> attributes, String[] exclude) {
        Set<String> keys;
        keys = attributes.keySet();
        outer:
        for(String key: keys) {
             for(String excludeKey: exclude) {
                 if(excludeKey.equals(key)) continue outer;
             }
            attributeMap.put(key,attributes.get(key));
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.FieldAttribute#toString(int)
     */
    public synchronized String toString(int indentLevel) {
        String result = "";
        Set<String> keys = attributeMap.keySet();
        for(String key : keys) {
            result += " " + key + "=" + attributeMap.get(key);
        }
        return result;
    }
}