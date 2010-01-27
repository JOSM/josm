// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Allows to download data from any URL.
 *
 * @author Matthias Julius <matthias@julius-net.net>
 */
public class DownloadUrlDialog extends JDialog  {

    /** the unique instance of the download URL dialog */
    static private DownloadUrlDialog instance;

    /**
     * Returns the unique instance of the download URL dialog
     *
     * @return the unique instance of the download URL dialog
     */
    static public DownloadUrlDialog getInstance() {
        if (instance == null) {
            instance = new DownloadUrlDialog(Main.parent);
        }
        return instance;
    }

    private JCheckBox cbNewLayer;
    private JTextField url;
    private boolean canceled;

    /** the download action and button */
    private DownloadAction actDownload;
    private SideButton btnDownload;

    private void makeCheckBoxRespondToEnter(JCheckBox cb) {
        cb.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "doDownload");
        cb.getActionMap().put("doDownload", actDownload);
    }

    public JPanel buildMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        cbNewLayer = new JCheckBox(tr("Download as new layer"));
        cbNewLayer.setToolTipText(tr("<html>Select to download data into a new data layer.<br>"
                +"Unselect to download into the currently active data layer.</html>"));
        panel.add(cbNewLayer, GBC.eol().insets(0,5,0,0));

        url = new JTextField(50);
        panel.add(new JLabel(tr("URL")), GBC.std().insets(0,0,10,0));
        panel.add(url, GBC.std());

        return panel;
    }

    protected JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        // -- download button
        panel.add(btnDownload = new SideButton(actDownload = new DownloadAction()));
        btnDownload.setFocusable(true);
        btnDownload.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "download");
        btnDownload.getActionMap().put("download", actDownload);
        makeCheckBoxRespondToEnter(cbNewLayer);

        // -- cancel button
        SideButton btnCancel;
        CancelAction actCancel = new CancelAction();
        panel.add(btnCancel = new SideButton(actCancel));
        btnCancel.setFocusable(true);
        btnCancel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enter");
        btnCancel.getActionMap().put("enter",actCancel);

        // -- cancel on ESC
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "cancel");
        getRootPane().getActionMap().put("cancel", actCancel);

        // -- help button
        SideButton btnHelp;
        panel.add(btnHelp = new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/DownloadUrlDialog"))));
        btnHelp.setFocusable(true);
        btnHelp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enter");
        btnHelp.getActionMap().put("enter",btnHelp.getAction());

        return panel;
    }

    public DownloadUrlDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent),tr("Download from URL"), true /* modal */);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildMainPanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);

        HelpUtil.setHelpContext(getRootPane(), ht("/Dialog/DownloadUrlDialog"));
        addWindowListener(new WindowEventHandler());
        restoreSettings();
    }

    public void startDownload() {
        actDownload.run();
    }

    /**
     * Returns true if the user selected to download into a new layer
     *
     * @return true if the user selected to download into a new layer
     */
    public boolean isNewLayerRequired() {
        return cbNewLayer.isSelected();
    }

    /*
     * Returns the URL to download
     */
    public String getUrl() {
        return url.getText().trim();
    }

    /**
     * Remembers the current settings in the download dialog
     */
    public void rememberSettings() {
        Main.pref.put("downloadUrl.newlayer", cbNewLayer.isSelected());
    }

    public void restoreSettings() {
        cbNewLayer.setSelected(Main.pref.getBoolean("downloadUrl.newlayer", false));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(480,180)
                    )
            ).applySafe(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Returns true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and to abort downloading"));
        }

        public void run() {
            setCanceled(true);
            setVisible(false);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class DownloadAction extends AbstractAction {
        public DownloadAction() {
            putValue(NAME, tr("Download"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(SHORT_DESCRIPTION, tr("Click do download from URL"));
        }

        public void run() {
            setCanceled(false);
            setVisible(false);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            new CancelAction().run();
        }

        @Override
        public void windowActivated(WindowEvent e) {
            btnDownload.requestFocusInWindow();
        }
    }
}
