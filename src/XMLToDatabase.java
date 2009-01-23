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

import org.epics.ioc.swtshell.SwtshellFactory;
import org.epics.ioc.util.IOCFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;

/**
 * read and dump a Database Definition and Record Instance Files.
 * @author mrk
 *
 */

public class XMLToDatabase {
    private enum State {
        dbFile,
        servers
    }

    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    /**
     * read and dump a database instance file.
     * @param  args is a sequence of flags and filenames.
     */
    public static void main(String[] args) {
        if(args.length==1 && args[0].equals("?")) {
            usage();
            return;
        }
        Requester iocRequester = new Listener();
        int nextArg = 0;
        State state = State.dbFile;
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
                if(arg.equals("dumpStructures")) {
                    dumpStructures();
                } else if(arg.equals("dumpRecords")) {
                    dumpRecords();
                } else if(arg.equals("pv")){
                    state = State.dbFile;
                } else if(arg.equals("swtshell")) {
                    SwtshellFactory.swtshell();
                } else if(arg.equals("server")) {
                    state = State.servers;
                } else {
                    System.err.println("unknown arg: " + arg);
                    usage();
                    return;
                }
            } else if(state==State.dbFile){
                parseDB(arg,iocRequester);
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
        System.out.println("Usage:"
                + " -pv pvList"
                + " -dumpStructures"
                + " -dumpRecords"
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
        
    static void dumpStructures() {
        PVStructure[] pvStructures = masterPVDatabase.getStructures();
        if(pvStructures.length>0) System.out.printf("\n\nstructures");
        for(PVStructure pvStructure : pvStructures) {
            System.out.print(pvStructure.toString());
        }
    }
    
    static void dumpRecords() {
        PVRecord[] pvRecords = masterPVDatabase.getRecords();
        if(pvRecords.length>0) System.out.printf("\n\nstructures");
        for(PVRecord pvRecord : pvRecords) {
            System.out.print(pvRecord.toString());
        }
    }

    static void parseDB(String fileName,Requester iocRequester) {
        System.out.printf("\nparsing PV file %s\n",fileName);
        try {
            IOCFactory.initDatabase(fileName,iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
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
}
