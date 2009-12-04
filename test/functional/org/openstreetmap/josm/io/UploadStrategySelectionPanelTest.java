// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.openstreetmap.josm.gui.io.UploadStrategySelectionPanel;

public class UploadStrategySelectionPanelTest extends JFrame {

    private UploadStrategySelectionPanel pnl;

    protected void build()  {
        getContentPane().setLayout(new BorderLayout());
        pnl = new UploadStrategySelectionPanel();
        getContentPane().add(pnl, BorderLayout.CENTER);
        setSize(400,400);
    }

    public UploadStrategySelectionPanelTest() {
        build();
        pnl.setNumUploadedObjects(1500);
    }

    public static void main(String args[]) {
        new UploadStrategySelectionPanelTest().setVisible(true);
    }
}
