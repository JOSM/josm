// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action synchronizes a set of primitives with their state on the server.
 * @since 2682
 */
public class UpdateModifiedAction extends UpdateSelectionAction {

    /**
     * Constructs a new {@code UpdateModifiedAction}.
     */
    public UpdateModifiedAction() {
        super(tr("Update modified"), "updatedata",
                tr("Updates the currently modified objects from the server (re-downloads data)"),
                Shortcut.registerShortcut("file:updatemodified",
                        tr("File: {0}", tr("Update modified")), KeyEvent.VK_M,
                        Shortcut.ALT_CTRL),
                        true, "updatemodified");
        putValue("help", ht("/Action/UpdateModified"));
    }

    // FIXME: overrides the behaviour of UpdateSelectionAction. Doesn't update
    // the enabled state based on the current selection because it doesn't depend on it.
    // The action should be enabled/disabled based on whether there is a least
    // one modified object in the current dataset. Unfortunately, there is no
    // efficient way to find out here. getDataSet().allModifiedPrimitives() is
    // too heavy weight because it loops over the whole dataset.
    // Perhaps this action should  be a DataSetListener? Or it could listen to the
    // REQUIRES_SAVE_TO_DISK_PROP and REQUIRES_UPLOAD_TO_SERVER_PROP properties
    // in the OsmLayer?
    //
    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        setEnabled(ds != null && !DownloadPolicy.BLOCKED.equals(ds.getDownloadPolicy())
                && !NetworkManager.isOffline(OnlineResource.OSM_API));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        // Do nothing
    }

    @Override
    public Collection<OsmPrimitive> getData() {
        DataSet ds = getLayerManager().getEditDataSet();
        return ds == null ? Collections.<OsmPrimitive>emptyList() : ds.allModifiedPrimitives();
    }
}
