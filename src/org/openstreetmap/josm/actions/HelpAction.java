// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open a help browser and displays lightweight online help.
 * @since 155
 */
public class HelpAction extends JosmAction {

    /**
     * Constructs a new {@code HelpAction}.
     */
    public HelpAction() {
        this(true);
    }

    private HelpAction(boolean shortcut) {
        super(tr("Help"), "help", null,
                shortcut ? Shortcut.registerShortcut("system:help", tr("Help: {0}", tr("Help")), KeyEvent.VK_F1, Shortcut.DIRECT) : null,
                true, false);
        setEnabled(!NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
    }

    /**
     * Constructs a new {@code HelpAction} without assigning a shortcut.
     * @return a new {@code HelpAction}
     */
    public static HelpAction createWithoutShortcut() {
        return new HelpAction(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == null) {
            String topic;
            MenuElement[] menuPath = MenuSelectionManager.defaultManager().getSelectedPath();
            if (menuPath.length > 0) {
                // Get help topic from last element in selected menu path (usually a JMenuItem).
                // If a JMenu is selected, which shows a JPopupMenu, then the last path element
                // is a JPopupMenu and it is necessary to look also into previous path elements.
                topic = null;
                for (int i = menuPath.length - 1; i >= 0; i--) {
                    Component c = menuPath[i].getComponent();
                    topic = HelpUtil.getContextSpecificHelpTopic(c);
                    if (topic != null) {
                        break;
                    }
                }
            } else if (e.getSource() instanceof Component) {
                Component c = SwingUtilities.getRoot((Component) e.getSource());
                Point mouse = c.getMousePosition();
                if (mouse != null) {
                    c = SwingUtilities.getDeepestComponentAt(c, mouse.x, mouse.y);
                    topic = HelpUtil.getContextSpecificHelpTopic(c);
                } else {
                    topic = null;
                }
            } else {
                Point mouse = MainApplication.getMainFrame().getMousePosition();
                topic = HelpUtil.getContextSpecificHelpTopic(
                        SwingUtilities.getDeepestComponentAt(MainApplication.getMainFrame(), mouse.x, mouse.y));
            }
            HelpBrowser.setUrlForHelpTopic(Optional.ofNullable(topic).orElse("/"));
        } else {
            HelpBrowser.setUrlForHelpTopic("/");
        }
    }
}
