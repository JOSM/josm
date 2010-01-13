// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class AbstractInfoAction extends JosmAction {

    public AbstractInfoAction() {
        super();
    }

    public AbstractInfoAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
        super(name, iconName, tooltip, shortcut, register);
    }

    /**
     * replies the base URL for browsing information about about a primitive
     *
     * @return the base URL, i.e. http://api.openstreetmap.org/browse
     */
    static public String getBaseBrowseUrl() {
        String baseUrl = Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api");
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/browse");
        if (ret.equals(baseUrl)) {
            System.out.println(tr("WARNING: unexpected format of API base URL. Redirection to info or history page for OSM primitive will probably fail. API base URL is: ''{0}''",baseUrl));
        }
        if (ret.startsWith("http://api.openstreetmap.org/")) {
            ret = ret.substring("http://api.openstreetmap.org/".length());
            ret = "http://www.openstreetmap.org/" + ret;
        }
        return ret;
    }

    /**
     * replies the base URL for browsing information about a user
     *
     * @return the base URL, i.e. http://ww.openstreetmap.org/user
     */
    static public String getBaseUserUrl() {
        String baseUrl = Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api");
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/user");
        if (ret.equals(baseUrl)) {
            System.out.println(tr("WARNING: unexpected format of API base URL. Redirection to user page for OSM user will probably fail. API base URL is: ''{0}''",baseUrl));
        }
        if (ret.startsWith("http://api.openstreetmap.org/")) {
            ret = ret.substring("http://api.openstreetmap.org/".length());
            ret = "http://www.openstreetmap.org/" + ret;
        }
        return ret;
    }

    protected void launchBrowser(URL url) {
        OpenBrowser.displayUrl(
                url.toString()
        );
    }

    protected void launchBrowser(String url) {
        OpenBrowser.displayUrl(
                url
        );
    }

    public static boolean confirmLaunchMultiple(int numBrowsers) {
        String msg  = tr(
                "You are about to launch {0} browser windows.<br>"
                + "This may both clutter your screen with browser windows<br>"
                + "and take some time to finish.", numBrowsers);
        msg = "<html>" + msg + "</html>";
        ButtonSpec[] spec = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Continue"),
                        ImageProvider.get("ok"),
                        tr("Click to continue and to open {0} browsers", numBrowsers),
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
        ArrayList<OsmPrimitive> primitivesToShow = new ArrayList<OsmPrimitive>(getCurrentDataSet().getSelected());

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
            launchBrowser(createInfoUrl(primitivesToShow.get(i)));
        }
    }

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
