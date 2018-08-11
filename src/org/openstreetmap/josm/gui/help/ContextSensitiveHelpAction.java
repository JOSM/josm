// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the standard help action to be used with help buttons for
 * context sensitive help
 * @since 2289
 */
public class ContextSensitiveHelpAction extends AbstractAction {

    private String helpTopic;

    /**
     * Sets the help topic.
     *
     * @param relativeHelpTopic the relative help topic
     */
    public void setHelpTopic(String relativeHelpTopic) {
        helpTopic = Optional.ofNullable(relativeHelpTopic).orElse("/");
    }

    /**
     * Constructs a new {@code ContextSensitiveHelpAction} for the root help topic.
     */
    public ContextSensitiveHelpAction() {
        this(ht("/"));
    }

    /**
     * Constructs a new {@code ContextSensitiveHelpAction} for a given help topic.
     * @param helpTopic The help topic
     */
    public ContextSensitiveHelpAction(String helpTopic) {
        putValue(SHORT_DESCRIPTION, tr("Show help information"));
        putValue(NAME, tr("Help"));
        new ImageProvider("help").getResource().attachImageIcon(this);
        this.helpTopic = helpTopic;
        setEnabled(!NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (helpTopic != null) {
            HelpBrowser.setUrlForHelpTopic(helpTopic);
        }
    }
}
