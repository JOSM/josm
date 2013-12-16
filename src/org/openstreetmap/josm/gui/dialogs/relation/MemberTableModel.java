// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

public class MemberTableModel extends AbstractTableModel implements TableModelListener, SelectionChangedListener, DataSetListener, OsmPrimitivesTableModel {

    /**
     * data of the table model: The list of members and the cached WayConnectionType of each member.
     **/
    private List<RelationMember> members;
    private List<WayConnectionType> connectionType = null;

    private DefaultListSelectionModel listSelectionModel;
    private final CopyOnWriteArrayList<IMemberModelListener> listeners;
    private final OsmDataLayer layer;
    private final PresetListPanel.PresetHandler presetHandler;

    private final WayConnectionTypeCalculator wayConnectionTypeCalculator = new WayConnectionTypeCalculator();
    private final RelationSorter relationSorter = new RelationSorter();

    /**
     * constructor
     */
    public MemberTableModel(OsmDataLayer layer, PresetListPanel.PresetHandler presetHandler) {
        members = new ArrayList<RelationMember>();
        listeners = new CopyOnWriteArrayList<IMemberModelListener>();
        this.layer = layer;
        this.presetHandler = presetHandler;
        addTableModelListener(this);
    }

    public OsmDataLayer getLayer() {
        return layer;
    }

    public void register() {
        DataSet.addSelectionListener(this);
        getLayer().data.addDataSetListener(this);
    }

    public void unregister() {
        DataSet.removeSelectionListener(this);
        getLayer().data.removeDataSetListener(this);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface SelectionChangedListener                                          */
    /* --------------------------------------------------------------------------- */
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (Main.main.getEditLayer() != this.layer) return;
        // just trigger a repaint
        Collection<RelationMember> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface DataSetListener                                                   */
    /* --------------------------------------------------------------------------- */
    @Override
    public void dataChanged(DataChangedEvent event) {
        // just trigger a repaint - the display name of the relation members may
        // have changed
        Collection<RelationMember> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {/* ignore */}
    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {/* ignore */}

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        // just refresh the respective table cells
        //
        Collection<RelationMember> sel = getSelectedMembers();
        for (int i=0; i < members.size();i++) {
            if (members.get(i).getMember() == event.getPrimitive()) {
                fireTableCellUpdated(i, 1 /* the column with the primitive name */);
            }
        }
        setSelectedMembers(sel);
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignore */}

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignore */}
    /* --------------------------------------------------------------------------- */

    public void addMemberModelListener(IMemberModelListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeMemberModelListener(IMemberModelListener listener) {
        listeners.remove(listener);
    }

    protected void fireMakeMemberVisible(int index) {
        for (IMemberModelListener listener : listeners) {
            listener.makeMemberVisible(index);
        }
    }

    public void populate(Relation relation) {
        members.clear();
        if (relation != null) {
            // make sure we work with clones of the relation members
            // in the model.
            members.addAll(new Relation(relation).getMembers());
        }
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return members.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return members.get(rowIndex).getRole();
        case 1:
            return members.get(rowIndex).getMember();
        case 2:
            return getWayConnection(rowIndex);
        }
        // should not happen
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        RelationMember member = members.get(rowIndex);
        RelationMember newMember = new RelationMember(value.toString(), member.getMember());
        members.remove(rowIndex);
        members.add(rowIndex, newMember);
    }

    @Override
    public OsmPrimitive getReferredPrimitive(int idx) {
        return members.get(idx).getMember();
    }

    public void moveUp(int[] selectedRows) {
        if (!canMoveUp(selectedRows))
            return;

        for (int row : selectedRows) {
            RelationMember member1 = members.get(row);
            RelationMember member2 = members.get(row - 1);
            members.set(row, member2);
            members.set(row - 1, member1);
        }
        fireTableDataChanged();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedRows) {
            row--;
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(selectedRows[0] - 1);
    }

    public void moveDown(int[] selectedRows) {
        if (!canMoveDown(selectedRows))
            return;

        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int row = selectedRows[i];
            RelationMember member1 = members.get(row);
            RelationMember member2 = members.get(row + 1);
            members.set(row, member2);
            members.set(row + 1, member1);
        }
        fireTableDataChanged();
        getSelectionModel();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedRows) {
            row++;
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(selectedRows[0] + 1);
    }

    public void remove(int[] selectedRows) {
        if (!canRemove(selectedRows))
            return;
        int offset = 0;
        for (int row : selectedRows) {
            row -= offset;
            if (members.size() > row) {
                members.remove(row);
                offset++;
            }
        }
        fireTableDataChanged();
    }

    public boolean canMoveUp(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        Arrays.sort(rows);
        return rows[0] > 0 && members.size() > 0;
    }

    public boolean canMoveDown(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        Arrays.sort(rows);
        return members.size() > 0 && rows[rows.length - 1] < members.size() - 1;
    }

    public boolean canRemove(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        return true;
    }

    public DefaultListSelectionModel getSelectionModel() {
        if (listSelectionModel == null) {
            listSelectionModel = new DefaultListSelectionModel();
            listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        return listSelectionModel;
    }

    public void removeMembersReferringTo(List<? extends OsmPrimitive> primitives) {
        if (primitives == null)
            return;
        Iterator<RelationMember> it = members.iterator();
        while (it.hasNext()) {
            RelationMember member = it.next();
            if (primitives.contains(member.getMember())) {
                it.remove();
            }
        }
        fireTableDataChanged();
    }

    public void applyToRelation(Relation relation) {
        relation.setMembers(members);
    }

    public boolean hasSameMembersAs(Relation relation) {
        if (relation == null)
            return false;
        if (relation.getMembersCount() != members.size())
            return false;
        for (int i = 0; i < relation.getMembersCount(); i++) {
            if (!relation.getMember(i).equals(members.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Replies the set of incomplete primitives
     *
     * @return the set of incomplete primitives
     */
    public Set<OsmPrimitive> getIncompleteMemberPrimitives() {
        Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (RelationMember member : members) {
            if (member.getMember().isIncomplete()) {
                ret.add(member.getMember());
            }
        }
        return ret;
    }

    /**
     * Replies the set of selected incomplete primitives
     *
     * @return the set of selected incomplete primitives
     */
    public Set<OsmPrimitive> getSelectedIncompleteMemberPrimitives() {
        Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (RelationMember member : getSelectedMembers()) {
            if (member.getMember().isIncomplete()) {
                ret.add(member.getMember());
            }
        }
        return ret;
    }

    /**
     * Replies true if at least one the relation members is incomplete
     *
     * @return true if at least one the relation members is incomplete
     */
    public boolean hasIncompleteMembers() {
        for (RelationMember member : members) {
            if (member.getMember().isIncomplete())
                return true;
        }
        return false;
    }

    /**
     * Replies true if at least one of the selected members is incomplete
     *
     * @return true if at least one of the selected members is incomplete
     */
    public boolean hasIncompleteSelectedMembers() {
        for (RelationMember member : getSelectedMembers()) {
            if (member.getMember().isIncomplete())
                return true;
        }
        return false;
    }

    protected List<Integer> getSelectedIndices() {
        List<Integer> selectedIndices = new ArrayList<Integer>();
        for (int i = 0; i < members.size(); i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    private void addMembersAtIndex(List<? extends OsmPrimitive> primitives, int index) {
        final Collection<TaggingPreset> presets = TaggingPreset.getMatchingPresets(EnumSet.of(TaggingPresetType.RELATION), presetHandler.getSelection().iterator().next().getKeys(), false);
        if (primitives == null)
            return;
        int idx = index;
        for (OsmPrimitive primitive : primitives) {
            Set<String> potentialRoles = new TreeSet<String>();
            for (TaggingPreset tp : presets) {
                String suggestedRole = tp.suggestRoleForOsmPrimitive(primitive);
                if (suggestedRole != null) {
                    potentialRoles.add(suggestedRole);
                }
            }
            // TODO: propose user to choose role among potential ones instead of picking first one
            final String role = potentialRoles.isEmpty() ? null : potentialRoles.iterator().next();
            RelationMember member = new RelationMember(role == null ? "" : role, primitive);
            members.add(idx++, member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        getSelectionModel().addSelectionInterval(index, index + primitives.size() - 1);
        fireMakeMemberVisible(index);
    }

    public void addMembersAtBeginning(List<? extends OsmPrimitive> primitives) {
        addMembersAtIndex(primitives, 0);
    }

    public void addMembersAtEnd(List<? extends OsmPrimitive> primitives) {
        addMembersAtIndex(primitives, members.size());
    }

    public void addMembersBeforeIdx(List<? extends OsmPrimitive> primitives, int idx) {
        addMembersAtIndex(primitives, idx);
    }

    public void addMembersAfterIdx(List<? extends OsmPrimitive> primitives, int idx) {
        addMembersAtIndex(primitives, idx + 1);
    }

    /**
     * Replies the number of members which refer to a particular primitive
     *
     * @param primitive the primitive
     * @return the number of members which refer to a particular primitive
     */
    public int getNumMembersWithPrimitive(OsmPrimitive primitive) {
        int count = 0;
        for (RelationMember member : members) {
            if (member.getMember().equals(primitive)) {
                count++;
            }
        }
        return count;
    }

    /**
     * updates the role of the members given by the indices in <code>idx</code>
     *
     * @param idx the array of indices
     * @param role the new role
     */
    public void updateRole(int[] idx, String role) {
        if (idx == null || idx.length == 0)
            return;
        for (int row : idx) {
            RelationMember oldMember = members.get(row);
            RelationMember newMember = new RelationMember(role, oldMember.getMember());
            members.remove(row);
            members.add(row, newMember);
        }
        fireTableDataChanged();
        for (int row : idx) {
            getSelectionModel().addSelectionInterval(row, row);
        }
    }

    /**
     * Get the currently selected relation members
     *
     * @return a collection with the currently selected relation members
     */
    public Collection<RelationMember> getSelectedMembers() {
        List<RelationMember> selectedMembers = new ArrayList<RelationMember>();
        for (int i : getSelectedIndices()) {
            selectedMembers.add(members.get(i));
        }
        return selectedMembers;
    }

    /**
     * Replies the set of selected referers. Never null, but may be empty.
     *
     * @return the set of selected referers
     */
    public Collection<OsmPrimitive> getSelectedChildPrimitives() {
        Collection<OsmPrimitive> ret = new ArrayList<OsmPrimitive>();
        for (RelationMember m: getSelectedMembers()) {
            ret.add(m.getMember());
        }
        return ret;
    }

    /**
     * Replies the set of selected referers. Never null, but may be empty.
     *
     * @return the set of selected referers
     */
    public Set<OsmPrimitive> getChildPrimitives(Collection<? extends OsmPrimitive> referenceSet) {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        if (referenceSet == null) return null;
        for (RelationMember m: members) {
            if (referenceSet.contains(m.getMember())) {
                ret.add(m.getMember());
            }
        }
        return ret;
    }

    /**
     * Selects the members in the collection selectedMembers
     *
     * @param selectedMembers the collection of selected members
     */
    public void setSelectedMembers(Collection<RelationMember> selectedMembers) {
        if (selectedMembers == null || selectedMembers.isEmpty()) {
            getSelectionModel().clearSelection();
            return;
        }

        // lookup the indices for the respective members
        //
        Set<Integer> selectedIndices = new HashSet<Integer>();
        for (RelationMember member : selectedMembers) {
            for (int idx = 0; idx < members.size(); ++idx) {
                if (member.equals(members.get(idx))) {
                    selectedIndices.add(idx);
                }
            }
        }
        setSelectedMembersIdx(selectedIndices);
    }

    /**
     * Selects the members in the collection selectedIndices
     *
     * @param selectedIndices the collection of selected member indices
     */
    public void setSelectedMembersIdx(Collection<Integer> selectedIndices) {
        if (selectedIndices == null || selectedIndices.isEmpty()) {
            getSelectionModel().clearSelection();
            return;
        }
        // select the members
        //
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedIndices) {
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);
        // make the first selected member visible
        //
        if (!selectedIndices.isEmpty()) {
            fireMakeMemberVisible(Collections.min(selectedIndices));
        }
    }

    /**
     * Replies true if the index-th relation members referrs
     * to an editable relation, i.e. a relation which is not
     * incomplete.
     *
     * @param index the index
     * @return true, if the index-th relation members referrs
     * to an editable relation, i.e. a relation which is not
     * incomplete
     */
    public boolean isEditableRelation(int index) {
        if (index < 0 || index >= members.size())
            return false;
        RelationMember member = members.get(index);
        if (!member.isRelation())
            return false;
        Relation r = member.getRelation();
        return !r.isIncomplete();
    }

    /**
     * Replies true if there is at least one relation member given as {@code members}
     * which refers to at least on the primitives in {@code primitives}.
     *
     * @param members the members
     * @param primitives the collection of primitives
     * @return true if there is at least one relation member in this model
     * which refers to at least on the primitives in <code>primitives</code>; false
     * otherwise
     */
    public static boolean hasMembersReferringTo(Collection<RelationMember> members, Collection<OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty())
            return false;
        HashSet<OsmPrimitive> referrers = new HashSet<OsmPrimitive>();
        for (RelationMember member : members) {
            referrers.add(member.getMember());
        }
        for (OsmPrimitive referred : primitives) {
            if (referrers.contains(referred))
                return true;
        }
        return false;
    }

    /**
     * Replies true if there is at least one relation member in this model
     * which refers to at least on the primitives in <code>primitives</code>.
     *
     * @param primitives the collection of primitives
     * @return true if there is at least one relation member in this model
     * which refers to at least on the primitives in <code>primitives</code>; false
     * otherwise
     */
    public boolean hasMembersReferringTo(Collection<OsmPrimitive> primitives) {
        return hasMembersReferringTo(members, primitives);
    }

    /**
     * Selects all mebers which refer to {@link OsmPrimitive}s in the collections
     * <code>primitmives</code>. Does nothing is primitives is null.
     *
     * @param primitives the collection of primitives
     */
    public void selectMembersReferringTo(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int i=0; i< members.size();i++) {
            RelationMember m = members.get(i);
            if (primitives.contains(m.getMember())) {
                this.getSelectionModel().addSelectionInterval(i,i);
            }
        }
        getSelectionModel().setValueIsAdjusting(false);
        if (!getSelectedIndices().isEmpty()) {
            fireMakeMemberVisible(getSelectedIndices().get(0));
        }
    }

    /**
     * Replies true if <code>primitive</code> is currently selected in the layer this
     * model is attached to
     *
     * @param primitive the primitive
     * @return true if <code>primitive</code> is currently selected in the layer this
     * model is attached to, false otherwise
     */
    public boolean isInJosmSelection(OsmPrimitive primitive) {
        return layer.data.isSelected(primitive);
    }

    /**
     * Replies true if the layer this model belongs to is equal to the active
     * layer
     *
     * @return true if the layer this model belongs to is equal to the active
     * layer
     */
    protected boolean isActiveLayer() {
        if (!Main.isDisplayingMapView()) return false;
        return Main.map.mapView.getActiveLayer() == layer;
    }

    /**
     * Sort the selected relation members by the way they are linked.
     */
    void sort() {
        List<RelationMember> selectedMembers = new ArrayList<RelationMember>(getSelectedMembers());
        List<RelationMember> sortedMembers = null;
        List<RelationMember> newMembers;
        if (selectedMembers.size() <= 1) {
            newMembers = relationSorter.sortMembers(members);
            sortedMembers = newMembers;
        } else {
            sortedMembers = relationSorter.sortMembers(selectedMembers);
            List<Integer> selectedIndices = getSelectedIndices();
            newMembers = new ArrayList<RelationMember>();
            boolean inserted = false;
            for (int i=0; i < members.size(); i++) {
                if (selectedIndices.contains(i)) {
                    if (!inserted) {
                        newMembers.addAll(sortedMembers);
                        inserted = true;
                    }
                } else {
                    newMembers.add(members.get(i));
                }
            }
        }

        if (members.size() != newMembers.size()) throw new AssertionError();

        members.clear();
        members.addAll(newMembers);
        fireTableDataChanged();
        setSelectedMembers(sortedMembers);
    }


    WayConnectionType getWayConnection(int i) {
        if (connectionType == null) {
            connectionType = wayConnectionTypeCalculator.updateLinks(members);
        }
        return connectionType.get(i);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        connectionType = null;
    }

    /**
     * Reverse the relation members.
     */
    void reverse() {
        List<Integer> selectedIndices = getSelectedIndices();
        List<Integer> selectedIndicesReversed = getSelectedIndices();

        if (selectedIndices.size() <= 1) {
            Collections.reverse(members);
            fireTableDataChanged();
            setSelectedMembers(members);
        } else {
            Collections.reverse(selectedIndicesReversed);

            List<RelationMember> newMembers = new ArrayList<RelationMember>(members);

            for (int i=0; i < selectedIndices.size(); i++) {
                newMembers.set(selectedIndices.get(i), members.get(selectedIndicesReversed.get(i)));
            }

            if (members.size() != newMembers.size()) throw new AssertionError();
            members.clear();
            members.addAll(newMembers);
            fireTableDataChanged();
            setSelectedMembersIdx(selectedIndices);
        }
    }
}
