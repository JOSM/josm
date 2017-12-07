// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.Optional;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.spi.preferences.Config;
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
    private final transient ChangesetReviewModel changesetReviewModel;

    /**
     * Creates a new panel
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @param changesetSourceModel the changeset source model. Must not be null.
     * @param changesetReviewModel the model for the changeset review. Must not be null.
     * @throws IllegalArgumentException if {@code changesetCommentModel} is null
     * @since 12719 (signature)
     */
    public TagSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel,
            ChangesetReviewModel changesetReviewModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        CheckParameterUtil.ensureParameterNotNull(changesetReviewModel, "changesetReviewModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        this.changesetReviewModel = changesetReviewModel;
        changesetCommentModel.addChangeListener(new ChangesetCommentChangeListener("comment", "hashtags"));
        changesetSourceModel.addChangeListener(new ChangesetCommentChangeListener("source"));
        changesetReviewModel.addChangeListener(new ChangesetReviewChangeListener());
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
        forceCommentFieldReload();
        return pnlTagEditor.getModel().getTags(keepEmpty);
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        pnlTagEditor.initAutoCompletion(MainApplication.getLayerManager().getEditLayer());
    }

    /* -------------------------------------------------------------------------- */
    /* Interface TableChangeListener                                              */
    /* -------------------------------------------------------------------------- */
    @Override
    public void tableChanged(TableModelEvent e) {
        changesetCommentModel.setComment(getTagEditorValue("comment"));
        changesetSourceModel.setComment(getTagEditorValue("source"));
        changesetReviewModel.setReviewRequested("yes".equals(getTagEditorValue("review_requested")));
    }

    /**
     * Force update the fields if the user is currently changing them. See #5676
     */
    private void forceCommentFieldReload() {
        setProperty("comment", changesetCommentModel.getComment());
        setProperty("source", changesetSourceModel.getComment());
        setProperty("review_requested", changesetReviewModel.isReviewRequested() ? "yes" : null);
    }

    /**
     * Observes the changeset comment model and keeps the tag editor in sync
     * with the current changeset comment
     */
    class ChangesetCommentChangeListener implements ChangeListener {

        private final String key;
        private final String hashtagsKey;

        ChangesetCommentChangeListener(String key) {
            this(key, null);
        }

        ChangesetCommentChangeListener(String key, String hashtagsKey) {
            this.key = key;
            this.hashtagsKey = hashtagsKey;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof ChangesetCommentModel) {
                ChangesetCommentModel model = ((ChangesetCommentModel) e.getSource());
                String newValue = model.getComment();
                String oldValue = Optional.ofNullable(getTagEditorValue(key)).orElse("");
                if (!oldValue.equals(newValue)) {
                    setProperty(key, newValue);
                    if (hashtagsKey != null && Config.getPref().getBoolean("upload.changeset.hashtags", true)) {
                        String newHashTags = String.join(";", model.findHashTags());
                        String oldHashTags = Optional.ofNullable(getTagEditorValue(hashtagsKey)).orElse("");
                        if (!oldHashTags.equals(newHashTags)) {
                            setProperty(hashtagsKey, newHashTags);
                        }
                    }
                }
            }
        }
    }

    /**
     * Observes the changeset review model and keeps the tag editor in sync
     * with the current changeset review request
     */
    class ChangesetReviewChangeListener implements ChangeListener {

        private static final String KEY = "review_requested";

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof ChangesetReviewModel) {
                boolean newState = ((ChangesetReviewModel) e.getSource()).isReviewRequested();
                boolean oldState = "yes".equals(Optional.ofNullable(getTagEditorValue(KEY)).orElse(""));
                if (oldState != newState) {
                    setProperty(KEY, newState ? "yes" : null);
                }
            }
        }
    }
}
