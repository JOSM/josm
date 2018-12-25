// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.ValidationListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * The main preferences dialog.
 *
 * Dialog window where the user can change various settings. Organized in main
 * tabs to the left ({@link TabPreferenceSetting}) and (optional) sub-pages
 * ({@link SubPreferenceSetting}).
 */
public class PreferenceDialog extends JDialog {

    private final PreferenceTabbedPane tpPreferences = new PreferenceTabbedPane();
    private final ContextSensitiveHelpAction helpAction = new ContextSensitiveHelpAction();
    private final WindowEventHandler windowEventHandler = new WindowEventHandler();
    private boolean canceled;

    /**
     * Constructs a new {@code PreferenceDialog}.
     * @param parent parent component
     */
    public PreferenceDialog(Component parent) {
        super(GuiHelper.getFrameForComponent(parent), tr("Preferences"), ModalityType.DOCUMENT_MODAL);
        build();
        this.setMinimumSize(new Dimension(600, 350));
        // set the maximum width to the current screen. If the dialog is opened on a
        // smaller screen than before, this will reset the stored preference.
        this.setMaximumSize(GuiHelper.getScreenSize());
    }

    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        JCheckBox expert = new JCheckBox(tr("Expert Mode"));
        expert.setSelected(ExpertToggleAction.isExpert());
        expert.addActionListener(e -> ExpertToggleAction.getInstance().actionPerformed(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OKAction okAction = new OKAction();
        btns.add(new JButton(okAction));
        btns.add(new JButton(new CancelAction()));
        btns.add(new JButton(helpAction));
        pnl.add(expert, GBC.std().insets(5, 0, 0, 0));
        pnl.add(btns, GBC.std().fill(GBC.HORIZONTAL));
        InputMapUtils.addCtrlEnterAction(pnl, okAction);
        return pnl;
    }

    protected final void build() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(tpPreferences, BorderLayout.CENTER);
        tpPreferences.buildGui();
        tpPreferences.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        c.add(buildActionPanel(), BorderLayout.SOUTH);
        addWindowListener(windowEventHandler);

        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());
        setHelpContext(HelpUtil.ht("/Action/Preferences"));
    }

    /**
     * Sets the help context of the preferences dialog.
     * @param helpContext new help context
     * @since 13431
     */
    public final void setHelpContext(String helpContext) {
        helpAction.setHelpTopic(helpContext);
        HelpUtil.setHelpContext(getRootPane(), helpContext);
    }

    /**
     * Replies the preferences tabbed pane.
     * @return The preferences tabbed pane, or null if the dialog is not yet initialized.
     * @since 5604
     */
    public PreferenceTabbedPane getTabbedPane() {
        return tpPreferences;
    }

    /**
     * Determines if preferences changes have been canceled.
     * @return {@code true} if preferences changes have been canceled
     */
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
            // Have to take window decorations into account or the windows will be too large
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
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Select preferences tab by name.
     * @param name preferences tab name (icon)
     */
    public void selectPreferencesTabByName(String name) {
        tpPreferences.selectTabByName(name);
    }

    /**
     * Select preferences tab by class.
     * @param clazz preferences tab class
     */
    public void selectPreferencesTabByClass(Class<? extends TabPreferenceSetting> clazz) {
        tpPreferences.selectTabByPref(clazz);
    }

    /**
     * Select preferences sub-tab by class.
     * @param clazz preferences sub-tab class
     */
    public void selectSubPreferencesTabByClass(Class<? extends SubPreferenceSetting> clazz) {
        tpPreferences.selectSubTabByPref(clazz);
    }

    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the preferences dialog and discard preference updates"));
        }

        public void cancel() {
            setCanceled(true);
            dispose();
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            cancel();
        }
    }

    class OKAction extends AbstractAction {
        OKAction() {
            putValue(NAME, tr("OK"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Save the preferences and close the dialog"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            for (ValidationListener listener: tpPreferences.validationListeners) {
                if (!listener.validatePreferences())
                    return;
            }

            tpPreferences.savePreferences();
            setCanceled(false);
            dispose();
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent arg0) {
            new CancelAction().cancel();
        }
    }

    @Override
    public void dispose() {
        removeWindowListener(windowEventHandler);
        super.dispose();
    }
}
