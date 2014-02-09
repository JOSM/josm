// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * TagEditorModel is a table model.
 *
 */
@SuppressWarnings("serial")
public class TagEditorModel extends AbstractTableModel {
    static public final String PROP_DIRTY = TagEditorModel.class.getName() + ".dirty";

    /** the list holding the tags */
    protected final List<TagModel> tags =new ArrayList<TagModel>();

    /** indicates whether the model is dirty */
    private boolean dirty =  false;
    private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);

    private DefaultListSelectionModel rowSelectionModel;
    private DefaultListSelectionModel colSelectionModel;

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
        this.rowSelectionModel = new DefaultListSelectionModel();
        this.colSelectionModel  = new DefaultListSelectionModel();
    }
    /**
     * Creates a new tag editor model.
     *
     * @param rowSelectionModel the row selection model. Must not be null.
     * @param colSelectionModel the column selection model. Must not be null.
     * @throws IllegalArgumentException thrown if {@code rowSelectionModel} is null
     * @throws IllegalArgumentException thrown if {@code colSelectionModel} is null
     */
    public TagEditorModel(DefaultListSelectionModel rowSelectionModel, DefaultListSelectionModel colSelectionModel) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(rowSelectionModel, "rowSelectionModel");
        CheckParameterUtil.ensureParameterNotNull(colSelectionModel, "colSelectionModel");
        this.rowSelectionModel = rowSelectionModel;
        this.colSelectionModel  = colSelectionModel;
    }

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

    public void removeProperyChangeListener(PropertyChangeListener listener) {
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

        TagModel tag = tags.get(rowIndex);
        switch(columnIndex) {
        case 0:
        case 1:
            return tag;

        default:
            throw new IndexOutOfBoundsException("unexpected columnIndex: columnIndex=" + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        TagModel tag = get(row);
        if (tag == null) return;
        switch(col) {
        case 0:
            updateTagName(tag, (String)value);
            break;
        case 1:
            String v = (String)value;
            if (tag.getValueCount() > 1 && !v.isEmpty()) {
                updateTagValue(tag, v);
            } else if (tag.getValueCount() <= 1) {
                updateTagValue(tag, v);
            }
        }
    }

    /**
     * removes all tags in the model
     */
    public void clear() {
        tags.clear();
        setDirty(true);
        fireTableDataChanged();
    }

    /**
     * adds a tag to the model
     *
     * @param tag the tag. Must not be null.
     *
     * @exception IllegalArgumentException thrown, if tag is null
     */
    public void add(TagModel tag) {
        if (tag == null)
            throw new IllegalArgumentException("argument 'tag' must not be null");
        tags.add(tag);
        setDirty(true);
        fireTableDataChanged();
    }

    public void prepend(TagModel tag) {
        if (tag == null)
            throw new IllegalArgumentException("argument 'tag' must not be null");
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
        name = (name == null) ? "" : name;
        value = (value == null) ? "" : value;

        TagModel tag = get(name);
        if (tag == null) {
            tag = new TagModel(name, value);
            int index = tags.size();
            while (index >= 1 && tags.get(index - 1).getName().isEmpty() && tags.get(index - 1).getValue().isEmpty()) {
                index--; // If last line(s) is empty, add new tag before it
            }
            tags.add(index, tag);
        } else {
            tag.addValue(value);
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
        name = (name == null) ? "" : name;
        for (TagModel tag : tags) {
            if (tag.getName().equals(name))
                return tag;
        }
        return null;
    }

    public TagModel get(int idx) {
        if (idx >= tags.size()) return null;
        return tags.get(idx);
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
    public void deleteTagNames(int [] tagIndices) {
        if (tags == null)
            return;
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
    public void deleteTagValues(int [] tagIndices) {
        if (tags == null)
            return;
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
        if (name == null) return;
        Iterator<TagModel> it = tags.iterator();
        boolean changed = false;
        while(it.hasNext()) {
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
    public void deleteTags(int [] tagIndices) {
        if (tags == null)
            return;
        ArrayList<TagModel> toDelete = new ArrayList<TagModel>();
        for (int tagIdx : tagIndices) {
            TagModel tag = tags.get(tagIdx);
            if (tag != null) {
                toDelete.add(tag);
            }
        }
        for (TagModel tag : toDelete) {
            tags.remove(tag);
        }
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
        setDirty(true);
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
        this.tags.clear();
        for (String key : primitive.keySet()) {
            String value = primitive.get(key);
            this.tags.add(new TagModel(key,value));
        }
        TagModel tag = new TagModel();
        sort();
        tags.add(tag);
        setDirty(false);
        fireTableDataChanged();
    }

    /**
     * Initializes the model with the tags of an OSM primitive
     *
     * @param tags the tags of an OSM primitive
     */
    public void initFromTags(Map<String,String> tags) {
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
        this.tags.clear();
        if (tags == null){
            setDirty(false);
            return;
        }
        for (String key : tags.getKeys()) {
            String value = tags.getJoinedValues(key);
            this.tags.add(new TagModel(key,value));
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
        Map<String,String> tags = primitive.getKeys();
        applyToTags(tags, false);
        primitive.setKeys(tags);
    }

    /**
     * applies the current state of the tag editor model to a map of tags
     *
     * @param tags the map of key/value pairs
     *
     */
    public void applyToTags(Map<String, String> tags, boolean keepEmpty) {
        tags.clear();
        for (TagModel tag: this.tags) {
            // tag still holds an unchanged list of different values for the same key.
            // no property change command required
            if (tag.getValueCount() > 1) {
                continue;
            }

            // tag name holds an empty key. Don't apply it to the selection.
            //
            if (!keepEmpty && (tag.getName().trim().isEmpty() || tag.getValue().trim().isEmpty())) {
                continue;
            }
            tags.put(tag.getName().trim(), tag.getValue().trim());
        }
    }

    public Map<String,String> getTags() {
        return getTags(false);
    }

    public Map<String,String> getTags(boolean keepEmpty) {
        Map<String,String> tags = new HashMap<String, String>();
        applyToTags(tags, keepEmpty);
        return tags;
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
        if (key == null) return false;
        for (TagModel tag : tags) {
            if (tag.getName().equals(key))
                return true;
        }
        return false;
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
        ArrayList<Command> commands = new ArrayList<Command>();

        for (OsmPrimitive primitive : primitives) {
            for (String oldkey : primitive.keySet()) {
                if (!currentkeys.contains(oldkey)) {
                    ChangePropertyCommand deleteCommand =
                        new ChangePropertyCommand(primitive,oldkey,null);
                    commands.add(deleteCommand);
                }
            }
        }

        SequenceCommand command = new SequenceCommand(
                trn("Remove old keys from up to {0} object", "Remove old keys from up to {0} objects", primitives.size(), primitives.size()),
                commands
        );

        return command;
    }

    /**
     * replies the list of keys of the tags managed by this model
     *
     * @return the list of keys managed by this model
     */
    public List<String> getKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        for (TagModel tag: tags) {
            if (!tag.getName().trim().isEmpty()) {
                keys.add(tag.getName());
            }
        }
        return keys;
    }

    /**
     * sorts the current tags according alphabetical order of names
     */
    protected void sort() {
        java.util.Collections.sort(
                tags,
                new Comparator<TagModel>() {
                    @Override
                    public int compare(TagModel self, TagModel other) {
                        return self.getName().compareTo(other.getName());
                    }
                }
        );
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
        if (! newName.equals(oldName)) {
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
        if (! newValue.equals(oldValue)) {
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

        Map<String, TagModel> modelTags = new HashMap<String, TagModel>();
        for (int i=0; i<getRowCount(); i++) {
            TagModel tagModel = get(i);
            modelTags.put(tagModel.getName(), tagModel);
        }
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

    class SelectionStateMemento {
        private int rowMin;
        private int rowMax;
        private int colMin;
        private int colMax;

        public SelectionStateMemento() {
            rowMin = rowSelectionModel.getMinSelectionIndex();
            rowMax = rowSelectionModel.getMaxSelectionIndex();
            colMin = colSelectionModel.getMinSelectionIndex();
            colMax = colSelectionModel.getMaxSelectionIndex();
        }

        public void apply() {
            rowSelectionModel.setValueIsAdjusting(true);
            colSelectionModel.setValueIsAdjusting(true);
            if (rowMin >= 0 && rowMax >=0) {
                rowSelectionModel.setSelectionInterval(rowMin, rowMax);
            }
            if (colMin >=0 && colMax >= 0) {
                colSelectionModel.setSelectionInterval(colMin, colMax);
            }
            rowSelectionModel.setValueIsAdjusting(false);
            colSelectionModel.setValueIsAdjusting(false);
        }
    }
}
