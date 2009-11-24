package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public abstract class RunnableAction extends AbstractAction implements Runnable {

    public RunnableAction() {
    }

    public abstract void run();

    public void actionPerformed(ActionEvent arg0) {
        run();
    }
}
