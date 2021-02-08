// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesTask;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action for downloading relations with members
 * @since 17485
 */
public class DownloadRelationAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>DownloadMembersAction</code>.
     */
    public DownloadRelationAction() {
        putValue(SHORT_DESCRIPTION, tr("Download relation with members"));
        putValue(NAME, tr("Download with members"));
        new ImageProvider("dialogs", "downloadincomplete").getResource().attachImageIcon(this, true);
        setHelpId(ht("/Dialog/RelationList#DownloadRelation"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !MainApplication.isDisplayingMapView())
            return;
        MainApplication.worker.submit(new DownloadPrimitivesTask(MainApplication.getLayerManager().getEditLayer(),
                new ArrayList<>(relations), true));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(canDownload());
    }
}
