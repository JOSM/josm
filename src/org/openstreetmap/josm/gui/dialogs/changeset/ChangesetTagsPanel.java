// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.TagTable;

/**
 * This panel displays the tags of the currently selected changeset in the {@see ChangesetCacheManager}
 *
 */
public class ChangesetTagsPanel extends JPanel implements PropertyChangeListener{

    private TagTable tblTags;
    private TagEditorModel model;

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        tblTags = new TagTable(model = new TagEditorModel());
        tblTags.setEnabled(false);
        add(new JScrollPane(tblTags), BorderLayout.CENTER);
    }

    public ChangesetTagsPanel() {
        build();
    }

    protected void init(Changeset cs) {
        model.clear();
        if (cs == null)
            return;
        model.initFromTags(cs.getKeys());
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    public void propertyChange(PropertyChangeEvent evt) {
        if (!evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        Changeset cs = (Changeset)evt.getNewValue();
        if (cs == null) {
            model.clear();
        } else {
            model.initFromPrimitive(cs);
        }
    }
}
