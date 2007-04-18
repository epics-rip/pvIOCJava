/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.db.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.util.*;;

/**
 * @author mrk
 *
 */
public class IntrospectDatabase {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    private static class Introspect implements SelectionListener, Requester{
        static private DBD dbd = DBDFactory.getMasterDBD();
        private Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        private Display display;
        private Shell shell;
        private Text recordSelectText;
        private Button clearButton;
        private Text consoleText;
        private SelectRecord selectRecord;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        public void start() {
            shell = new Shell(display);
            shell.setText("introspectDatabase");
            GridLayout layout = new GridLayout();
            layout.numColumns = 1;
            shell.setLayout(layout);
            Menu menuBar = new Menu(shell,SWT.BAR);
            shell.setMenuBar(menuBar);
            MenuItem dbdMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdMenu.setText("menu");
            new MenuDBD(dbdMenu);
            MenuItem dbdStructureMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdStructureMenu.setText("structure");
            new StructureDBD(dbdStructureMenu);
            MenuItem dbdRecordTypeMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdRecordTypeMenu.setText("recordType");
            new RecordTypeDBD(dbdRecordTypeMenu);
            MenuItem dbdSupportMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdSupportMenu.setText("support");
            new SupportDBD(dbdSupportMenu);
            MenuItem dbdLinkSupportMenu = new MenuItem(menuBar,SWT.CASCADE);
            dbdLinkSupportMenu.setText("linkSupport");
            new LinkSupportDBD(dbdLinkSupportMenu);
            Composite recordSelectComposite = new Composite(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            recordSelectComposite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            recordSelectComposite.setLayout(layout);
            new Label(recordSelectComposite,SWT.NONE).setText("recordName");
            Button recordSelectButton = new Button(recordSelectComposite,SWT.PUSH);
            recordSelectButton.setText("select");
            selectRecord = new SelectRecord(shell,this);
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
            recordSelectButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    String name = selectRecord.getRecordName();
                    if(name!=null) {
                        DBRecord dbRecord = recordMap.get(name);
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
                    DBRecord dbRecord = recordMap.get(name);
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

        private class MenuDBD implements SelectionListener {
            private Map<String,DBDMenu> menuMap;
            
            private MenuDBD(MenuItem menuItem) {
                Menu menuMenu = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuMenu);
                MenuItem choiceAll = new MenuItem(menuMenu,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                menuMap = dbd.getMenuMap();
                Iterator<String> iter = menuMap.keySet().iterator();
                for(int i=0; i< menuMap.size(); i++) {
                    MenuItem choiceItem = new MenuItem(menuMenu,SWT.PUSH);
                    choiceItem.setText(iter.next());
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
                    DBDMenu value = (DBDMenu)menuMap.get(key);
                    consoleText.append(value.toString());
                    return;
                }
                Set<String> keys = menuMap.keySet();
                for(String next: keys) {
                    DBDMenu value = menuMap.get(next);
                    consoleText.append(value.toString());
                }
            }
            
        }
        
        private class StructureDBD implements SelectionListener {
            private Map<String,DBDStructure> structureMap;
            
            private StructureDBD(MenuItem menuItem) {
                Menu menuStructure = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuStructure);
                MenuItem choiceAll = new MenuItem(menuStructure,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                structureMap = dbd.getStructureMap();
                Iterator<String> iter = structureMap.keySet().iterator();
                for(int i=0; i< structureMap.size(); i++) {
                    MenuItem choiceItem = new MenuItem(menuStructure,SWT.PUSH);
                    choiceItem.setText(iter.next());
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
                    DBDStructure value = (DBDStructure)structureMap.get(key);
                    consoleText.append(value.toString());
                    return;
                }
                Set<String> keys = structureMap.keySet();
                for(String next: keys) {
                    DBDStructure value = structureMap.get(next);
                    consoleText.append(value.toString());
                }
            }
            
        }
        
        private class RecordTypeDBD implements SelectionListener {
            private Map<String,DBDRecordType> recordTypeMap;
            
            private RecordTypeDBD(MenuItem menuItem) {
                Menu menuRecordType = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuRecordType);
                MenuItem choiceAll = new MenuItem(menuRecordType,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                recordTypeMap = dbd.getRecordTypeMap();
                Iterator<String> iter = recordTypeMap.keySet().iterator();
                for(int i=0; i< recordTypeMap.size(); i++) {
                    MenuItem choiceItem = new MenuItem(menuRecordType,SWT.PUSH);
                    choiceItem.setText(iter.next());
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
                    DBDRecordType value = (DBDRecordType)recordTypeMap.get(key);
                    consoleText.append(value.toString());
                    return;
                }
                Set<String> keys = recordTypeMap.keySet();
                for(String next: keys) {
                    DBDRecordType value = recordTypeMap.get(next);
                    consoleText.append(value.toString());
                }
            }
            
        }
        
        private class SupportDBD implements SelectionListener {
            private Map<String,DBDSupport> supportMap;
            
            private SupportDBD(MenuItem menuItem) {
                Menu menuSupport = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuSupport);
                MenuItem choiceAll = new MenuItem(menuSupport,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                supportMap = dbd.getSupportMap();
                Iterator<String> iter = supportMap.keySet().iterator();
                for(int i=0; i< supportMap.size(); i++) {
                    MenuItem choiceItem = new MenuItem(menuSupport,SWT.PUSH);
                    choiceItem.setText(iter.next());
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
                    DBDSupport value = (DBDSupport)supportMap.get(key);
                    consoleText.append(value.toString());
                    return;
                }
                Set<String> keys = supportMap.keySet();
                for(String next: keys) {
                    DBDSupport value = supportMap.get(next);
                    consoleText.append(value.toString());
                }
            }
        }
        
        
        private class LinkSupportDBD implements SelectionListener {
            private Map<String,DBDLinkSupport> linkSupportMap;
            
            private LinkSupportDBD(MenuItem menuItem) {
                Menu menuLinkSupport = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuLinkSupport);
                MenuItem choiceAll = new MenuItem(menuLinkSupport,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                linkSupportMap = dbd.getLinkSupportMap();
                Iterator<String> iter = linkSupportMap.keySet().iterator();
                for(int i=0; i< linkSupportMap.size(); i++) {
                    MenuItem choiceItem = new MenuItem(menuLinkSupport,SWT.PUSH);
                    choiceItem.setText(iter.next());
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
                    DBDLinkSupport value = (DBDLinkSupport)linkSupportMap.get(key);
                    consoleText.append(value.toString());
                    return;
                }
                Set<String> keys = linkSupportMap.keySet();
                for(String next: keys) {
                    DBDLinkSupport value = linkSupportMap.get(next);
                    consoleText.append(value.toString());
                }
            }
        }
    }
}
