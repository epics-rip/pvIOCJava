/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDCreate;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDRecordType;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

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
    
    /**
     * A shell for introspecting the local IOC database.
     * @param display The display.
     */
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    
    private static class Introspect implements SelectionListener, Requester{
        static private DBD dbd = DBDFactory.getMasterDBD();
        private Display display;
        private Shell shell;
        private Text recordSelectText;
        private Button clearButton;
        private Text consoleText;
        private SelectLocalRecord selectLocalRecord;
        
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
            MenuItem dbdRecordTypeMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdRecordTypeMenu.setText("recordType");
            new RecordTypeDBD(dbdRecordTypeMenu);
            MenuItem dbdCreateMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdCreateMenu.setText("create");
            new CreateDBD(dbdCreateMenu);
            MenuItem dbdSupportMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdSupportMenu.setText("support");
            new SupportDBD(dbdSupportMenu);
            Composite recordSelectComposite = new Composite(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            recordSelectComposite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            recordSelectComposite.setLayout(layout);
            new Label(recordSelectComposite,SWT.NONE).setText("recordName");
            Button recordSelectButton = new Button(recordSelectComposite,SWT.PUSH);
            recordSelectButton.setText("select");
            recordSelectText = new Text(recordSelectComposite,SWT.SINGLE);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            recordSelectText.setLayoutData(gridData);
            recordSelectText.addSelectionListener(this);            
            Composite consoleComposite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 1;
            consoleComposite.setLayout(layout);
            gridData = new GridData(GridData.FILL_BOTH);
            consoleComposite.setLayoutData(gridData);
            clearButton = new Button(consoleComposite,SWT.PUSH);
            clearButton.setText("&Clear");
            clearButton.addSelectionListener(this);
            consoleText = new Text(consoleComposite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            consoleText.setLayoutData(gridData);
            selectLocalRecord = SelectLocalRecordFactory.create(shell,this);
            recordSelectButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    
                    String recordName = selectLocalRecord.getRecordName();
                    if(recordName!=null) {
                        DBRecord dbRecord = iocdb.findRecord(recordName);
                        if(dbRecord!=null) {
                            consoleText.append(dbRecord.toString());
                        } else {
                            consoleText.append("record not found");
                        }
                    }
                }
            });
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
            if(e.getSource()==recordSelectText) {
                String name = recordSelectText.getText();
                if(name!=null) {
                    DBRecord dbRecord = iocdb.findRecord(name);
                    if(dbRecord!=null) {
                        consoleText.append(dbRecord.toString());
                    } else {
                        consoleText.append("record not found");
                    }
                }
                return;
            }
            if(e.getSource()==clearButton) {
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
        
        private class RecordTypeDBD implements SelectionListener {
            
            private RecordTypeDBD(MenuItem menuItem) {
                Menu menuRecordType = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuRecordType);
                MenuItem choiceAll = new MenuItem(menuRecordType,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                DBDRecordType[] dbdRecordTypes = dbd.getDBDRecordTypes();
                for(DBDRecordType dbdRecordType : dbdRecordTypes) {
                    String name = dbdRecordType.getStructureName();
                    MenuItem choiceItem = new MenuItem(menuRecordType,SWT.PUSH);
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
                    DBDRecordType value = dbd.getRecordType(name);
                    consoleText.append(value.toString());
                    return;
                }
                DBDRecordType[] dbdRecordTypes = dbd.getDBDRecordTypes();
                for(DBDRecordType dbdRecordType : dbdRecordTypes) {
                    consoleText.append(dbdRecordType.toString());
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
