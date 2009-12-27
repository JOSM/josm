// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import javax.swing.JFrame;

import org.openstreetmap.josm.fixtures.JOSMFixture;

public class ChangesetQueryDialogTest extends JFrame {

    private ChangesetQueryDialog dialog;

    public ChangesetQueryDialogTest() {
    }

    public void start() {
        JOSMFixture fixture = JOSMFixture.createFunctionalTestFixture();
        dialog = new ChangesetQueryDialog(this);
        dialog.initForUserInput();
        dialog.setVisible(true);
    }


    static public void main(String args[]) {
        new ChangesetQueryDialogTest().start();
    }
}
