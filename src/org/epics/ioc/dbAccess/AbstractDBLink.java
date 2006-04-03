/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for DBLink
 * @author mrk
 *
 */
public abstract class AbstractDBLink extends AbstractDBStructure implements DBLink
{

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBLink#getConfigStructureFieldName()
     */
    public String getConfigStructureFieldName() {
        return pvConfigStructFieldName.get();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBLink#putConfigStructureFieldName(java.lang.String)
     */
    public void putConfigStructureFieldName(String name) {
        pvConfigStructFieldName.put(name);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBLink#getLinkSupportName()
     */
    public String getLinkSupportName() {
        return pvLinkSupportName.get();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBLink#putLinkSupportName(java.lang.String)
     */
    public void putLinkSupportName(String name) {
        pvLinkSupportName.put(name);
    }

    /**
     * Constructor
     * @param dbdLinkField
     */
    AbstractDBLink(DBDLinkField dbdLinkField)
    {
        super(dbdLinkField);
        assert(super.pvData.length==2);
        PVData config = super.pvData[0];
        Field field = config.getField();
        assert(field.getType()==Type.pvString);
        assert(field.getName().equals("configStructFieldName"));
        pvConfigStructFieldName = (PVString)config;
        PVData linkSupport = super.pvData[1];
        field = linkSupport.getField();
        assert(field.getType()==Type.pvString);
        assert(field.getName().equals("linkSupportName"));
        pvLinkSupportName = (PVString)linkSupport;
    }

    /**
     * PVString for the field name that has the configuration information
     */
    protected PVString pvConfigStructFieldName;
    /**
     * PVString for the link support name
     */
    protected PVString pvLinkSupportName;
}
