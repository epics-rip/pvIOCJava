/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDCreate;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScannerFactory;
import org.epics.ioc.util.ThreadCreateFactory;

/**
 * A shell for introspecting a JavaIOC Database.
 * The menubar at the top of the display provides access to the DBD (Database Definition Database).
 * The recordName row provides access to record instances. It provides two controls: a select
 * button and a text entry widget.
 * A record name can be entered in the text window followed by the enter key.
 * If the select button is pressed, a list of all record instances is displayed.
 * The user can select a name.
 * @author mrk
 *
 */
public class IntrospectDatabaseFactory {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    
    /**
     * A shell for introspecting the local IOC database.
     * @param display The display.
     */
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    
    private static class Introspect implements SelectionListener, Requester{
        private static DBD dbd = DBDFactory.getMasterDBD();
        private static final String newLine = String.format("%n");
        private Display display;
        private Shell shell;
        private Button selectButton;
        private Button dumpButton;
        private Button showStateButton;
        private Button setTraceButton;
        private Button setEnableButton;
        private Button setSupportStateButton;
        private Button releaseProcessorButton;
        private Button showBadRecordsButton;
        private Button showThreadsButton;
        private Button clearButton;
        private Text consoleText;
        private SelectLocalRecord selectLocalRecord;
        
        private DBRecord dbRecord = null;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText("introspectDatabase");
            GridLayout layout = new GridLayout();
            layout.numColumns = 1;
            shell.setLayout(layout);
            Menu menuBar = new Menu(shell,SWT.BAR);
            shell.setMenuBar(menuBar);
            MenuItem dbdStructureMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdStructureMenu.setText("structure");
            new StructureDBD(dbdStructureMenu);
            MenuItem dbdCreateMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdCreateMenu.setText("create");
            new CreateDBD(dbdCreateMenu);
            MenuItem dbdSupportMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdSupportMenu.setText("support");
            new SupportDBD(dbdSupportMenu);
            Composite composite = new Composite(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 8;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("record");
            selectButton = new Button(composite,SWT.PUSH);
            selectButton.setText("select");
            selectLocalRecord = SelectLocalRecordFactory.create(shell,this);
            selectButton.addSelectionListener(this);
            dumpButton = new Button(composite,SWT.PUSH);
            dumpButton.setText("dump");
            dumpButton.setEnabled(false);
            dumpButton.addSelectionListener(this);
            showStateButton = new Button(composite,SWT.PUSH);
            showStateButton.setText("showState");
            showStateButton.setEnabled(false);
            showStateButton.addSelectionListener(this);
            setTraceButton = new Button(composite,SWT.PUSH);
            setTraceButton.setText("setTrace");
            setTraceButton.setEnabled(false);
            setTraceButton.addSelectionListener(this);
            setEnableButton = new Button(composite,SWT.PUSH);
            setEnableButton.setText("setEnable");
            setEnableButton.setEnabled(false);
            setEnableButton.addSelectionListener(this);
            setSupportStateButton = new Button(composite,SWT.PUSH);
            setSupportStateButton.setText("setSupportState");
            setSupportStateButton.setEnabled(false);
            setSupportStateButton.addSelectionListener(this);
            releaseProcessorButton = new Button(composite,SWT.PUSH);
            releaseProcessorButton.setText("releaseProcessor");
            releaseProcessorButton.setEnabled(false);
            releaseProcessorButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 2;
            composite.setLayout(layout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            showBadRecordsButton = new Button(composite,SWT.PUSH);
            showBadRecordsButton.setText("&showBadRecords");
            showBadRecordsButton.addSelectionListener(this);
            showThreadsButton = new Button(composite,SWT.PUSH);
            showThreadsButton.setText("&showThreads");
            showThreadsButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 1;
            composite.setLayout(layout);
            gridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(gridData);
            clearButton = new Button(composite,SWT.PUSH);
            clearButton.setText("&Clear");
            clearButton.addSelectionListener(this);
            consoleText = new Text(composite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            consoleText.setLayoutData(gridData);
            shell.open();
        }       
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            Object object = e.getSource();
            if(object==selectButton) {
                dbRecord = selectLocalRecord.getDBRecord();
                if(dbRecord==null) {
                    consoleText.append("record not found"+ String.format("%n"));
                    setButtonsEnabled(false);
                } else {
                    consoleText.append(dbRecord.getPVRecord().getRecordName() + String.format("%n"));
                    setButtonsEnabled(true);
                }
                return;
            }
            if(object==dumpButton) {
                consoleText.append(dbRecord.getPVRecord().toString());
                return;
            }
            if(object==showStateButton) {
                RecordProcess recordProcess = dbRecord.getRecordProcess();
                boolean processSelf = recordProcess.canProcessSelf();
                String processRequesterName = recordProcess.getRecordProcessRequesterName();
                SupportState supportState = recordProcess.getSupportState();
                boolean isActive = recordProcess.isActive();
                boolean isEnabled = recordProcess.isEnabled();
                boolean isTrace = recordProcess.isTrace();
                PVRecord pvRecord = dbRecord.getPVRecord();
                String alarmSeverity = null;
                PVField pvField = pvProperty.findProperty(pvRecord,"alarm.severity.choice");
                if(pvField!=null) alarmSeverity = pvField.toString();
                String alarmMessage = null;
                pvField = pvProperty.findProperty(pvRecord,"alarm.message");
                if(pvField!=null) alarmMessage = pvField.toString();
                consoleText.append(dbRecord.getPVRecord().getRecordName() + newLine);
                consoleText.append(
                    "  processSelf " + processSelf + " processRequester " + processRequesterName
                    + " supportState " + supportState.name() + newLine);
                consoleText.append(
                    "  isActive " + isActive + " isEnabled " + isEnabled + " isTrace " + isTrace + newLine);
                consoleText.append(
                    "  alarmSeverity " + alarmSeverity + " alarmMessage " + alarmMessage + newLine);
                return;
            }
            if(object==setTraceButton) {
                SetTrace set = new SetTrace(shell);
                set.set();
                return;
            }
            if(object==setEnableButton) {
                SetEnable set = new SetEnable(shell);
                set.set();
                return;
            }
            if(object==setSupportStateButton) {
                SetSupportState set = new SetSupportState(shell);
                set.set();
                return;
            }
            if(object==releaseProcessorButton) {
                RecordProcess recordProcess = dbRecord.getRecordProcess();
                if(recordProcess==null) {
                    message("recordProcess is null", MessageType.error);
                    return;
                }
                MessageBox mb = new MessageBox(
                        shell,SWT.ICON_WARNING|SWT.YES|SWT.NO);
                mb.setMessage("VERY DANGEROUS. DO YOU WANT TO PROCEED?");
                int rc = mb.open();
                if(rc==SWT.YES) {
                    recordProcess.releaseRecordProcessRequester();
                }
                return;
            }
            if(object==showBadRecordsButton) {
                DBRecord[] dbRecords = iocdb.getDBRecords();
                for(DBRecord dbRecord : dbRecords) {
                    RecordProcess recordProcess = dbRecord.getRecordProcess();
                    boolean isActive = recordProcess.isActive();
                    boolean isEnabled = recordProcess.isEnabled();
                    SupportState supportState = recordProcess.getSupportState();
                    PVRecord pvRecord = dbRecord.getPVRecord();
                    String alarmSeverity = null;
                    PVField pvField = pvProperty.findProperty(pvRecord,"alarm.severity.choice");
                    if(pvField!=null) alarmSeverity = pvField.toString();
                    String alarmMessage = null;
                    pvField = pvProperty.findProperty(pvRecord,"alarm.message");
                    if(pvField!=null) alarmMessage = pvField.toString();
                    String status = "";
                    if(isActive) status += " isActive";
                    if(!isEnabled) status += " disabled";
                    if(supportState!=SupportState.ready) status += " supportState " + supportState.name();
                    if(alarmSeverity!=null && !alarmSeverity.equals("none")) status += " alarmSeverity " + alarmSeverity;
                    if(alarmMessage!=null && !alarmMessage.equals("null") && alarmMessage.length()>0)
                        status += " alarmMessage " + alarmMessage;
                    if(status!="") {
                        status = pvRecord.getRecordName() + " " + status + newLine;
                        consoleText.append(status);
                    }
                }
                return;
            }
            if(object==showThreadsButton) {
                Thread[] threads = ThreadCreateFactory.getThreadCreate().getThreads();
                for(Thread thread : threads) {
                    String name = thread.getName();
                    int priority = thread.getPriority();
                    consoleText.append(name + " priority " + priority + newLine);
                }
                PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
                EventScanner eventScanner = ScannerFactory.getEventScanner();
                consoleText.append(periodicScanner.toString());
                consoleText.append(eventScanner.toString());
                return;
            }
            if(object==clearButton) {
                consoleText.selectAll();
                consoleText.clearSelection();
                consoleText.setText("");
                return;
            }
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "introspectDatabase";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            consoleText.setText(message);
        }
        
        private void setButtonsEnabled(boolean value) {
            dumpButton.setEnabled(value);
            showStateButton.setEnabled(value);
            setTraceButton.setEnabled(value);
            setEnableButton.setEnabled(value);
            setSupportStateButton.setEnabled(value);
            releaseProcessorButton.setEnabled(value);
        }
        
        private class SetEnable extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button falseButton;
            private Button trueButton;
            RecordProcess recordProcess;

            private SetEnable(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = dbRecord.getRecordProcess();
                boolean initialState = recordProcess.isEnabled();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                falseButton = new Button(composite,SWT.RADIO);
                falseButton.setText("disable");
                falseButton.setSelection(initialState ? false : true);
                trueButton = new Button(composite,SWT.RADIO);
                trueButton.setText("enable");
                trueButton.setSelection(initialState ? true : false);
                falseButton.addSelectionListener(this);
                trueButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==falseButton) {
                    recordProcess.setEnabled(false);
                }
                if(object==trueButton) {
                    recordProcess.setEnabled(true);
                }
            }
            
        }
        private class SetTrace extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button falseButton;
            private Button trueButton;
            RecordProcess recordProcess;

            private SetTrace(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = dbRecord.getRecordProcess();
                boolean initialState = recordProcess.isTrace();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                falseButton = new Button(composite,SWT.RADIO);
                falseButton.setText("traceOff");
                falseButton.setSelection(initialState ? false : true);
                trueButton = new Button(composite,SWT.RADIO);
                trueButton.setText("traceOn");
                trueButton.setSelection(initialState ? true : false);
                falseButton.addSelectionListener(this);
                trueButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==falseButton) {
                    recordProcess.setTrace(false);
                }
                if(object==trueButton) {
                    recordProcess.setTrace(true);
                }
            }
            
        }
        
        private class SetSupportState extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button initializeButton;
            private Button startButton;
            private Button stopButton;
            private Button uninitializeButton;
            RecordProcess recordProcess;

            private SetSupportState(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = dbRecord.getRecordProcess();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                initializeButton = new Button(composite,SWT.PUSH);
                initializeButton.setText("initialize");
                startButton = new Button(composite,SWT.PUSH);
                startButton.setText("start");
                stopButton = new Button(composite,SWT.PUSH);
                stopButton.setText("stop");
                uninitializeButton = new Button(composite,SWT.PUSH);
                uninitializeButton.setText("uninitialize");
                initializeButton.addSelectionListener(this);
                startButton.addSelectionListener(this);
                stopButton.addSelectionListener(this);
                uninitializeButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==initializeButton) {
                    recordProcess.initialize();
                }
                if(object==startButton) {
                    recordProcess.start();
                }
                if(object==stopButton) {
                    recordProcess.stop();
                }
                if(object==uninitializeButton) {
                    recordProcess.uninitialize();
                }
            }
            
        }
        private class StructureDBD implements SelectionListener {
            
            private StructureDBD(MenuItem menuItem) {
                Menu menuStructure = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuStructure);
                MenuItem choiceAll = new MenuItem(menuStructure,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                DBDStructure[] dbdStructures = dbd.getDBDStructures();
                for(DBDStructure dbdStructure : dbdStructures) {
                    String name = dbdStructure.getStructureName();
                    MenuItem choiceItem = new MenuItem(menuStructure,SWT.PUSH);
                    choiceItem.setText(name);
                    choiceItem.addSelectionListener(this);
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                MenuItem choice = (MenuItem)arg0.getSource();
                String name = choice.getText();
                if(!name.equals("all")) {
                    DBDStructure value = dbd.getStructure(name);
                    consoleText.append(value.toString());
                    return;
                }
                DBDStructure[] dbdStructures = dbd.getDBDStructures();
                for(DBDStructure dbdStructure : dbdStructures) {
                    consoleText.append(dbdStructure.toString());
                }
            }
            
        }
        
        private class CreateDBD implements SelectionListener {
            
            private CreateDBD(MenuItem menuItem) {
                Menu menuCreate = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuCreate);
                MenuItem choiceAll = new MenuItem(menuCreate,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                DBDCreate[] dbdCreates = dbd.getDBDCreates();
                for(DBDCreate dbdCreate : dbdCreates) {
                    String name = dbdCreate.getCreateName();
                    MenuItem choiceItem = new MenuItem(menuCreate,SWT.PUSH);
                    choiceItem.setText(name);
                    choiceItem.addSelectionListener(this);
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                MenuItem choice = (MenuItem)arg0.getSource();
                String name = choice.getText();
                if(!name.equals("all")) {
                    DBDCreate value = dbd.getCreate(name);
                    consoleText.append(value.toString());
                    return;
                }
                DBDCreate[] dbdCreates = dbd.getDBDCreates();
                for(DBDCreate dbdCreate: dbdCreates) {
                    consoleText.append(dbdCreate.toString());
                }
            }
        }
        
        
        private class SupportDBD implements SelectionListener {
            
            private SupportDBD(MenuItem menuItem) {
                Menu menuSupport = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuSupport);
                MenuItem choiceAll = new MenuItem(menuSupport,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                DBDSupport[] dbdSupports = dbd.getDBDSupports();
                for(DBDSupport dbdSupport : dbdSupports) {
                    String name = dbdSupport.getSupportName();
                    MenuItem choiceItem = new MenuItem(menuSupport,SWT.PUSH);
                    choiceItem.setText(name);
                    choiceItem.addSelectionListener(this);
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                MenuItem choice = (MenuItem)arg0.getSource();
                String key = choice.getText();
                if(!key.equals("all")) {
                    DBDSupport value = dbd.getSupport(key);
                    consoleText.append(value.toString());
                    return;
                }
                DBDSupport[] dbdSupports = dbd.getDBDSupports();
                for(DBDSupport dbdSupport : dbdSupports) {
                    consoleText.append(dbdSupport.toString());
                }
            }
        }
    }
}
