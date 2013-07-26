// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.awt.event.ActionEvent;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

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

    /**
     * Sets the help topic
     *
     * @param relativeHelpTopic the relative help topic
     */
    public void setHelpTopic(String relativeHelpTopic) {
        if (relativeHelpTopic == null)
            relativeHelpTopic = "/";
        this.helpTopic = relativeHelpTopic;
    }

    /**
     * Creates a help topic for the root help topic
     *
     */
    public ContextSensitiveHelpAction() {
        this(ht("/"));
    }

    /**
     *
     * @param helpTopic
     */
    public ContextSensitiveHelpAction(String helpTopic) {
        putValue(SHORT_DESCRIPTION, tr("Show help information"));
        putValue(NAME, tr("Help"));
        putValue(SMALL_ICON, ImageProvider.get("help"));
        this.helpTopic = helpTopic;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (helpTopic != null) {
            HelpBrowser.setUrlForHelpTopic(helpTopic);
        }
    }
}
