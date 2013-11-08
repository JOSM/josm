// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Open a help browser and displays lightweight online help.
 *
 */
public class HelpAction extends AbstractAction {

    public HelpAction() {
        super(tr("Help"), ImageProvider.get("help"));
        putValue("toolbar", "help");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == null) {
            String topic;
            if (e.getSource() instanceof Component) {
                Component c = SwingUtilities.getRoot((Component)e.getSource());
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
            if (topic == null) {
                HelpBrowser.setUrlForHelpTopic("/");
            } else {
                HelpBrowser.setUrlForHelpTopic(topic);
            }
        } else {
            HelpBrowser.setUrlForHelpTopic("/");
        }
    }
}
