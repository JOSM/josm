// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class AbstractInfoAction extends JosmAction {

    public AbstractInfoAction(boolean installAdapters) {
        super(installAdapters);
    }

    public AbstractInfoAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register, String toolbarId, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbarId, installAdapters);
    }

    /**
     * replies the base URL for browsing information about about a primitive
     *
     * @return the base URL, i.e. http://api.openstreetmap.org/browse
     */
    static public String getBaseBrowseUrl() {
        String baseUrl = Main.pref.get("osm-server.url", OsmApi.DEFAULT_API_URL);
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/browse");
        if (ret.equals(baseUrl)) {
            Main.warn(tr("Unexpected format of API base URL. Redirection to info or history page for OSM object will probably fail. API base URL is: ''{0}''",baseUrl));
        }
        if (ret.startsWith("http://api.openstreetmap.org/")) {
            ret = ret.substring("http://api.openstreetmap.org/".length());
            ret = Main.OSM_WEBSITE + "/" + ret;
        }
        return ret;
    }

    /**
     * replies the base URL for browsing information about a user
     *
     * @return the base URL, i.e. http://www.openstreetmap.org/user
     */
    static public String getBaseUserUrl() {
        String baseUrl = Main.pref.get("osm-server.url", OsmApi.DEFAULT_API_URL);
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/user");
        if (ret.equals(baseUrl)) {
            Main.warn(tr("Unexpected format of API base URL. Redirection to user page for OSM user will probably fail. API base URL is: ''{0}''",baseUrl));
        }
        if (ret.startsWith("http://api.openstreetmap.org/")) {
            ret = ret.substring("http://api.openstreetmap.org/".length());
            ret = Main.OSM_WEBSITE + "/" + ret;
        }
        return ret;
    }

    public static boolean confirmLaunchMultiple(int numBrowsers) {
        String msg  = /* for correct i18n of plural forms - see #9110 */ trn(
                "You are about to launch {0} browser window.<br>"
                        + "This may both clutter your screen with browser windows<br>"
                        + "and take some time to finish.",
                "You are about to launch {0} browser windows.<br>"
                        + "This may both clutter your screen with browser windows<br>"
                        + "and take some time to finish.", numBrowsers, numBrowsers);
        msg = "<html>" + msg + "</html>";
        ButtonSpec[] spec = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Continue"),
                        ImageProvider.get("ok"),
                        trn("Click to continue and to open {0} browser", "Click to continue and to open {0} browsers", numBrowsers, numBrowsers),
                        null // no specific help topic
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to abort launching external browsers"),
                        null // no specific help topic
                )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                null,
                spec,
                spec[0],
                HelpUtil.ht("/WarningMessages#ToManyBrowsersToOpen")
        );
        return ret == 0;
    }

    protected void launchInfoBrowsersForSelectedPrimitives() {
        List<OsmPrimitive> primitivesToShow = new ArrayList<OsmPrimitive>(getCurrentDataSet().getAllSelected());

        // filter out new primitives which are not yet uploaded to the server
        //
        Iterator<OsmPrimitive> it = primitivesToShow.iterator();
        while(it.hasNext()) {
            if (it.next().isNew()) {
                it.remove();
            }
        }

        if (primitivesToShow.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least one already uploaded node, way, or relation."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // don't launch more than 10 browser instances / browser windows
        //
        int max = Math.min(10, primitivesToShow.size());
        if (primitivesToShow.size() > max && ! confirmLaunchMultiple(primitivesToShow.size()))
            return;
        for(int i = 0; i < max; i++) {
            OpenBrowser.displayUrl(createInfoUrl(primitivesToShow.get(i)));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        launchInfoBrowsersForSelectedPrimitives();
    }

    protected abstract String createInfoUrl(Object infoObject);

    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null && !getCurrentDataSet().getSelected().isEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
