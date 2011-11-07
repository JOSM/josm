// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.ValidationListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class PreferenceDialog extends JDialog {

    private PreferenceTabbedPane tpPreferences;
    private boolean canceled;

    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.add(new SideButton(new OKAction()));
        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Action/Preferences"))));
        return pnl;
    }

    protected void build() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(tpPreferences = new PreferenceTabbedPane(), BorderLayout.CENTER);
        tpPreferences.buildGui();
        tpPreferences.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        c.add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());

        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new CancelAction());
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Action/Preferences"));
    }

    public PreferenceDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), tr("Preferences"), ModalityType.DOCUMENT_MODAL);
        build();
        this.setMinimumSize(new Dimension(600, 350));
        // set the maximum width to the current screen. If the dialog is opened on a
        // smaller screen than before, this will reset the stored preference.
        this.setMaximumSize( Toolkit.getDefaultToolkit().getScreenSize());
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Make the pref window at most as large as the parent JOSM window
            // Have to take window decorations into account or the windows will
            // be too large
            Insets i = this.getParent().getInsets();
            Dimension p = this.getParent().getSize();
            p = new Dimension(Math.min(p.width-i.left-i.right, 700),
                    Math.min(p.height-i.top-i.bottom, 800));
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            p
                    )
            ).applySafe(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    void selectPreferencesTabByName(String name) {
        tpPreferences.selectTabByName(name);
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close the preferences dialog and discard preference updates"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
            tpPreferences.validationListeners.clear();
        }

        public void actionPerformed(ActionEvent evt) {
            cancel();
        }
    }

    class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, tr("OK"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(SHORT_DESCRIPTION, tr("Save the preferences and close the dialog"));
        }

        public void actionPerformed(ActionEvent evt) {
            for (ValidationListener listener: tpPreferences.validationListeners) {
                if (!listener.validatePreferences())
                    return;
            }

            tpPreferences.savePreferences();
            tpPreferences.validationListeners.clear();
            setCanceled(false);
            setVisible(false);
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent arg0) {
            new CancelAction().cancel();
        }
    }

    public void selectMapPaintPreferenceTab() {
        tpPreferences.setSelectedComponent(tpPreferences.map);
        tpPreferences.mapcontent.setSelectedIndex(1);
    }
}
