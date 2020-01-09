// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Action to open browser on given URL.
 * @see OpenBrowser
 * @since xxx
 */
public class OpenBrowserAction extends AbstractAction {

    private final String url;

    /**
     * Constructs a new {@link OpenBrowserAction}.
     * @param name the name of this action
     * @param url the URL to launch
     */
    public OpenBrowserAction(String name, String url) {
        super(name);
        putValue(SHORT_DESCRIPTION, tr("Open {0}", url));
        new ImageProvider("help/internet").getResource().attachImageIcon(this, true);
        this.url = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenBrowser.displayUrl(url);
    }
}
