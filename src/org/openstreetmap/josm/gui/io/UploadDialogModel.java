// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Utils;

/**
 * A model for the upload dialog
 *
 * @since 18173
 */
public class UploadDialogModel extends TagEditorModel {
    /** the "created_by" changeset OSM key */
    private static final String CREATED_BY = "created_by";
    /** the user-agent */
    private final String agent = Version.getInstance().getAgentString(false);
    /** whether to extract hashtags from comment */
    private final boolean hashtags = Config.getPref().getBoolean("upload.changeset.hashtags", true);

    /** a lock to prevent loops  */
    private boolean locked;

    @Override
    public void fireTableDataChanged() {
        if (!locked) {
            try {
                locked = true;
                // add "hashtags" if any
                if (hashtags) {
                    put("hashtags", findHashTags(getValue("comment")));
                }
                // add/update "created_by"
                final String createdBy = getValue(CREATED_BY);
                if (createdBy.isEmpty()) {
                    put(CREATED_BY, agent);
                } else if (!createdBy.contains(agent)) {
                    put(CREATED_BY, createdBy + ';' + agent);
                }
                super.fireTableDataChanged();
            } finally {
                locked = false;
            }
        }
    }

    /**
     * Get the value of a key.
     *
     * @param key The key to retrieve
     * @return The value (may be null)
     */
    public String getValue(String key) {
        TagModel tag = get(key);
        return tag == null ? "" : tag.getValue();
    }

    /**
     * Extracts the list of hashtags from the comment text.
     * @param comment The comment with the hashtags
     * @return the hashtags separated by ";" or null
     */
    String findHashTags(String comment) {
        String hashtags = String.join(";",
            Arrays.stream(comment.split("\\s", -1))
                .map(s -> Utils.strip(s, ",;"))
                .filter(s -> s.matches("#[a-zA-Z0-9][-_a-zA-Z0-9]+"))
                .collect(Collectors.toList()));
        return hashtags.isEmpty() ? null : hashtags;
    }

    /**
     * Returns the given comment with appended hashtags from dataset changeset tags, if not already present.
     * @param comment changeset comment. Can be null
     * @param dataSet optional dataset, which can contain hashtags in its changeset tags
     * @return comment with dataset changesets tags, if any, not duplicated
     */
    static String addHashTagsFromDataSet(String comment, DataSet dataSet) {
        StringBuilder result = comment == null ? new StringBuilder() : new StringBuilder(comment);
        if (dataSet != null) {
            String hashtags = dataSet.getChangeSetTags().get("hashtags");
            if (hashtags != null) {
                Set<String> sanitizedHashtags = new LinkedHashSet<>();
                for (String hashtag : hashtags.split(";", -1)) {
                    sanitizedHashtags.add(hashtag.startsWith("#") ? hashtag : "#" + hashtag);
                }
                if (!sanitizedHashtags.isEmpty()) {
                    result.append(' ').append(String.join(" ", sanitizedHashtags));
                }
            }
        }
        return result.toString();
    }

    /**
     * Inserts/updates/deletes a tag.
     *
     * Existing keys are updated. Others are added. A value of {@code null}
     * deletes the key.
     *
     * @param key The key of the tag to insert.
     * @param value The value of the tag to insert.
     */
    private void doPut(String key, String value) {
        List<TagModel> l = tags.stream().filter(tm -> tm.getName().equals(key)).collect(Collectors.toList());
        if (!l.isEmpty()) {
            if (value != null)
                l.get(0).setValue(value);
            else
                tags.remove(l.get(0));
        } else if (value != null) {
            tags.add(new TagModel(key, value));
        }
    }

    /**
     * Inserts/updates/deletes a tag.
     *
     * Existing keys are updated. Others are added. A value of {@code null}
     * deletes the key.
     *
     * @param key The key of the tag to insert.
     * @param value The value of the tag to insert.
     */
    public void put(String key, String value) {
        commitPendingEdit();
        doPut(key, value);
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * Inserts/updates/deletes all tags from {@code map}.
     *
     * Existing keys are updated. Others are added. A value of {@code null}
     * deletes the key.
     *
     * @param map a map of tags to insert or update
     */
    public void putAll(Map<String, String> map) {
        commitPendingEdit();
        map.forEach((key, value) -> doPut(key, value));
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * Inserts all tags from a {@code DataSet}.
     *
     * @param dataSet The DataSet to take tags from.
     */
    public void putAll(DataSet dataSet) {
        if (dataSet != null) {
            putAll(dataSet.getChangeSetTags());
            put("comment", addHashTagsFromDataSet(getValue("comment"), dataSet));
        }
    }
}
