// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class TagSettingsPanel extends JPanel implements TableModelListener {

    /** checkbox for selecting whether an atomic upload is to be used  */
    private final TagEditorPanel pnlTagEditor = new TagEditorPanel(null);
    /** the model for the changeset comment */
    private final ChangesetCommentModel changesetCommentModel;
    private final ChangesetCommentModel changesetSourceModel;
    /** tags that applied to uploaded changesets by default*/
    private final Map<String, String> defaultTags = new HashMap<String, String>();

    protected void build() {
        setLayout(new BorderLayout());
        add(pnlTagEditor, BorderLayout.CENTER);
    }

    /**
     * Creates a new panel
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @param changesetSourceModel the changeset source model. Must not be null.
     * @throws IllegalArgumentException thrown if {@code changesetCommentModel} is null
     */
    public TagSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        this.changesetCommentModel.addObserver(new ChangesetCommentObserver("comment"));
        this.changesetSourceModel.addObserver(new ChangesetCommentObserver("source"));
        build();
        pnlTagEditor.getModel().addTableModelListener(this);
    }

    protected void setProperty(String key, String value) {
        if (value == null) {
            value = "";
        }
        value = value.trim();
        String commentInTag = getTagEditorValue(key);
        if (value.equals(commentInTag))
            return;

        if (value.isEmpty()) {
            pnlTagEditor.getModel().delete(key);
            return;
        }
        TagModel tag = pnlTagEditor.getModel().get(key);
        if (tag == null) {
            tag = new TagModel(key, value);
            pnlTagEditor.getModel().add(tag);
        } else {
            pnlTagEditor.getModel().updateTagValue(tag, value);
        }
    }

    protected String getTagEditorValue(String key) {
        TagModel tag = pnlTagEditor.getModel().get(key);
        if (tag == null) return null;
        return tag.getValue();
    }

    public void initFromChangeset(Changeset cs) {
        Map<String,String> tags = getDefaultTags();
        if (cs != null) {
            tags.putAll(cs.getKeys());
        }
        if (tags.get("comment") == null) {
            tags.put("comment", getTagEditorValue("comment"));
        }
        if (tags.get("source") == null) {
            tags.put("source", getTagEditorValue("source"));
        }
        String agent = Version.getInstance().getAgentString(false);
        String created_by = tags.get("created_by");
        if (created_by == null || created_by.isEmpty()) {
            tags.put("created_by", agent);
        } else if (!created_by.contains(agent)) {
            tags.put("created_by", created_by + ";" + agent);
        }
        pnlTagEditor.getModel().initFromTags(tags);
    }

    /**
     * Replies the map with the current tags in the tag editor model.
     *
     * @return the map with the current tags in the tag editor model.
     */
    public Map<String,String> getTags(boolean keepEmpty) {
        return pnlTagEditor.getModel().getTags(keepEmpty);
    }

    public Map<String,String> getDefaultTags() {
        Map<String,String> tags = new HashMap<String, String>();
        tags.putAll(defaultTags);
        return tags;
    }

    public void setDefaultTags(Map<String, String> tags) {
        defaultTags.clear();
        defaultTags.putAll(tags);
        tableChanged(null);
    }

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
     *
     */
    class ChangesetCommentObserver implements Observer {

        private final String key;

        ChangesetCommentObserver(String key) {
            this.key = key;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (!(o instanceof ChangesetCommentModel)) return;
            String newValue = (String)arg;
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
