// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import javax.swing.JFrame;

import org.junit.Ignore;

@Ignore
public class ChangesetQueryDialogTest extends JFrame {

    private ChangesetQueryDialog dialog;

    public void start() {
        dialog = new ChangesetQueryDialog(this);
        dialog.initForUserInput();
        dialog.setVisible(true);
    }

    public static void main(String args[]) {
        new ChangesetQueryDialogTest().start();
    }
}
