// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class PleaseWaitDialog extends JDialog {

    private final JProgressBar progressBar = new JProgressBar();

    public final JLabel currentAction = new JLabel(I18n.tr("Contacting the OSM server..."));
    public final BoundedRangeModel progress = progressBar.getModel();
    public final JButton cancel = new JButton(I18n.tr("Cancel"));

    public PleaseWaitDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), true);
        setLayout(new GridBagLayout());
        JPanel pane = new JPanel(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        pane.add(currentAction, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(progressBar, GBC.eop().fill(GBC.HORIZONTAL));
        pane.add(cancel, GBC.eol().anchor(GBC.CENTER));
        setContentPane(pane);
        setSize(Main.pref.getInteger("progressdialog.size",600),100);
        setLocationRelativeTo(Main.parent);
        addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
            public void componentResized(ComponentEvent ev) {
               int w = getWidth();
               if(w > 200)
                   Main.pref.putInteger("progressdialog.size",w);
            }
        });
    }
    
    public void setIndeterminate(boolean newValue) {
        progressBar.setIndeterminate(newValue);
    }
}
