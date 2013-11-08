// License: GPL. For details, see LICENSE file.
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
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor.ProgressMonitorDialog;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class PleaseWaitDialog extends JDialog implements ProgressMonitorDialog {

    private final JProgressBar progressBar = new JProgressBar();

    public final JLabel currentAction = new JLabel("");
    private final JLabel customText = new JLabel("");
    public final BoundedRangeModel progress = progressBar.getModel();
    private JButton btnCancel;
    private JButton btnInBackground;
    /** the text area and the scroll pane for the log */
    private JosmTextArea taLog = new JosmTextArea(5,50);
    private  JScrollPane spLog;

    private void initDialog() {
        setLayout(new GridBagLayout());
        JPanel pane = new JPanel(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        pane.add(currentAction, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(customText, GBC.eol().fill(GBC.HORIZONTAL));
        pane.add(progressBar, GBC.eop().fill(GBC.HORIZONTAL));
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridBagLayout());
        btnCancel = new JButton(tr("Cancel"));
        btnCancel.setIcon(ImageProvider.get("cancel"));
        btnCancel.setToolTipText(tr("Click to cancel the current operation"));
        buttons.add(btnCancel);
        btnInBackground = new JButton(tr("In background"));
        btnInBackground.setToolTipText(tr("Click to run job in background"));
        buttons.add(btnInBackground, GBC.std().fill(GBC.VERTICAL).insets(5, 0, 0, 0));
        pane.add(buttons, GBC.eol().anchor(GBC.CENTER));
        GridBagConstraints gc = GBC.eol().fill(GBC.BOTH);
        gc.weighty = 1.0;
        gc.weightx = 1.0;
        pane.add(spLog = new JScrollPane(taLog), gc);
        spLog.setVisible(false);
        setContentPane(pane);
        setCustomText("");
        setLocationRelativeTo(getParent());
        addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {}
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent ev) {
                int w = getWidth();
                if(w > 200) {
                    Main.pref.putInteger("progressdialog.size",w);
                }
            }
        });
    }

    /**
     * Constructs a new {@code PleaseWaitDialog}.
     * @param parent the {@code Component} from which the dialog is displayed. Can be {@code null}.
     */
    public PleaseWaitDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        initDialog();
    }

    @Override
    public void setIndeterminate(boolean newValue) {
        UIManager.put("ProgressBar.cycleTime", UIManager.getInt("ProgressBar.repaintInterval") * 100);
        progressBar.setIndeterminate(newValue);
    }

    protected void adjustLayout() {
        invalidate();
        setDropTarget(null); // Workaround to JDK bug 7027598/7100524/7169912 (#8613)
        pack();
        setSize(Main.pref.getInteger("progressdialog.size", 600), getSize().height);
    }

    /**
     * Sets a custom text line below currentAction. Can be used to display additional information
     * @param text
     */
    @Override
    public void setCustomText(String text) {
        if(text == null || text.trim().length() == 0) {
            customText.setVisible(false);
            adjustLayout();
            return;
        }
        customText.setText(text);
        if (!customText.isVisible()) {
            customText.setVisible(true);
            adjustLayout();
        }
    }

    @Override
    public void setCurrentAction(String text) {
        currentAction.setText(text);
    }

    /**
     * Appends a log message to the progress dialog. If the log area isn't visible yet
     * it becomes visible. The height of the progress dialog is slightly increased too.
     *
     * @param message the message to append to the log. Ignore if null or white space only.
     */
    @Override
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

    public void setInBackgroundPossible(boolean value) {
        btnInBackground.setVisible(value);
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

    /**
     * Installs a callback for the "In background" button. If callback is null, all action listeners
     * are removed from the cancel button.
     *
     * @param callback the cancel callback
     */
    public void setInBackgroundCallback(ActionListener callback) {
        if (callback == null) {
            ActionListener[] listeners = btnInBackground.getActionListeners();
            for (ActionListener l: listeners) {
                btnInBackground.removeActionListener(l);
            }
        } else {
            btnInBackground.addActionListener(callback);
        }
    }

    @Override
    public void updateProgress(int progress) {
        this.progress.setValue(progress);
        this.progressBar.repaint();
    }

}
