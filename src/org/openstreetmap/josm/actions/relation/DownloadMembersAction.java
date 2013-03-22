package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * The action for downloading members of relations
 */
public class DownloadMembersAction extends AbstractRelationAction {

    public DownloadMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download all members of the selected relations"));
        putValue(NAME, tr("Download members"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "downloadincomplete"));
        putValue("help", ht("/Dialog/RelationList#DownloadMembers"));
    }
    
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        Main.worker.submit(new DownloadRelationTask(relations, Main.map.mapView.getEditLayer()));
    }
}