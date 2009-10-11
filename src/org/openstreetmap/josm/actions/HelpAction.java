// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpBrowserProxy;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Open a help browser and displays lightweight online help.
 *
 * @author imi
 */
public class HelpAction extends AbstractAction {


    private String pathhelp = Main.pref.get("help.pathhelp", "Help/");
    private String pathmenu = Main.pref.get("help.pathmenu", "Menu/");

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
                    topic = contextSensitiveHelp(c);
                } else {
                    topic = null;
                }
            } else {
                Point mouse = Main.parent.getMousePosition();
                topic = contextSensitiveHelp(SwingUtilities.getDeepestComponentAt(Main.parent, mouse.x, mouse.y));
            }
            if (topic == null) {
                HelpBrowserProxy.getInstance().setUrlForHelpTopic("Help");
            } else {
                help(topic);
            }
        } else {
            HelpBrowserProxy.getInstance().setUrlForHelpTopic("Help");
        }
    }

    /**
     * @return The topic of the help. <code>null</code> for "don't know"
     */
    private String contextSensitiveHelp(Object c) {
        if (c == null)
            return null;
        if (c instanceof Helpful)
            return ((Helpful)c).helpTopic();
        if (c instanceof JMenu) {
            JMenu b = (JMenu)c;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return pathmenu+b.getText();
        }
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)c;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return contextSensitiveHelp(((AbstractButton)c).getAction());
        }
        if (c instanceof Action)
            return (String)((Action)c).getValue("help");
        if (c instanceof JComponent && ((JComponent)c).getClientProperty("help") != null)
            return (String)((JComponent)c).getClientProperty("help");
        if (c instanceof Component)
            return contextSensitiveHelp(((Component)c).getParent());
        return null;
    }

    /**
     * Displays the help (or browse on the already open help) on the online page
     * with the given help topic. Use this for larger help descriptions.
     */
    public void help(String topic) {
        HelpBrowserProxy.getInstance().setUrlForHelpTopic(pathhelp + topic);
    }
}
