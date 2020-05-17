// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * TagEditorModel is a table model to use with {@link TagEditorPanel}.
 * @since 1762
 */
public class TagEditorModel extends AbstractTableModel {
    /**
     * The dirty property. It is set whenever this table was changed
     */
    public static final String PROP_DIRTY = TagEditorModel.class.getName() + ".dirty";

    /** the list holding the tags */
    protected final transient List<TagModel> tags = new ArrayList<>();

    /** indicates whether the model is dirty */
    private boolean dirty;
    private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);

    private final DefaultListSelectionModel rowSelectionModel;
    private final DefaultListSelectionModel colSelectionModel;

    private transient OsmPrimitive primitive;

    private EndEditListener endEditListener;

    /**
     * Creates a new tag editor model. Internally allocates two selection models
     * for row selection and column selection.
     *
     * To create a {@link javax.swing.JTable} with this model:
     * <pre>
     *    TagEditorModel model = new TagEditorModel();
     *    TagTable tbl  = new TagTabel(model);
     * </pre>
     *
     * @see #getRowSelectionModel()
     * @see #getColumnSelectionModel()
     */
    public TagEditorModel() {
        this(new DefaultListSelectionModel(), new DefaultListSelectionModel());
    }

    /**
     * Creates a new tag editor model.
     *
     * @param rowSelectionModel the row selection model. Must not be null.
     * @param colSelectionModel the column selection model. Must not be null.
     * @throws IllegalArgumentException if {@code rowSelectionModel} is null
     * @throws IllegalArgumentException if {@code colSelectionModel} is null
     */
    public TagEditorModel(DefaultListSelectionModel rowSelectionModel, DefaultListSelectionModel colSelectionModel) {
        CheckParameterUtil.ensureParameterNotNull(rowSelectionModel, "rowSelectionModel");
        CheckParameterUtil.ensureParameterNotNull(colSelectionModel, "colSelectionModel");
        this.rowSelectionModel = rowSelectionModel;
        this.colSelectionModel = colSelectionModel;
    }

    /**
     * Adds property change listener.
     * @param listener property change listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Replies the row selection model used by this tag editor model
     *
     * @return the row selection model used by this tag editor model
     */
    public DefaultListSelectionModel getRowSelectionModel() {
        return rowSelectionModel;
    }

    /**
     * Replies the column selection model used by this tag editor model
     *
     * @return the column selection model used by this tag editor model
     */
    public DefaultListSelectionModel getColumnSelectionModel() {
        return colSelectionModel;
    }

    /**
     * Removes property change listener.
     * @param listener property change listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propChangeSupport.removePropertyChangeListener(listener);
    }

    protected void fireDirtyStateChanged(final boolean oldValue, final boolean newValue) {
        propChangeSupport.firePropertyChange(PROP_DIRTY, oldValue, newValue);
    }

    protected void setDirty(boolean newValue) {
        boolean oldValue = dirty;
        dirty = newValue;
        if (oldValue != newValue) {
            fireDirtyStateChanged(oldValue, newValue);
        }
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return tags.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= getRowCount())
            throw new IndexOutOfBoundsException("unexpected rowIndex: rowIndex=" + rowIndex);

        return tags.get(rowIndex);
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        TagModel tag = get(row);
        if (tag != null) {
            switch(col) {
            case 0:
                updateTagName(tag, (String) value);
                break;
            case 1:
                String v = (String) value;
                if ((tag.getValueCount() > 1 && !v.isEmpty()) || tag.getValueCount() <= 1) {
                    updateTagValue(tag, v);
                }
                break;
            default: // Do nothing
            }
        }
    }

    /**
     * removes all tags in the model
     */
    public void clear() {
        commitPendingEdit();
        boolean wasEmpty = tags.isEmpty();
        tags.clear();
        if (!wasEmpty) {
            setDirty(true);
            fireTableDataChanged();
        }
    }

    /**
     * adds a tag to the model
     *
     * @param tag the tag. Must not be null.
     *
     * @throws IllegalArgumentException if tag is null
     */
    public void add(TagModel tag) {
        commitPendingEdit();
        CheckParameterUtil.ensureParameterNotNull(tag, "tag");
        tags.add(tag);
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * Add a tag at the beginning of the table.
     *
     * @param tag The tag to add
     *
     * @throws IllegalArgumentException if tag is null
     *
     * @see #add(TagModel)
     */
    public void prepend(TagModel tag) {
        commitPendingEdit();
        CheckParameterUtil.ensureParameterNotNull(tag, "tag");
        tags.add(0, tag);
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * adds a tag given by a name/value pair to the tag editor model.
     *
     * If there is no tag with name <code>name</code> yet, a new {@link TagModel} is created
     * and append to this model.
     *
     * If there is a tag with name <code>name</code>, <code>value</code> is merged to the list
     * of values for this tag.
     *
     * @param name the name; converted to "" if null
     * @param value the value; converted to "" if null
     */
    public void add(String name, String value) {
        commitPendingEdit();
        String key = (name == null) ? "" : name;
        String val = (value == null) ? "" : value;

        TagModel tag = get(key);
        if (tag == null) {
            tag = new TagModel(key, val);
            int index = tags.size();
            while (index >= 1 && tags.get(index - 1).getName().isEmpty() && tags.get(index - 1).getValue().isEmpty()) {
                index--; // If last line(s) is empty, add new tag before it
            }
            tags.add(index, tag);
        } else {
            tag.addValue(val);
        }
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * replies the tag with name <code>name</code>; null, if no such tag exists
     * @param name the tag name
     * @return the tag with name <code>name</code>; null, if no such tag exists
     */
    public TagModel get(String name) {
        String key = (name == null) ? "" : name;
        return tags.stream().filter(tag -> tag.getName().equals(key)).findFirst().orElse(null);
    }

    /**
     * Gets a tag row
     * @param idx The index of the row
     * @return The tag model for that row
     */
    public TagModel get(int idx) {
        return idx >= tags.size() ? null : tags.get(idx);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        // all cells are editable
        return true;
    }

    /**
     * deletes the names of the tags given by tagIndices
     *
     * @param tagIndices a list of tag indices
     */
    public void deleteTagNames(int... tagIndices) {
        if (tags == null)
            return;
        commitPendingEdit();
        for (int tagIdx : tagIndices) {
            TagModel tag = tags.get(tagIdx);
            if (tag != null) {
                tag.setName("");
            }
        }
        fireTableDataChanged();
        setDirty(true);
    }

    /**
     * deletes the values of the tags given by tagIndices
     *
     * @param tagIndices the lit of tag indices
     */
    public void deleteTagValues(int... tagIndices) {
        if (tags == null)
            return;
        commitPendingEdit();
        for (int tagIdx : tagIndices) {
            TagModel tag = tags.get(tagIdx);
            if (tag != null) {
                tag.setValue("");
            }
        }
        fireTableDataChanged();
        setDirty(true);
    }

    /**
     * Deletes all tags with name <code>name</code>
     *
     * @param name the name. Ignored if null.
     */
    public void delete(String name) {
        commitPendingEdit();
        if (name == null)
            return;
        Iterator<TagModel> it = tags.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            TagModel tm = it.next();
            if (tm.getName().equals(name)) {
                changed = true;
                it.remove();
            }
        }
        if (changed) {
            fireTableDataChanged();
            setDirty(true);
        }
    }

    /**
     * deletes the tags given by tagIndices
     *
     * @param tagIndices the list of tag indices
     */
    public void deleteTags(int... tagIndices) {
        if (tags == null)
            return;
        commitPendingEdit();
        List<TagModel> toDelete = Arrays.stream(tagIndices).mapToObj(tags::get).filter(Objects::nonNull).collect(Collectors.toList());
        toDelete.forEach(tags::remove);
        fireTableDataChanged();
        setDirty(true);
    }

    /**
     * creates a new tag and appends it to the model
     */
    public void appendNewTag() {
        TagModel tag = new TagModel();
        tags.add(tag);
        fireTableDataChanged();
    }

    /**
     * makes sure the model includes at least one (empty) tag
     */
    public void ensureOneTag() {
        if (tags.isEmpty()) {
            appendNewTag();
        }
    }

    /**
     * initializes the model with the tags of an OSM primitive
     *
     * @param primitive the OSM primitive
     */
    public void initFromPrimitive(Tagged primitive) {
        commitPendingEdit();
        this.tags.clear();
        for (String key : primitive.keySet()) {
            String value = primitive.get(key);
            this.tags.add(new TagModel(key, value));
        }
        sort();
        TagModel tag = new TagModel();
        tags.add(tag);
        setDirty(false);
        fireTableDataChanged();
    }

    /**
     * Initializes the model with the tags of an OSM primitive
     *
     * @param tags the tags of an OSM primitive
     */
    public void initFromTags(Map<String, String> tags) {
        commitPendingEdit();
        this.tags.clear();
        for (Entry<String, String> entry : tags.entrySet()) {
            this.tags.add(new TagModel(entry.getKey(), entry.getValue()));
        }
        sort();
        TagModel tag = new TagModel();
        this.tags.add(tag);
        setDirty(false);
    }

    /**
     * Initializes the model with the tags in a tag collection. Removes
     * all tags if {@code tags} is null.
     *
     * @param tags the tags
     */
    public void initFromTags(TagCollection tags) {
        commitPendingEdit();
        this.tags.clear();
        if (tags == null) {
            setDirty(false);
            return;
        }
        for (String key : tags.getKeys()) {
            String value = tags.getJoinedValues(key);
            this.tags.add(new TagModel(key, value));
        }
        sort();
        // add an empty row
        TagModel tag = new TagModel();
        this.tags.add(tag);
        setDirty(false);
    }

    /**
     * applies the current state of the tag editor model to a primitive
     *
     * @param primitive the primitive
     *
     */
    public void applyToPrimitive(Tagged primitive) {
        primitive.setKeys(applyToTags(false));
    }

    /**
     * applies the current state of the tag editor model to a map of tags
     * @param keepEmpty {@code true} to keep empty tags
     *
     * @return the map of key/value pairs
     */
    private Map<String, String> applyToTags(boolean keepEmpty) {
        // TagMap preserves the order of tags.
        TagMap result = new TagMap();
        for (TagModel tag: this.tags) {
            // tag still holds an unchanged list of different values for the same key.
            // no property change command required
            if (tag.getValueCount() > 1) {
                continue;
            }

            // tag name holds an empty key. Don't apply it to the selection.
            if (!keepEmpty && (tag.getName().trim().isEmpty() || tag.getValue().trim().isEmpty())) {
                continue;
            }
            result.put(tag.getName().trim(), tag.getValue().trim());
        }
        return result;
    }

    /**
     * Returns tags, without empty ones.
     * @return not-empty tags
     */
    public Map<String, String> getTags() {
        return getTags(false);
    }

    /**
     * Returns tags.
     * @param keepEmpty {@code true} to keep empty tags
     * @return tags
     */
    public Map<String, String> getTags(boolean keepEmpty) {
        return applyToTags(keepEmpty);
    }

    /**
     * Replies the tags in this tag editor model as {@link TagCollection}.
     *
     * @return the tags in this tag editor model as {@link TagCollection}
     */
    public TagCollection getTagCollection() {
        return TagCollection.from(getTags());
    }

    /**
     * checks whether the tag model includes a tag with a given key
     *
     * @param key  the key
     * @return true, if the tag model includes the tag; false, otherwise
     */
    public boolean includesTag(String key) {
        return key != null && tags.stream().anyMatch(tag -> tag.getName().equals(key));
    }

    protected Command createUpdateTagCommand(Collection<OsmPrimitive> primitives, TagModel tag) {

        // tag still holds an unchanged list of different values for the same key.
        // no property change command required
        if (tag.getValueCount() > 1)
            return null;

        // tag name holds an empty key. Don't apply it to the selection.
        //
        if (tag.getName().trim().isEmpty())
            return null;

        return new ChangePropertyCommand(primitives, tag.getName(), tag.getValue());
    }

    protected Command createDeleteTagsCommand(Collection<OsmPrimitive> primitives) {

        List<String> currentkeys = getKeys();
        List<Command> commands = new ArrayList<>();

        for (OsmPrimitive prim : primitives) {
            for (String oldkey : prim.keySet()) {
                if (!currentkeys.contains(oldkey)) {
                    commands.add(new ChangePropertyCommand(prim, oldkey, null));
                }
            }
        }

        return commands.isEmpty() ? null : new SequenceCommand(
                trn("Remove old keys from up to {0} object", "Remove old keys from up to {0} objects", primitives.size(), primitives.size()),
                commands
        );
    }

    /**
     * replies the list of keys of the tags managed by this model
     *
     * @return the list of keys managed by this model
     */
    public List<String> getKeys() {
        return tags.stream()
                .filter(tag -> !tag.getName().trim().isEmpty())
                .map(TagModel::getName)
                .collect(Collectors.toList());
    }

    /**
     * sorts the current tags according alphabetical order of names
     */
    protected void sort() {
        tags.sort(Comparator.comparing(TagModel::getName));
    }

    /**
     * updates the name of a tag and sets the dirty state to  true if
     * the new name is different from the old name.
     *
     * @param tag   the tag
     * @param newName  the new name
     */
    public void updateTagName(TagModel tag, String newName) {
        String oldName = tag.getName();
        tag.setName(newName);
        if (!newName.equals(oldName)) {
            setDirty(true);
        }
        SelectionStateMemento memento = new SelectionStateMemento();
        fireTableDataChanged();
        memento.apply();
    }

    /**
     * updates the value value of a tag and sets the dirty state to true if the
     * new name is different from the old name
     *
     * @param tag  the tag
     * @param newValue  the new value
     */
    public void updateTagValue(TagModel tag, String newValue) {
        String oldValue = tag.getValue();
        tag.setValue(newValue);
        if (!newValue.equals(oldValue)) {
            setDirty(true);
        }
        SelectionStateMemento memento = new SelectionStateMemento();
        fireTableDataChanged();
        memento.apply();
    }

    /**
     * Load tags from given list
     * @param tags - the list
     */
    public void updateTags(List<Tag> tags) {
        if (tags.isEmpty())
            return;

        commitPendingEdit();
        Map<String, TagModel> modelTags = IntStream.range(0, getRowCount())
                .mapToObj(this::get)
                .collect(Collectors.toMap(TagModel::getName, tagModel -> tagModel, (a, b) -> b));
        for (Tag tag: tags) {
            TagModel existing = modelTags.get(tag.getKey());

            if (tag.getValue().isEmpty()) {
                if (existing != null) {
                    delete(tag.getKey());
                }
            } else {
                if (existing != null) {
                    updateTagValue(existing, tag.getValue());
                } else {
                    add(tag.getKey(), tag.getValue());
                }
            }
        }
    }

    /**
     * replies true, if this model has been updated
     *
     * @return true, if this model has been updated
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Returns the list of tagging presets types to consider when updating the presets list panel.
     * By default returns type of associated primitive or empty set.
     * @return the list of tagging presets types to consider when updating the presets list panel
     * @see #forPrimitive
     * @see TaggingPresetType#forPrimitive
     * @since 9588
     */
    public Collection<TaggingPresetType> getTaggingPresetTypes() {
        return primitive == null ? EnumSet.noneOf(TaggingPresetType.class) : EnumSet.of(TaggingPresetType.forPrimitive(primitive));
    }

    /**
     * Makes this TagEditorModel specific to a given OSM primitive.
     * @param primitive primitive to consider
     * @return {@code this}
     * @since 9588
     */
    public TagEditorModel forPrimitive(OsmPrimitive primitive) {
        this.primitive = primitive;
        return this;
    }

    /**
     * Sets the listener that is notified when an edit should be aborted.
     * @param endEditListener The listener to be notified when editing should be aborted.
     */
    public void setEndEditListener(EndEditListener endEditListener) {
        this.endEditListener = endEditListener;
    }

    private void commitPendingEdit() {
        if (endEditListener != null) {
            endEditListener.endCellEditing();
        }
    }

    class SelectionStateMemento {
        private final int rowMin;
        private final int rowMax;
        private final int colMin;
        private final int colMax;

        SelectionStateMemento() {
            rowMin = rowSelectionModel.getMinSelectionIndex();
            rowMax = rowSelectionModel.getMaxSelectionIndex();
            colMin = colSelectionModel.getMinSelectionIndex();
            colMax = colSelectionModel.getMaxSelectionIndex();
        }

        void apply() {
            rowSelectionModel.setValueIsAdjusting(true);
            colSelectionModel.setValueIsAdjusting(true);
            if (rowMin >= 0 && rowMax >= 0) {
                rowSelectionModel.setSelectionInterval(rowMin, rowMax);
            }
            if (colMin >= 0 && colMax >= 0) {
                colSelectionModel.setSelectionInterval(colMin, colMax);
            }
            rowSelectionModel.setValueIsAdjusting(false);
            colSelectionModel.setValueIsAdjusting(false);
        }
    }

    /**
     * A listener that is called whenever the cells may be updated from outside the editor and the editor should thus be committed.
     * @since 10604
     */
    @FunctionalInterface
    public interface EndEditListener {
        /**
         * Requests to end the editing of any cells on this model
         */
        void endCellEditing();
    }
}
