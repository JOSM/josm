// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
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
     * @deprecated use {@link OpenBrowserAction#confirmLaunchMultiple(int)}
     */
    @Deprecated
    public static boolean confirmLaunchMultiple(int numBrowsers) {
        return OpenBrowserAction.confirmLaunchMultiple(numBrowsers);
    }

    protected void launchInfoBrowsersForSelectedPrimitivesAndNote() {
        List<IPrimitive> primitivesToShow = new ArrayList<>();
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        if (ds != null) {
            primitivesToShow.addAll(ds.getAllSelected());
        }

        Note noteToShow = MainApplication.isDisplayingMapView() ? MainApplication.getMap().noteDialog.getSelectedNote() : null;

        // filter out new primitives which are not yet uploaded to the server
        //
        primitivesToShow.removeIf(IPrimitive::isNew);

        if (primitivesToShow.isEmpty() && noteToShow == null) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Please select at least one already uploaded node, way, or relation."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // don't launch more than 10 browser instances / browser windows
        //
        int max = Math.min(10, primitivesToShow.size());
        if (primitivesToShow.size() > max && !OpenBrowserAction.confirmLaunchMultiple(primitivesToShow.size()))
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
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        setEnabled(ds != null && !ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
