// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abstract base class for info actions, opening an URL describing a particular object.
 * @since 1697
 */
public abstract class AbstractInfoAction extends JosmAction {

    /**
     * Constructs a new {@code AbstractInfoAction}.
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public AbstractInfoAction(boolean installAdapters) {
        super(installAdapters);
    }

    /**
     * Constructs a new {@code AbstractInfoAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param register register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public AbstractInfoAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register,
            String toolbarId, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbarId, installAdapters);
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
        return 0 == HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                new StringBuilder(msg).insert(0, "<html>").append("</html>").toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                null,
                spec,
                spec[0],
                HelpUtil.ht("/WarningMessages#ToManyBrowsersToOpen")
        );
    }

    protected void launchInfoBrowsersForSelectedPrimitivesAndNote() {
        List<OsmPrimitive> primitivesToShow = new ArrayList<>();
        DataSet ds = getLayerManager().getActiveDataSet();
        if (ds != null) {
            primitivesToShow.addAll(ds.getAllSelected());
        }

        Note noteToShow = MainApplication.isDisplayingMapView() ? MainApplication.getMap().noteDialog.getSelectedNote() : null;

        // filter out new primitives which are not yet uploaded to the server
        //
        primitivesToShow.removeIf(AbstractPrimitive::isNew);

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
                Logging.warn(result);
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
        DataSet ds = getLayerManager().getActiveDataSet();
        setEnabled(ds != null && !ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
