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
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class TagSettingsPanel extends JPanel implements TableModelListener {

    /** checkbox for selecting whether an atomic upload is to be used  */
    private TagEditorPanel pnlTagEditor;
    /** the model for the changeset comment */
    private ChangesetCommentModel changesetCommentModel;
    /** tags that applied to uploaded changesets by default*/
    private Map<String, String> defaultTags = new HashMap<String, String>();

    protected void build() {
        setLayout(new BorderLayout());
        add(pnlTagEditor = new TagEditorPanel(null), BorderLayout.CENTER);
    }

    /**
     * Creates a new panel
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @throws IllegalArgumentException thrown if {@code changesetCommentModel} is null
     */
    public TagSettingsPanel(ChangesetCommentModel changesetCommentModel) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetCommentModel.addObserver(new ChangesetCommentObserver());
        build();
        pnlTagEditor.getModel().addTableModelListener(this);
    }

    /**
     * Replies the default value for "created_by"
     *
     * @return the default value for "created_by"
     */
    public static String getDefaultCreatedBy() {
        Object ua = System.getProperties().get("http.agent");
        return(ua == null) ? "JOSM" : ua.toString();
    }

    protected void setUploadComment(String comment) {
        if (comment == null) {
            comment = "";
        }
        comment  = comment.trim();
        String commentInTag = getUploadComment();
        if (comment.equals(commentInTag))
            return;

        if (comment.equals("")) {
            pnlTagEditor.getModel().delete("comment");
            return;
        }
        TagModel tag = pnlTagEditor.getModel().get("comment");
        if (tag == null) {
            tag = new TagModel("comment", comment);
            pnlTagEditor.getModel().add(tag);
        } else {
            pnlTagEditor.getModel().updateTagValue(tag, comment);
        }
    }

    protected String getUploadComment() {
        TagModel tag = pnlTagEditor.getModel().get("comment");
        if (tag == null) return null;
        return tag.getValue();
    }

    public void initFromChangeset(Changeset cs) {
        String currentComment = getUploadComment();
        Map<String,String> tags = getDefaultTags();
        if (cs != null) {
            tags.putAll(cs.getKeys());
        }
        if (tags.get("comment") == null) {
            tags.put("comment", currentComment);
        }
        String created_by = tags.get("created_by");
        if (created_by == null || "".equals(created_by)) {
            tags.put("created_by", getDefaultCreatedBy());
        } else if (!created_by.contains(getDefaultCreatedBy())) {
            tags.put("created_by", created_by + ";" + getDefaultCreatedBy());
        }
        pnlTagEditor.getModel().initFromTags(tags);
    }

    /**
     * Replies the map with the current tags in the tag editor model.
     *
     * @return the map with the current tags in the tag editor model.
     */
    public Map<String,String> getTags() {
        return pnlTagEditor.getModel().getTags();
    }

    public Map<String,String> getDefaultTags() {
        Map<String,String> tags = new HashMap<String, String>();
        tags.putAll(defaultTags);
        return tags;
    }

    public void setDefaultTags(Map<String, String> tags) {
        defaultTags.clear();
        defaultTags.putAll(tags);
    }

    public void startUserInput() {
        pnlTagEditor.initAutoCompletion(Main.main.getEditLayer());
    }

    /* -------------------------------------------------------------------------- */
    /* Interface TableChangeListener                                              */
    /* -------------------------------------------------------------------------- */
    public void tableChanged(TableModelEvent e) {
        String uploadComment = getUploadComment();
        changesetCommentModel.setComment(uploadComment);
    }

    /**
     * Observes the changeset comment model and keeps the tag editor in sync
     * with the current changeset comment
     *
     */
    class ChangesetCommentObserver implements Observer {
        public void update(Observable o, Object arg) {
            if (!(o instanceof ChangesetCommentModel)) return;
            String newValue = (String)arg;
            String oldValue = getUploadComment();
            if (oldValue == null) {
                oldValue = "";
            }
            if (!oldValue.equals(newValue)) {
                setUploadComment((String)arg);
            }
        }
    }
}
