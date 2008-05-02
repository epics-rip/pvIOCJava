/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDCreate;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDRecordType;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.process.SupportCreation;
import org.epics.ioc.process.SupportCreationFactory;
import org.epics.ioc.swtshell.SwtshellFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * read and dump a Database Definition and Record Instance Files.
 * @author mrk
 *
 */

public class XMLToDatabase {
    private enum State {
        dbdFile,
        dbFile,
        servers
    }

    /**
     * read and dump a database instance file.
     * @param  args is a sequence of flags and filenames.
     */
    public static void main(String[] args) {
        if(args.length==1 && args[0].equals("?")) {
            usage();
            return;
        }
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdb = IOCDBFactory.create("master");
        Requester iocRequester = new Listener();
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
                } else if(arg.equals("swtshell")) {
                    SwtshellFactory.swtshell();
                } else if(arg.equals("server")) {
                    state = State.servers;
                } else if(arg.equals("startIOC")) {
                    SupportCreate supportCreate = new SupportCreate();
                    if(!supportCreate.create()) {
                        System.out.println("support create failed");
                        return;
                    }
                } else {
                    System.err.println("unknown arg: " + arg);
                    usage();
                    return;
                }
            } else if(state==State.dbdFile) {
                parseDBD(dbd,arg,iocRequester);
            } else if(state==State.dbFile){
                parseDB(dbd,iocdb,arg,iocRequester);
            } else if(state==State.servers) {
                startServer(arg);
            } else {
                System.err.println("unknown arg: " + arg);
                usage();
                return;
            }
        }
    }
    
    static void usage() {
        System.out.println("Usage: -dbd DatabaseDefinitionList"
                + " -db InstanceList"
                + " -dumpDBD -dumpDB"
                + " -startIOC"
                + " -server file"
                + " -swtshell ");
    }
    
    static void printError(String message) {
        System.err.println(message);
    }
    
    static void startServer(String fileName) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String factoryName = null;
            while((factoryName = in.readLine()) !=null) {
                Class startClass;
                Method method = null;
                try {
                    startClass = Class.forName(factoryName);
                }catch (ClassNotFoundException e) {
                    printError("server factory "
                            + e.getLocalizedMessage()
                            + " class not found");
                    return;
                }
                try {
                    method = startClass.getDeclaredMethod("start", (Class[])null);
                } catch (NoSuchMethodException e) {
                    printError("server factory "
                            + e.getLocalizedMessage()
                            + " method start not found");
                    return;
                }
                if(!Modifier.isStatic(method.getModifiers())) {
                    printError("server factory "
                            + factoryName
                            + " start is not a static method ");
                    return;
                }
                try {
                    method.invoke(null, new Object[0]);
                } catch(IllegalAccessException e) {
                    printError("server factory "
                            + e.getLocalizedMessage());
                    return;
                } catch(IllegalArgumentException e) {
                    printError("server factory "
                            + e.getLocalizedMessage());
                    return;
                } catch(InvocationTargetException e) {
                    printError("server factory "
                            + e.getLocalizedMessage());
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("startServer error " + e.getMessage());
            return;
        }
    }
        
    static void dumpDBD(DBD dbd) {
        Set<String> keys = null;
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
            System.out.printf("\n\nsupport");
            for(String key: keys) {
                DBDSupport dbdSupport = supportMap.get(key);
                System.out.print(dbdSupport.toString());
            }
        }
        
        Map<String,DBDCreate> createMap = dbd.getCreateMap();
        keys = createMap.keySet();
        if(keys.size()>0) {
            System.out.printf("\n\ncreate");
            for(String key: keys) {
                DBDCreate dbdCreate = createMap.get(key);
                System.out.print(dbdCreate.toString());
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
        
    static void parseDBD(DBD dbd, String fileName,Requester iocRequester) {
        System.out.printf("\nparsing DBD file %s\n",fileName);
        try {
            XMLToDBDFactory.convert(dbd,fileName,iocRequester);
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
    }

    static void parseDB(DBD dbd, IOCDB iocdb,String fileName,Requester iocRequester) {
        System.out.printf("\nparsing DB file %s\n",fileName);
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,fileName,iocRequester);
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
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "XMLTODatabase";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
    
    private static class SupportCreate implements Requester{
        
        private SupportCreate() {}
        
        private void message(String message) {
            System.out.println(message);
        }
        private boolean create() {
            IOCDB iocdb = IOCDBFactory.getMaster();
            SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(iocdb, this);
            boolean gotSupport = supportCreation.createSupport();
            if(!gotSupport) {
                message("Did not find all support.");
                return false;
            }
            boolean readyForStart = supportCreation.initializeSupport();
            if(!readyForStart) {
                message("initializeSupport failed");
                return false;
            }
            boolean ready = supportCreation.startSupport();
            if(!ready) {
                message("startSupport failed");
                return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "swtshell";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("swtshell " + message);
        }
    }
}
