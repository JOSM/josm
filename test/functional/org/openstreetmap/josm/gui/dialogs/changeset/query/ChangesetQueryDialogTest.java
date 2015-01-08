// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import org.junit.Ignore;

import javax.swing.JFrame;

@Ignore
public class ChangesetQueryDialogTest extends JFrame {

    private ChangesetQueryDialog dialog;

    public void start() {
        dialog = new ChangesetQueryDialog(this);
        dialog.initForUserInput();
        dialog.setVisible(true);
    }

    static public void main(String args[]) {
        new ChangesetQueryDialogTest().start();
    }
}
