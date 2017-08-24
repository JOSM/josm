// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action loads the set of primitives referring to the current selection from the OSM server.
 * @since 1810
 */
public class DownloadReferrersAction extends JosmAction {

    /**
     * Constructs a new {@code DownloadReferrersAction}.
     */
    public DownloadReferrersAction() {
        super(tr("Download parent ways/relations..."), "download",
                tr("Download objects referring to one of the selected objects"),
                Shortcut.registerShortcut("file:downloadreferrers",
                        tr("File: {0}", tr("Download parent ways/relations...")), KeyEvent.VK_D, Shortcut.ALT_CTRL),
                true, "downloadreferrers", true);
        putValue("help", ht("/Action/DownloadParentWaysAndRelation"));
    }

    /**
     * Downloads the primitives referring to the primitives in <code>primitives</code>
     * into the target layer <code>targetLayer</code>.
     * Does nothing if primitives is null or empty.
     *
     * @param targetLayer the target layer. Must not be null.
     * @param children the collection of child primitives.
     * @throws IllegalArgumentException if targetLayer is null
     */
    public static void downloadReferrers(OsmDataLayer targetLayer, Collection<OsmPrimitive> children) {
        if (children == null || children.isEmpty())
            return;
        MainApplication.worker.submit(new DownloadReferrersTask(targetLayer, children));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmDataLayer layer = getLayerManager().getEditLayer();
        if (layer == null)
            return;
        Collection<OsmPrimitive> primitives = layer.data.getSelected();
        downloadReferrers(layer, primitives);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
