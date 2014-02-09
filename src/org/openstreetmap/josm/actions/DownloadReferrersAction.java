// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
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
     * @param targetLayer  the target layer. Must not be null.
     * @param children the collection of child primitives.
     * @exception IllegalArgumentException thrown if targetLayer is null
     */
    public static void downloadReferrers(OsmDataLayer targetLayer, Collection<OsmPrimitive> children) throws IllegalArgumentException {
        if (children == null || children.isEmpty()) return;
        Main.worker.submit(new DownloadReferrersTask(targetLayer, children));
    }

    /**
     * Downloads the primitives referring to the primitives in <code>primitives</code>
     * into the target layer <code>targetLayer</code>.
     * Does nothing if primitives is null or empty.
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param children the collection of primitives, given as map of ids and types
     * @exception IllegalArgumentException thrown if targetLayer is null
     */
    public static void downloadReferrers(OsmDataLayer targetLayer, Map<Long, OsmPrimitiveType> children) throws IllegalArgumentException {
        if (children == null || children.isEmpty()) return;
        Main.worker.submit(new DownloadReferrersTask(targetLayer, children));
    }

    /**
     * Downloads the primitives referring to the primitive given by <code>id</code> and
     * <code>type</code>.
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param id the primitive id. id &gt; 0 required.
     * @param type the primitive type. type != null required
     * @exception IllegalArgumentException thrown if targetLayer is null
     * @exception IllegalArgumentException thrown if id &lt;= 0
     * @exception IllegalArgumentException thrown if type == null
     */
    public static void downloadReferrers(OsmDataLayer targetLayer, long id, OsmPrimitiveType type) throws IllegalArgumentException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Id > 0 required, got {0}", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        Main.worker.submit(new DownloadReferrersTask(targetLayer, id, type));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmDataLayer layer = Main.main.getEditLayer();
        if (layer == null)
            return;
        Collection<OsmPrimitive> primitives = layer.data.getSelected();
        downloadReferrers(layer,primitives);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
