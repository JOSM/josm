// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the standard help action to be used with help buttons for
 * context sensitive help
 * 
 */
public class ContextSensitiveHelpAction extends AbstractAction {

    /** the help topic */
    private String helpTopic;

    public ContextSensitiveHelpAction(String helpTopic) {
        putValue(SHORT_DESCRIPTION, tr("Show help information"));
        putValue(NAME, tr("Help"));
        putValue(SMALL_ICON, ImageProvider.get("help"));
        this.helpTopic = helpTopic;
    }

    public void actionPerformed(ActionEvent e) {
        if (helpTopic != null) {
            HelpBrowserProxy.getInstance().setUrlForHelpTopic(helpTopic);
        }
    }
}
