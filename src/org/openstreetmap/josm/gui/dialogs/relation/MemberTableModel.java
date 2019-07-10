// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
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
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.SortableTableModel;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;
import org.openstreetmap.josm.tools.ArrayUtils;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is the base model used for the {@link MemberTable}. It holds the member data.
 */
public class MemberTableModel extends AbstractTableModel
implements TableModelListener, DataSelectionListener, DataSetListener, OsmPrimitivesTableModel, SortableTableModel<RelationMember> {

    /**
     * data of the table model: The list of members and the cached WayConnectionType of each member.
     **/
    private final transient List<RelationMember> members;
    private transient List<WayConnectionType> connectionType;
    private final transient Relation relation;

    private DefaultListSelectionModel listSelectionModel;
    private final transient CopyOnWriteArrayList<IMemberModelListener> listeners;
    private final transient OsmDataLayer layer;
    private final transient TaggingPresetHandler presetHandler;

    private final transient WayConnectionTypeCalculator wayConnectionTypeCalculator = new WayConnectionTypeCalculator();
    private final transient RelationSorter relationSorter = new RelationSorter();

    /**
     * constructor
     * @param relation relation
     * @param layer data layer
     * @param presetHandler tagging preset handler
     */
    public MemberTableModel(Relation relation, OsmDataLayer layer, TaggingPresetHandler presetHandler) {
        this.relation = relation;
        this.members = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.layer = layer;
        this.presetHandler = presetHandler;
        addTableModelListener(this);
    }

    /**
     * Returns the data layer.
     * @return the data layer
     */
    public OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Registers listeners (selection change and dataset change).
     */
    public void register() {
        SelectionEventManager.getInstance().addSelectionListener(this);
        getLayer().data.addDataSetListener(this);
    }

    /**
     * Unregisters listeners (selection change and dataset change).
     */
    public void unregister() {
        SelectionEventManager.getInstance().removeSelectionListener(this);
        getLayer().data.removeDataSetListener(this);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface DataSelectionListener                                             */
    /* --------------------------------------------------------------------------- */
    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        if (MainApplication.getLayerManager().getActiveDataLayer() != this.layer) return;
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
        // just trigger a repaint - the display name of the relation members may have changed
        Collection<RelationMember> sel = getSelectedMembers();
        GuiHelper.runInEDT(this::fireTableDataChanged);
        setSelectedMembers(sel);
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        // ignore
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // ignore
    }

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
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getMember() == event.getPrimitive()) {
                fireTableCellUpdated(i, 1 /* the column with the primitive name */);
            }
        }
        setSelectedMembers(sel);
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        // ignore
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // ignore
    }

    /* --------------------------------------------------------------------------- */

    /**
     * Add a new member model listener.
     * @param listener member model listener to add
     */
    public void addMemberModelListener(IMemberModelListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * Remove a member model listener.
     * @param listener member model listener to remove
     */
    public void removeMemberModelListener(IMemberModelListener listener) {
        listeners.remove(listener);
    }

    protected void fireMakeMemberVisible(int index) {
        for (IMemberModelListener listener : listeners) {
            listener.makeMemberVisible(index);
        }
    }

    /**
     * Populates this model from the given relation.
     * @param relation relation
     */
    public void populate(Relation relation) {
        members.clear();
        if (relation != null) {
            // make sure we work with clones of the relation members in the model.
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
        // fix #10524 - IndexOutOfBoundsException: Index: 2, Size: 2
        if (rowIndex >= members.size()) {
            return;
        }
        RelationMember member = members.get(rowIndex);
        String role = value.toString();
        if (member.hasRole(role))
            return;
        RelationMember newMember = new RelationMember(role, member.getMember());
        members.remove(rowIndex);
        members.add(rowIndex, newMember);
        fireTableDataChanged();
    }

    @Override
    public OsmPrimitive getReferredPrimitive(int idx) {
        return members.get(idx).getMember();
    }

    @Override
    public boolean move(int delta, int... selectedRows) {
        if (!canMove(delta, this::getRowCount, selectedRows))
            return false;
        doMove(delta, selectedRows);
        fireTableDataChanged();
        final ListSelectionModel selectionModel = getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        BitSet selected = new BitSet();
        for (int row : selectedRows) {
            row += delta;
            selected.set(row);
        }
        addToSelectedMembers(selected);
        selectionModel.setValueIsAdjusting(false);
        fireMakeMemberVisible(selectedRows[0] + delta);
        return true;
    }

    /**
     * Remove selected rows, if possible.
     * @param selectedRows rows to remove
     * @see #canRemove
     */
    public void remove(int... selectedRows) {
        if (!canRemove(selectedRows))
            return;
        int offset = 0;
        final ListSelectionModel selectionModel = getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        for (int row : selectedRows) {
            row -= offset;
            if (members.size() > row) {
                members.remove(row);
                selectionModel.removeIndexInterval(row, row);
                offset++;
            }
        }
        selectionModel.setValueIsAdjusting(false);
        fireTableDataChanged();
    }

    /**
     * Checks that a range of rows can be removed.
     * @param rows indexes of rows to remove
     * @return {@code true} if rows can be removed
     */
    public boolean canRemove(int... rows) {
        return rows != null && rows.length != 0;
    }

    @Override
    public DefaultListSelectionModel getSelectionModel() {
        if (listSelectionModel == null) {
            listSelectionModel = new DefaultListSelectionModel();
            listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        return listSelectionModel;
    }

    @Override
    public RelationMember getValue(int index) {
        return members.get(index);
    }

    @Override
    public RelationMember setValue(int index, RelationMember value) {
        return members.set(index, value);
    }

    /**
     * Remove members referring to the given list of primitives.
     * @param primitives list of OSM primitives
     */
    public void removeMembersReferringTo(List<? extends OsmPrimitive> primitives) {
        if (primitives == null)
            return;
        if (members.removeIf(member -> primitives.contains(member.getMember())))
            fireTableDataChanged();
    }

    /**
     * Applies this member model to the given relation.
     * @param relation relation
     */
    public void applyToRelation(Relation relation) {
        relation.setMembers(members.stream()
                .filter(rm -> !rm.getMember().isDeleted()).collect(Collectors.toList()));
    }

    /**
     * Determines if this model has the same members as the given relation.
     * @param relation relation
     * @return {@code true} if this model has the same members as {@code relation}
     */
    public boolean hasSameMembersAs(Relation relation) {
        if (relation == null || relation.getMembersCount() != members.size())
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
        return members.stream().map(RelationMember::getMember).filter(AbstractPrimitive::isIncomplete).collect(Collectors.toSet());
    }

    /**
     * Replies the set of selected incomplete primitives
     *
     * @return the set of selected incomplete primitives
     */
    public Set<OsmPrimitive> getSelectedIncompleteMemberPrimitives() {
        return getSelectedMembers().stream().map(RelationMember::getMember).filter(AbstractPrimitive::isIncomplete).collect(Collectors.toSet());
    }

    /**
     * Replies true if at least one the relation members is incomplete
     *
     * @return true if at least one the relation members is incomplete
     */
    public boolean hasIncompleteMembers() {
        return members.stream().anyMatch(rm -> rm.getMember().isIncomplete());
    }

    /**
     * Replies true if at least one of the selected members is incomplete
     *
     * @return true if at least one of the selected members is incomplete
     */
    public boolean hasIncompleteSelectedMembers() {
        return getSelectedMembers().stream().anyMatch(rm -> rm.getMember().isIncomplete());
    }

    private void addMembersAtIndex(List<? extends OsmPrimitive> primitives, int index) {
        if (primitives == null || primitives.isEmpty())
            return;
        int idx = index;
        for (OsmPrimitive primitive : primitives) {
            final RelationMember member = getRelationMemberForPrimitive(primitive);
            members.add(idx++, member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        getSelectionModel().addSelectionInterval(index, index + primitives.size() - 1);
        fireMakeMemberVisible(index);
    }

    RelationMember getRelationMemberForPrimitive(final OsmPrimitive primitive) {
        final Collection<TaggingPreset> presets = TaggingPresets.getMatchingPresets(
                EnumSet.of(relation != null ? TaggingPresetType.forPrimitive(relation) : TaggingPresetType.RELATION),
                presetHandler.getSelection().iterator().next().getKeys(), false);
        Collection<String> potentialRoles = new TreeSet<>();
        for (TaggingPreset tp : presets) {
            String suggestedRole = tp.suggestRoleForOsmPrimitive(primitive);
            if (suggestedRole != null) {
                potentialRoles.add(suggestedRole);
            }
        }
        // TODO: propose user to choose role among potential ones instead of picking first one
        final String role = potentialRoles.isEmpty() ? "" : potentialRoles.iterator().next();
        return new RelationMember(role == null ? "" : role, primitive);
    }

    void addMembersAtIndexKeepingOldSelection(final Iterable<RelationMember> newMembers, final int index) {
        int idx = index;
        for (RelationMember member : newMembers) {
            members.add(idx++, member);
        }
        invalidateConnectionType();
        fireTableRowsInserted(index, idx - 1);
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
            // fix #7885 - IndexOutOfBoundsException: Index: 39, Size: 39
            if (row >= members.size()) {
                continue;
            }
            RelationMember oldMember = members.get(row);
            RelationMember newMember = new RelationMember(role, oldMember.getMember());
            members.remove(row);
            members.add(row, newMember);
        }
        fireTableDataChanged();
        BitSet selected = new BitSet();
        for (int row : idx) {
            selected.set(row);
        }
        addToSelectedMembers(selected);
    }

    /**
     * Get the currently selected relation members
     *
     * @return a collection with the currently selected relation members
     */
    public Collection<RelationMember> getSelectedMembers() {
        List<RelationMember> selectedMembers = new ArrayList<>();
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
        Collection<OsmPrimitive> ret = new ArrayList<>();
        for (RelationMember m: getSelectedMembers()) {
            ret.add(m.getMember());
        }
        return ret;
    }

    /**
     * Replies the set of selected referers. Never null, but may be empty.
     * @param referenceSet reference set
     *
     * @return the set of selected referers
     */
    public Set<OsmPrimitive> getChildPrimitives(Collection<? extends OsmPrimitive> referenceSet) {
        Set<OsmPrimitive> ret = new HashSet<>();
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
        Set<Integer> selectedIndices = new HashSet<>();
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
        BitSet selected = new BitSet();
        for (int row : selectedIndices) {
            selected.set(row);
        }
        addToSelectedMembers(selected);
        getSelectionModel().setValueIsAdjusting(false);
        // make the first selected member visible
        //
        if (!selectedIndices.isEmpty()) {
            fireMakeMemberVisible(Collections.min(selectedIndices));
        }
    }

    /**
     * Add one or more members indices to the selection.
     * Detect groups of consecutive indices.
     * Only one costly call of addSelectionInterval is performed for each group

     * @param selectedIndices selected indices as a bitset
     * @return number of groups
     */
    private int addToSelectedMembers(BitSet selectedIndices) {
        if (selectedIndices == null || selectedIndices.isEmpty()) {
            return 0;
        }
        // select the members
        //
        int start = selectedIndices.nextSetBit(0);
        int end;
        int steps = 0;
        int last = selectedIndices.length();
        while (start >= 0) {
            end = selectedIndices.nextClearBit(start);
            steps++;
            getSelectionModel().addSelectionInterval(start, end-1);
            start = selectedIndices.nextSetBit(end);
            if (start < 0 || end == last)
                break;
        }
        return steps;
    }

    /**
     * Replies true if the index-th relation members refers
     * to an editable relation, i.e. a relation which is not
     * incomplete.
     *
     * @param index the index
     * @return true, if the index-th relation members refers
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
        Set<OsmPrimitive> referrers = new HashSet<>();
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
     * Selects all members which refer to {@link OsmPrimitive}s in the collections
     * <code>primitmives</code>. Does nothing is primitives is null.
     *
     * @param primitives the collection of primitives
     */
    public void selectMembersReferringTo(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        BitSet selected = new BitSet();
        for (int i = 0; i < members.size(); i++) {
            RelationMember m = members.get(i);
            if (primitives.contains(m.getMember())) {
                selected.set(i);
            }
        }
        addToSelectedMembers(selected);
        getSelectionModel().setValueIsAdjusting(false);
        int[] selectedIndices = getSelectedIndices();
        if (selectedIndices.length > 0) {
            fireMakeMemberVisible(selectedIndices[0]);
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
     * Sort the selected relation members by the way they are linked.
     */
    @Override
    public void sort() {
        List<RelationMember> selectedMembers = new ArrayList<>(getSelectedMembers());
        List<RelationMember> sortedMembers;
        List<RelationMember> newMembers;
        if (selectedMembers.size() <= 1) {
            newMembers = relationSorter.sortMembers(members);
            sortedMembers = newMembers;
        } else {
            sortedMembers = relationSorter.sortMembers(selectedMembers);
            List<Integer> selectedIndices = ArrayUtils.toList(getSelectedIndices());
            newMembers = new ArrayList<>();
            boolean inserted = false;
            for (int i = 0; i < members.size(); i++) {
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

        if (members.size() != newMembers.size())
            throw new AssertionError();

        members.clear();
        members.addAll(newMembers);
        fireTableDataChanged();
        setSelectedMembers(sortedMembers);
    }

    /**
     * Sort the selected relation members and all members below by the way they are linked.
     */
    public void sortBelow() {
        final List<RelationMember> subList = members.subList(Math.max(0, getSelectionModel().getMinSelectionIndex()), members.size());
        final List<RelationMember> sorted = relationSorter.sortMembers(subList);
        subList.clear();
        subList.addAll(sorted);
        fireTableDataChanged();
        setSelectedMembers(sorted);
    }

    WayConnectionType getWayConnection(int i) {
        try {
            if (connectionType == null) {
                connectionType = wayConnectionTypeCalculator.updateLinks(members);
            }
            return connectionType.get(i);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("i", i).put("members", members).put("relation", relation);
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        invalidateConnectionType();
    }

    private void invalidateConnectionType() {
        connectionType = null;
    }

    /**
     * Reverse the relation members.
     */
    @Override
    public void reverse() {
        List<Integer> selectedIndices = ArrayUtils.toList(getSelectedIndices());
        List<Integer> selectedIndicesReversed = ArrayUtils.toList(getSelectedIndices());

        if (selectedIndices.size() <= 1) {
            Collections.reverse(members);
            fireTableDataChanged();
            setSelectedMembers(members);
        } else {
            Collections.reverse(selectedIndicesReversed);

            List<RelationMember> newMembers = new ArrayList<>(members);

            for (int i = 0; i < selectedIndices.size(); i++) {
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
