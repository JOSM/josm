// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Tag settings panel of upload dialog.
 * @since 2599
 */
public class TagSettingsPanel extends JPanel implements TableModelListener {

    /** checkbox for selecting whether an atomic upload is to be used  */
    private final TagEditorPanel pnlTagEditor = new TagEditorPanel(null, null, Changeset.MAX_CHANGESET_TAG_LENGTH);
    /** the model for the changeset comment */
    private final transient ChangesetCommentModel changesetCommentModel;
    private final transient ChangesetCommentModel changesetSourceModel;

    /**
     * Creates a new panel
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @param changesetSourceModel the changeset source model. Must not be null.
     * @throws IllegalArgumentException if {@code changesetCommentModel} is null
     */
    public TagSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        this.changesetCommentModel.addObserver(new ChangesetCommentObserver("comment"));
        this.changesetSourceModel.addObserver(new ChangesetCommentObserver("source"));
        build();
        pnlTagEditor.getModel().addTableModelListener(this);
    }

    protected void build() {
        setLayout(new BorderLayout());
        add(pnlTagEditor, BorderLayout.CENTER);
    }

    protected void setProperty(String key, String value) {
        String val = (value == null ? "" : value).trim();
        String commentInTag = getTagEditorValue(key);
        if (val.equals(commentInTag))
            return;

        if (val.isEmpty()) {
            pnlTagEditor.getModel().delete(key);
            return;
        }
        TagModel tag = pnlTagEditor.getModel().get(key);
        if (tag == null) {
            tag = new TagModel(key, val);
            pnlTagEditor.getModel().add(tag);
        } else {
            pnlTagEditor.getModel().updateTagValue(tag, val);
        }
    }

    protected String getTagEditorValue(String key) {
        TagModel tag = pnlTagEditor.getModel().get(key);
        return tag == null ? null : tag.getValue();
    }

    /**
     * Initialize panel from the given tags.
     * @param tags the tags used to initialize the panel
     */
    public void initFromTags(Map<String, String> tags) {
        pnlTagEditor.getModel().initFromTags(tags);
    }

    /**
     * Replies the map with the current tags in the tag editor model.
     * @param keepEmpty {@code true} to keep empty tags
     * @return the map with the current tags in the tag editor model.
     */
    public Map<String, String> getTags(boolean keepEmpty) {
        return pnlTagEditor.getModel().getTags(keepEmpty);
    }

    /**
     * @return an empty map
     * @deprecated No longer supported, returns an empty map
     */
    @Deprecated
    public Map<String, String> getDefaultTags() {
        return Collections.emptyMap();
    }

    /**
     * @param tags ignored
     * @deprecated No longer supported, does nothing; use {@link UploadDialog#setChangesetTags(DataSet)} instead!
     */
    @Deprecated
    public void setDefaultTags(Map<String, String> tags) {
        // Deprecated
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        pnlTagEditor.initAutoCompletion(Main.main.getEditLayer());
    }

    /* -------------------------------------------------------------------------- */
    /* Interface TableChangeListener                                              */
    /* -------------------------------------------------------------------------- */
    @Override
    public void tableChanged(TableModelEvent e) {
        changesetCommentModel.setComment(getTagEditorValue("comment"));
        changesetSourceModel.setComment(getTagEditorValue("source"));
    }

    /**
     * Observes the changeset comment model and keeps the tag editor in sync
     * with the current changeset comment
     */
    class ChangesetCommentObserver implements Observer {

        private final String key;

        ChangesetCommentObserver(String key) {
            this.key = key;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (o instanceof ChangesetCommentModel) {
                String newValue = (String) arg;
                String oldValue = getTagEditorValue(key);
                if (oldValue == null) {
                    oldValue = "";
                }
                if (!oldValue.equals(newValue)) {
                    setProperty(key, (String) arg);
                }
            }
        }
    }
}
