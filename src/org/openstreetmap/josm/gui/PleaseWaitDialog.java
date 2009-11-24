// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class PleaseWaitDialog extends JDialog {

    private final JProgressBar progressBar = new JProgressBar();

    public final JLabel currentAction = new JLabel("");
    private final JLabel customText = new JLabel("");
    public final BoundedRangeModel progress = progressBar.getModel();
    private  JButton btnCancel;
    /** the text area and the scroll pane for the log */
    private JTextArea taLog = new JTextArea(5,50);
    private  JScrollPane spLog;

    private void initDialog() {
        setLayout(new GridBagLayout());
        JPanel pane = new JPanel(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        pane.add(currentAction, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(customText, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(progressBar, GBC.eop().fill(GBC.HORIZONTAL));
        btnCancel = new JButton(tr("Cancel"));
        btnCancel.setIcon(ImageProvider.get("cancel"));
        btnCancel.setToolTipText(tr("Click to cancel the current operation"));
        pane.add(btnCancel, GBC.eol().anchor(GBC.CENTER));
        GridBagConstraints gc = GBC.eol().fill(GBC.BOTH);
        gc.weighty = 1.0;
        gc.weightx = 1.0;
        pane.add(spLog = new JScrollPane(taLog), gc);
        spLog.setVisible(false);
        setContentPane(pane);
        //setSize(Main.pref.getInteger("progressdialog.size",600),100);
        setCustomText("");
        setLocationRelativeTo(getParent());
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

    public PleaseWaitDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), true);
        initDialog();
    }

    public void setIndeterminate(boolean newValue) {
        UIManager.put("ProgressBar.cycleTime", UIManager.getInt("ProgressBar.repaintInterval") * 100);
        progressBar.setIndeterminate(newValue);
    }

    protected void adjustLayout() {
        invalidate();
        pack();
        setSize(Main.pref.getInteger("progressdialog.size", 600), getSize().height);
    }

    /**
     * Sets a custom text line below currentAction. Can be used to display additional information
     * @param text
     */
    public void setCustomText(String text) {
        if(text == null || text.trim().length() == 0) {
            customText.setVisible(false);
            adjustLayout();
            return;
        }
        if (!customText.isVisible()) {
            customText.setVisible(true);
            adjustLayout();
        }
        customText.setText(text);
    }

    /**
     * Appends a log message to the progress dialog. If the log area isn't visible yet
     * it becomes visible. The height of the progress dialog is slightly increased too.
     *
     * @param message the message to append to the log. Ignore if null or white space only.
     */
    public void appendLogMessage(String message) {
        if (message == null || message.trim().length() ==0 )
            return;
        if (!spLog.isVisible()) {
            spLog.setVisible(true);
            taLog.setVisible(true);
            adjustLayout();
        }
        taLog.append(message);
        taLog.append("\n");
        spLog.getVerticalScrollBar().setValue(spLog.getVerticalScrollBar().getMaximum());
    }

    /**
     * Sets whether the cancel button is enabled or not
     *
     * @param enabled true, if the cancel button is enabled; false otherwise
     */
    public void setCancelEnabled(boolean enabled) {
        btnCancel.setEnabled(enabled);
    }

    /**
     * Installs a callback for the cancel button. If callback is null, all action listeners
     * are removed from the cancel button.
     *
     * @param callback the cancel callback
     */
    public void setCancelCallback(ActionListener callback) {
        if (callback == null) {
            ActionListener[] listeners = btnCancel.getActionListeners();
            for (ActionListener l: listeners) {
                btnCancel.removeActionListener(l);
            }
        } else {
            btnCancel.addActionListener(callback);
        }
    }
}
