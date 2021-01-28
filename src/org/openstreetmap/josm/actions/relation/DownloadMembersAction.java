// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesTask;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action for downloading members of relations
 * @since 5793
 */
public class DownloadMembersAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>DownloadMembersAction</code>.
     */
    public DownloadMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download all members of the selected relations"));
        putValue(NAME, tr("Download members"));
        new ImageProvider("dialogs", "downloadincomplete").getResource().attachImageIcon(this, true);
        setHelpId(ht("/Dialog/RelationList#DownloadMembers"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !MainApplication.isDisplayingMapView()) return;
        List<PrimitiveId> members = relations.stream()
                .flatMap(r -> r.getMemberPrimitivesList().stream().filter(osm -> !osm.isNew()).map(IPrimitive::getOsmPrimitiveId))
                .distinct()
                .collect(Collectors.toList());

        MainApplication.worker.submit(new DownloadPrimitivesTask(MainApplication.getLayerManager().getEditLayer(), members, false));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(canDownload());
    }
}
