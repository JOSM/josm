// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpBrowserProxy;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Open a help browser and displays lightweight online help.
 *
 */
public class HelpAction extends AbstractAction {

    public HelpAction() {
        super(tr("Help"), ImageProvider.get("help"));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == null) {
            String topic = null;
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
                HelpBrowserProxy.getInstance().setUrlForHelpTopic("/");
            } else {
                HelpBrowserProxy.getInstance().setUrlForHelpTopic(topic);
            }
        } else {
            HelpBrowserProxy.getInstance().setUrlForHelpTopic("/");
        }
    }
}
