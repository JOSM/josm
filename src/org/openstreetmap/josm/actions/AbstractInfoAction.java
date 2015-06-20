// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class AbstractInfoAction extends JosmAction {

    public AbstractInfoAction(boolean installAdapters) {
        super(installAdapters);
    }

    public AbstractInfoAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register,
            String toolbarId, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbarId, installAdapters);
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
                        trn("Click to continue and to open {0} browser", "Click to continue and to open {0} browsers",
                                numBrowsers, numBrowsers),
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

    protected void launchInfoBrowsersForSelectedPrimitivesAndNote() {
        List<OsmPrimitive> primitivesToShow = new ArrayList<>();
        if (getCurrentDataSet() != null) {
            primitivesToShow.addAll(getCurrentDataSet().getAllSelected());
        }

        Note noteToShow = Main.isDisplayingMapView() ? Main.map.noteDialog.getSelectedNote() : null;

        // filter out new primitives which are not yet uploaded to the server
        //
        Iterator<OsmPrimitive> it = primitivesToShow.iterator();
        while(it.hasNext()) {
            if (it.next().isNew()) {
                it.remove();
            }
        }

        if (primitivesToShow.isEmpty() && noteToShow == null) {
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
        if (primitivesToShow.size() > max && !confirmLaunchMultiple(primitivesToShow.size()))
            return;
        for (int i = 0; i < max; i++) {
            launchInfoBrowser(primitivesToShow.get(i));
        }

        if (noteToShow != null) {
            launchInfoBrowser(noteToShow);
        }
    }

    protected final void launchInfoBrowser(Object o) {
        String url = createInfoUrl(o);
        if (url != null) {
            String result = OpenBrowser.displayUrl(url);
            if (result != null) {
                Main.warn(result);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        launchInfoBrowsersForSelectedPrimitivesAndNote();
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
