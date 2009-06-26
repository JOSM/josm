// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
    protected String getBaseURL() {
        String baseUrl = Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api");
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/browse");
        if (ret.equals(baseUrl)) {
            System.out.println(tr("WARNING: unexpected format of API base URL. Redirection to history page for OSM primitive will probably fail. API base URL is: ''{0}''",baseUrl));
        }
        return ret;
    }

    protected void launchBrowser() {
        ArrayList<OsmPrimitive> primitivesToShow = new ArrayList<OsmPrimitive>(Main.ds.getSelected());

        // filter out new primitives which are not yet uploaded to the server
        //
        Iterator<OsmPrimitive> it = primitivesToShow.iterator();
        while(it.hasNext()) {
            if (it.next().id == 0) {
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
        if (max < primitivesToShow.size()) {
            System.out.println(tr("WARNING: launching browser windows for the first {0} of {1} selected primitives only", 10, primitivesToShow.size()));
        }
        for(int i = 0; i < max; i++) {
            OpenBrowser.displayUrl(
                    createInfoUrl(primitivesToShow.get(i))
            );
        }
    }

    public void actionPerformed(ActionEvent e) {
        launchBrowser();
    }

    protected abstract String createInfoUrl(OsmPrimitive primitive);
}
