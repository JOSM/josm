// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
        super(JOptionPane.getFrameForComponent(parent), tr("Preferences"), true /* modal */);
        build();
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
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(600,800)
                    )
            ).applySafe(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
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
            tpPreferences.savePreferences();
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
}
