import org.epics.ioc.dbAccess.DBRecord;
import org.epics.ioc.dbAccess.IOCDB;
import org.epics.ioc.dbAccess.IOCDBFactory;
import org.epics.ioc.dbAccess.XMLToIOCDBFactory;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.util.IOCMessageListener;
import org.epics.ioc.util.IOCMessageType;

import java.util.*;

/**
 * read and dump a Database Definition and Record Instance Files.
 * @author mrk
 *
 */
enum State {
    dbdFile,
    dbFile,
}
public class XMLToDatabase {

    /**
     * read and dump a database instance file.
     * @param  args is a sequence of flags and filenames.
     * - dbd  Following fileNames are database definition files
     * - db Following fileNames are record instance files.
     * - dumpDBD Dump all database definitions given so far
     * - dumpDB Dump all record instances given so far.
     * 
     */
    public static void main(String[] args) {
        if(args.length==0 || args[0].equals("?")) {
            System.out.printf("-dbd DatabaseDefinitionList"
                    + " -db InstanceList -dumpDBD -dumpDB  ...\n");
            return;
        }
        DBD dbd = DBDFactory.create("master",null);
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        IOCMessageListener iocMessageListener = new Listener();
        int nextArg = 0;
        State state = State.dbdFile;
        while(nextArg<args.length) {
            String arg = args[nextArg++];
            if(arg.charAt(0) == '-') {
                if(arg.length()>1) {
                    arg = arg.substring(1);
                } else {
                    if(nextArg>=args.length) {
                        System.out.printf("last arg is - illegal\n");
                        return;
                    }
                    arg = args[nextArg++];
                }
                if(arg.equals("dumpDBD")) {
                    dumpDBD(dbd);
                } else if(arg.equals("dumpDB")) {
                    dumpDB(dbd,iocdb);
                } else if(arg.equals("dbd")) {
                    state = State.dbdFile;
                } else if(arg.equals("db")){
                    state = State.dbFile;
                } else {
                    System.out.printf("arg %d %s not understood\n",nextArg,arg);
                }
            } else if(state==State.dbdFile) {
                parseDBD(dbd,arg,iocMessageListener);
            } else {
                parseDB(dbd,iocdb,arg,iocMessageListener);
            }
        }
    }
        
    static void dumpDBD(DBD dbd) {
        Map<String,DBDMenu> menuMap = dbd.getMenuMap();
        Set<String> keys = menuMap.keySet();
        if(keys.size()>0) {
            System.out.printf("\n\nmenus");
            for(String key: keys) {
                DBDMenu dbdMenu = menuMap.get(key);
                System.out.print(dbdMenu.toString());
            }
        }
        Map<String,DBDStructure> structureMap = dbd.getStructureMap();
        keys = structureMap.keySet();
        if(keys.size()>0) {
            System.out.printf("\n\nstructures");
            for(String key: keys) {
                DBDStructure dbdStructure = structureMap.get(key);
                System.out.print(dbdStructure.toString());
            }
        }
        
        Map<String,DBDSupport> supportMap = dbd.getSupportMap();
        keys = supportMap.keySet();
        if(keys.size()>0) {
            System.out.printf("\n\nlinkSupport");
            for(String key: keys) {
                DBDSupport dbdSupport = supportMap.get(key);
                System.out.print(dbdSupport.toString());
            }
        }
        
        
        Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
        keys = recordTypeMap.keySet();
        if(keys.size()>0) {
            System.out.printf("\n\nrecordTypes");
            for(String key: keys) {
                DBDRecordType dbdRecordType = recordTypeMap.get(key);
                System.out.print(dbdRecordType.toString());
            }
        }           
    }
        
    static void parseDBD(DBD dbd, String fileName,IOCMessageListener iocMessageListener) {
        System.out.printf("\nparsing DBD file %s\n",fileName);
        try {
            XMLToDBDFactory.convert(dbd,fileName,iocMessageListener);
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
    }

    static void parseDB(DBD dbd, IOCDB iocdb,String fileName,IOCMessageListener iocMessageListener) {
        System.out.printf("\nparsing DB file %s\n",fileName);
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,fileName,iocMessageListener);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
    }
    
    static void dumpDB(DBD dbd, IOCDB iocdb) {
        System.out.printf("\nrecords\n");
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> keys = recordMap.keySet();
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }
    
    private static class Listener implements IOCMessageListener {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCMessageListener#message(java.lang.String, org.epics.ioc.util.IOCMessageType)
         */
        public void message(String message, IOCMessageType messageType) {
            System.out.println(message);
            
        }
    }
}
