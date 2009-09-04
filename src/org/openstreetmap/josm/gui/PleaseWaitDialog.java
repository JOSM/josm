// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class PleaseWaitDialog extends JDialog {

    private final JProgressBar progressBar = new JProgressBar();

    public final JLabel currentAction = new JLabel("");
    private final JLabel customText = new JLabel("");
    public final BoundedRangeModel progress = progressBar.getModel();
    public final JButton cancel = new JButton(I18n.tr("Cancel"));

    private void initDialog() {
        setLayout(new GridBagLayout());
        JPanel pane = new JPanel(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        pane.add(currentAction, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(customText, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(progressBar, GBC.eop().fill(GBC.HORIZONTAL));
        pane.add(cancel, GBC.eol().anchor(GBC.CENTER));
        setContentPane(pane);
        //setSize(Main.pref.getInteger("progressdialog.size",600),100);
        setCustomText("");
        setLocationRelativeTo(Main.parent);
        addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
            public void componentResized(ComponentEvent ev) {
                int w = getWidth();
                if(w > 200) {
                    Main.pref.putInteger("progressdialog.size",w);
                }
            }
        });
    }

    public PleaseWaitDialog(Frame parent) {
        super(parent, true);
        initDialog();
    }

    public PleaseWaitDialog(Dialog parent) {
        super(parent, true);
        initDialog();
    }

    public void setIndeterminate(boolean newValue) {
        UIManager.put("ProgressBar.cycleTime", UIManager.getInt("ProgressBar.repaintInterval") * 100);
        progressBar.setIndeterminate(newValue);
    }

    /**
     * Sets a custom text line below currentAction. Can be used to display additional information
     * @param text
     */
    public void setCustomText(String text) {
        if(text.length() == 0) {
            customText.setVisible(false);
            setSize(Main.pref.getInteger("progressdialog.size", 600), 100);
            return;
        }

        customText.setVisible(true);
        customText.setText(text);
        setSize(Main.pref.getInteger("progressdialog.size", 600), 120);
    }
}
