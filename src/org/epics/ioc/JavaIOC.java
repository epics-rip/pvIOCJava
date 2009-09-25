/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.ioc.install.Install;
import org.epics.ioc.install.InstallFactory;
import org.epics.ioc.swtshell.SwtshellFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
/**
 * The main program to start a JavaIOC.
 * The program is started with a command line of
 * java org.epics.ioc.JavaIOC 
 * The command line options are:
 * <pre>
 *     -structures list
 *             list is a list of xml files containing structure definitions. Each is parsed and put into the master database
 *     -records list
 *             list is a list of xml files containing records definitions. Each is parsed and started and put into the master database 
 *     -dumpStructures
 *             Dump all structures in the master database
 *     -dumpRecords
 *             Dump all record instances in the master database
 *     -server serverFile
 *             JavaIOC the server specified in the serverFile
 *     -swtshell
 *             Starts the JavaIOC running under swtshell
 *            
 * @author mrk
 *
 */
public class JavaIOC {
    private enum State {
        structures,
        records,
        servers
    }

    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final Install install = InstallFactory.get();
    /**
     * read and dump a database instance file.
     * @param  args is a sequence of flags and filenames.
     */
    public static void main(String[] args) {
        if(args.length==1 && args[0].equals("?")) {
            usage();
            return;
        }
        boolean runSWTShell = false;
        Requester iocRequester = new Listener();
        int nextArg = 0;
        State state = null;
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
                } else if(arg.equals("structures")){
                    state = State.structures;
                } else if(arg.equals("records")){
                    state = State.records;
                } else if(arg.equals("swtshell")) {
                	runSWTShell = true;
                } else if(arg.equals("server")) {
                    state = State.servers;
                } else {
                    System.err.println("unknown arg: " + arg);
                    usage();
                    return;
                }
            } else if(state==State.structures){
                parseStructures(arg,iocRequester);
            } else if(state==State.records){
                parseRecords(arg,iocRequester);
            } else if(state==State.servers) {
                startServer(arg);
            } else {
                System.err.println("unknown arg: " + arg);
                usage();
                return;
            }
            
            if (runSWTShell)
                SwtshellFactory.swtshell();
        }
    }
    
    static void usage() {
        System.out.println("Usage:"
                + " -structures fileList"
                + " -records fileList"
                + " -dumpStructures"
                + " -dumpRecords"
                + " -server file"
                + " -swtshell ");
    }
    
    static void printError(String message) {
        System.err.println(message);
    }
    
    static void startServer(String fileName) {
        System.out.println("starting servers fileName " + fileName);
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String factoryName = null;
            while((factoryName = in.readLine()) !=null) {
                System.out.println("starting server factoryName " + factoryName);
                Class startClass;
                Method method = null;
                try {
                    startClass = Class.forName(factoryName);
                }catch (ClassNotFoundException e) {
                    printError("server factory "
                            + e.getLocalizedMessage()
                            + " class not found");
                    continue;
                }
                try {
                    method = startClass.getDeclaredMethod("start", (Class[])null);
                } catch (NoSuchMethodException e) {
                    printError("server factory "
                            + e.getLocalizedMessage()
                            + " method start not found");
                    continue;
                }
                if(!Modifier.isStatic(method.getModifiers())) {
                    printError("server factory "
                            + factoryName
                            + " start is not a static method ");
                    continue;
                }
                try {
                    method.invoke(null, new Object[0]);
                } catch(IllegalAccessException e) {
                    printError("server start IllegalAccessException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(IllegalArgumentException e) {
                    printError("server start IllegalArgumentException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(InvocationTargetException e) {
                    printError("server start InvocationTargetException "
                            + e.getLocalizedMessage());
                    continue;
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
        if(pvRecords.length>0) System.out.printf("\n\nrecords");
        for(PVRecord pvRecord : pvRecords) {
            System.out.print(pvRecord.toString());
        }
    }

    static void parseStructures(String fileName,Requester iocRequester) {
        System.out.printf("\nparsing PV file %s\n",fileName);
        try {
            install.installStructures(fileName,iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
    }
    
    static void parseRecords(String fileName,Requester iocRequester) {
        System.out.println("Starting local channel Access");
        org.epics.ca.LocalFactory.start();
        System.out.printf("\nparsing PV file %s\n",fileName);
        try {
            install.installRecords(fileName,iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
    }
     
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "javaIOC";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
