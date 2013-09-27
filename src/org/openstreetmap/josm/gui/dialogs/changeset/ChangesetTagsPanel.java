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
 * This panel displays the tags of the currently selected changeset in the {@link ChangesetCacheManager}
 *
 */
public class ChangesetTagsPanel extends JPanel implements PropertyChangeListener{

    private TagEditorModel model;

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        model = new TagEditorModel();
        TagTable tblTags = new TagTable(model);
        tblTags.setEnabled(false);
        add(new JScrollPane(tblTags), BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code ChangesetTagsPanel}.
     */
    public ChangesetTagsPanel() {
        build();
    }

    protected void init(Changeset cs) {
        if (cs == null) {
            model.clear();
            return;
        }
        model.initFromTags(cs.getKeys());
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
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
