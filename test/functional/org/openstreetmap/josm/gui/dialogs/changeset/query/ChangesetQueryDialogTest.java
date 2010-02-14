// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import javax.swing.JFrame;

public class ChangesetQueryDialogTest extends JFrame {

    private ChangesetQueryDialog dialog;

    public ChangesetQueryDialogTest() {
    }

    public void start() {
        dialog = new ChangesetQueryDialog(this);
        dialog.initForUserInput();
        dialog.setVisible(true);
    }


    static public void main(String args[]) {
        new ChangesetQueryDialogTest().start();
    }
}
