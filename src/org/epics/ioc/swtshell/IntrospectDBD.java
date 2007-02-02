/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;

import org.epics.ioc.dbd.*;
/**
 * @author mrk
 *
 */
public class IntrospectDBD {
    static private DBD dbd = DBDFactory.getMasterDBD();
    
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    private static class Introspect {
        private Display display;
        private Shell shell;
        private Text text;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        public void start() {
            shell = new Shell(display);
            shell.setText("introspectDBD");
            Menu menuBar = new Menu(shell,SWT.BAR);
            MenuItem fileMenuHeader = new MenuItem(menuBar,SWT.CASCADE);
            fileMenuHeader.setText("&File");
            Menu fileMenu = new Menu(shell,SWT.DROP_DOWN);
            fileMenuHeader.setMenu(fileMenu);
            MenuItem dbdMenuItem = new MenuItem(fileMenu,SWT.CASCADE);
            dbdMenuItem.setText("menu");
            new MenuDBD(dbdMenuItem);
            MenuItem dbdStructureItem = new MenuItem(fileMenu,SWT.CASCADE);
            dbdStructureItem.setText("structure");
            new StructureDBD(dbdStructureItem);
            MenuItem dbdRecordTypeItem = new MenuItem(fileMenu,SWT.CASCADE);
            dbdRecordTypeItem.setText("recordType");
            new RecordTypeDBD(dbdRecordTypeItem);
            MenuItem dbdSupportItem = new MenuItem(fileMenu,SWT.CASCADE);
            dbdSupportItem.setText("support");
            new SupportDBD(dbdSupportItem);
            MenuItem dbdLinkSupportItem = new MenuItem(fileMenu,SWT.CASCADE);
            dbdLinkSupportItem.setText("linkSupport");
            new LinkSupportDBD(dbdLinkSupportItem);
            MenuItem exitItem = new MenuItem(fileMenu,SWT.PUSH);
            exitItem.setText("Exit");
            exitItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    shell.dispose();
                }
                public void widgetSelected(SelectionEvent arg0) {
                    shell.dispose();
                }
            });
            shell.setMenuBar(menuBar);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            Button clearItem = new Button(composite,SWT.PUSH);
            clearItem.setText("&Clear");
            clearItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
                public void widgetSelected(SelectionEvent arg0) {
                    text.selectAll();
                    text.clearSelection();
                    text.setText("");
                }
            });
            text = new Text(composite,SWT.BORDER|SWT.WRAP|SWT.V_SCROLL|SWT.READ_ONLY);
            //text.setSize(sizeX,sizeY); DOES NOT WORK
            Swtshell.makeBlanks(text,20,120);
            shell.pack();
            text.selectAll();
            text.clearSelection();
            text.setText("");
            shell.open();
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
                    text.append(value.toString());
                    return;
                }
                Set<String> keys = menuMap.keySet();
                for(String next: keys) {
                    DBDMenu value = menuMap.get(next);
                    text.append(value.toString());
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
                    text.append(value.toString());
                    return;
                }
                Set<String> keys = structureMap.keySet();
                for(String next: keys) {
                    DBDStructure value = structureMap.get(next);
                    text.append(value.toString());
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
                    text.append(value.toString());
                    return;
                }
                Set<String> keys = recordTypeMap.keySet();
                for(String next: keys) {
                    DBDRecordType value = recordTypeMap.get(next);
                    text.append(value.toString());
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
                    text.append(value.toString());
                    return;
                }
                Set<String> keys = supportMap.keySet();
                for(String next: keys) {
                    DBDSupport value = supportMap.get(next);
                    text.append(value.toString());
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
                    text.append(value.toString());
                    return;
                }
                Set<String> keys = linkSupportMap.keySet();
                for(String next: keys) {
                    DBDLinkSupport value = linkSupportMap.get(next);
                    text.append(value.toString());
                }
            }
        }
    }
}
