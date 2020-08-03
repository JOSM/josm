// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action to open browser on given URL.
 * @see OpenBrowser
 * @since 15706
 */
public class OpenBrowserAction extends AbstractAction {

    private final List<String> urls = new ArrayList<>();
    private final String originalName;

    /**
     * Constructs a new {@link OpenBrowserAction}.
     * @param name the name of this action
     * @param url the URL to launch
     */
    public OpenBrowserAction(String name, String url) {
        new ImageProvider("help/internet").getResource().attachImageIcon(this, true);
        this.urls.add(url);
        this.originalName = name;
        updateNameAndDescription();
    }

    /**
     * Adds an additional URL to be launched.
     * @param url the URL to launch
     */
    public void addUrl(String url) {
        urls.add(url);
        updateNameAndDescription();
    }

    private void updateNameAndDescription() {
        final Serializable countString = urls.size() > 1 ? tr(" ({0})", urls.size()) : "";
        putValue(NAME, originalName + countString);
        putValue(SHORT_DESCRIPTION, Utils.shortenString(tr("Open {0}", String.join(", ", urls)), 256));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final int size = urls.size();
        if (size > Config.getPref().getInt("warn.open.maxbrowser", 10) && !confirmLaunchMultiple(size)) {
            return;
        }
        for (String url : urls) {
            OpenBrowser.displayUrl(url);
        }
    }

    /**
     * Asks user confirmation before launching a large number of browser windows.
     * @param numBrowsers the number of browser windows to open
     * @return {@code true} if the user confirms, {@code false} otherwise
     */
    public static boolean confirmLaunchMultiple(int numBrowsers) {
        String msg = /* for correct i18n of plural forms - see #9110 */ trn(
                "You are about to launch {0} browser window.<br>"
                        + "This may both clutter your screen with browser windows<br>"
                        + "and take some time to finish.",
                "You are about to launch {0} browser windows.<br>"
                        + "This may both clutter your screen with browser windows<br>"
                        + "and take some time to finish.", numBrowsers, numBrowsers);
        HelpAwareOptionPane.ButtonSpec[] spec = {
                new HelpAwareOptionPane.ButtonSpec(
                        tr("Continue"),
                        new ImageProvider("ok"),
                        trn("Click to continue and to open {0} browser", "Click to continue and to open {0} browsers",
                                numBrowsers, numBrowsers),
                        null // no specific help topic
                ),
                new HelpAwareOptionPane.ButtonSpec(
                        tr("Cancel"),
                        new ImageProvider("cancel"),
                        tr("Click to abort launching external browsers"),
                        null // no specific help topic
                )
        };
        return 0 == HelpAwareOptionPane.showOptionDialog(
                MainApplication.getMainFrame(),
                new StringBuilder(msg).insert(0, "<html>").append("</html>").toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                null,
                spec,
                spec[0],
                HelpUtil.ht("/WarningMessages#ToManyBrowsersToOpen")
        );
    }
}
