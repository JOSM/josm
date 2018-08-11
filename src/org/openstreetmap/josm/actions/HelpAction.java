// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
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
                shortcut ? Shortcut.registerShortcut("system:help", tr("Help"), KeyEvent.VK_F1, Shortcut.DIRECT) : null,
                true);
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
            if (e.getSource() instanceof Component) {
                Component c = SwingUtilities.getRoot((Component) e.getSource());
                Point mouse = c.getMousePosition();
                if (mouse != null) {
                    c = SwingUtilities.getDeepestComponentAt(c, mouse.x, mouse.y);
                    topic = HelpUtil.getContextSpecificHelpTopic(c);
                } else {
                    topic = null;
                }
            } else {
                Point mouse = Main.parent.getMousePosition();
                topic = HelpUtil.getContextSpecificHelpTopic(SwingUtilities.getDeepestComponentAt(Main.parent, mouse.x, mouse.y));
            }
            HelpBrowser.setUrlForHelpTopic(Optional.ofNullable(topic).orElse("/"));
        } else {
            HelpBrowser.setUrlForHelpTopic("/");
        }
    }
}
