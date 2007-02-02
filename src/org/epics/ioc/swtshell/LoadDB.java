/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;

import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class LoadDB {
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
            shell.setText("loadDB");
            Menu menuBar = new Menu(shell,SWT.BAR);
            MenuItem fileMenuHeader = new MenuItem(menuBar,SWT.CASCADE);
            fileMenuHeader.setText("&File");
            Menu fileMenu = new Menu(shell,SWT.DROP_DOWN);
            fileMenuHeader.setMenu(fileMenu);
            new Load(fileMenu);
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
        
        private class Load implements SelectionListener, Requestor {
            private FileDialog fd;
            
            private Load(Menu filemenu) {
                final MenuItem openItem = new MenuItem(filemenu, SWT.PUSH);
                fd = new FileDialog(shell, SWT.OPEN);
                fd.setText("Open");
                String[] filterExt = { "*.xml"};
                fd.setFilterExtensions(filterExt);
                openItem.setText("&Open");
                openItem.addSelectionListener(this);
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
                String file = fd.open();
                if(file==null) return;
                try {
                    boolean initOK = IOCFactory.initDatabase(file,this);
                    if(!initOK) {
                        text.append(String.format("IOCFactory.initDatabase failed%n"));
                    }
                } catch (RuntimeException e) {
                    text.append(String.format("%s%n",e.getMessage()));
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#getRequestorName()
             */
            public String getRequestorName() {
                return "swtshell.loadDBD";
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                text.append(String.format("%s %s %n",messageType.toString(),message));
            }
            
        }
    }
}
