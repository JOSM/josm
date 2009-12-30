// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.BACKWARD;
import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.FORWARD;
import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.NONE;
import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.ROUNDABOUT_LEFT;
import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.ROUNDABOUT_RIGHT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class MemberTableModel extends AbstractTableModel implements TableModelListener, SelectionChangedListener, DataChangeListener, DataSetListener{

    /**
     * data of the table model: The list of members and the cached WayConnectionType of each member.
     **/
    private ArrayList<RelationMember> members;
    private ArrayList<WayConnectionType> connectionType = null;

    private DefaultListSelectionModel listSelectionModel;
    private CopyOnWriteArrayList<IMemberModelListener> listeners;
    private OsmDataLayer layer;

    /**
     * constructor
     */
    public MemberTableModel(OsmDataLayer layer) {
        members = new ArrayList<RelationMember>();
        listeners = new CopyOnWriteArrayList<IMemberModelListener>();
        this.layer = layer;
        addTableModelListener(this);
    }

    public OsmDataLayer getLayer() {
        return layer;
    }

    /* --------------------------------------------------------------------------- */
    /* Interface SelectionChangedListener                                          */
    /* --------------------------------------------------------------------------- */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (Main.main.getEditLayer() != this.layer) return;
        // just trigger a repaint
        Collection<RelationMember> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    /* --------------------------------------------------------------------------- */
    /* Interface DataChangeListener                                                */
    /* --------------------------------------------------------------------------- */
    public void dataChanged(OsmDataLayer l) {
        if (l != this.layer) return;
        // just trigger a repaint
        Collection<RelationMember> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }
    /* --------------------------------------------------------------------------- */
    /* Interface DataSetListener                                                   */
    /* --------------------------------------------------------------------------- */
    public void dataChanged(DataChangedEvent event) {
        // just trigger a repaint - the display name of the relation members may
        // have changed
        Collection<RelationMember> sel = getSelectedMembers();
        fireTableDataChanged();
        setSelectedMembers(sel);
    }

    public void nodeMoved(NodeMovedEvent event) {/* ignore */}
    public void primtivesAdded(PrimitivesAddedEvent event) {/* ignore */}

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {
        // ignore - the relation in the editor might become out of sync with the relation
        // in the dataset. We will deal with it when the relation editor is closed or
        // when the changes in the editor are applied.
    }

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

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignore */}

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

    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        return members.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return members.get(rowIndex).getRole();
        case 1:
            return members.get(rowIndex).getMember();
        case 2:
            return wayConnection(rowIndex);
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
            members.remove(row);
            offset++;
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
        ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
        for (int i = 0; i < members.size(); i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    public void addMembersAtBeginning(List<OsmPrimitive> primitives) {
        if (primitives == null)
            return;
        for (OsmPrimitive primitive : primitives) {
            RelationMember member = new RelationMember("", primitive);
            members.add(0, member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i = 0; i < primitives.size(); i++) {
            getSelectionModel().addSelectionInterval(i, i);
        }
        fireMakeMemberVisible(0);
    }

    public void addMembersAtEnd(List<? extends OsmPrimitive> primitives) {
        if (primitives == null)
            return;

        for (OsmPrimitive primitive : primitives) {
            RelationMember member = new RelationMember("", primitive);
            members.add(member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i = 0; i < primitives.size(); i++) {
            getSelectionModel().addSelectionInterval(members.size() - 1 - i, members.size() - 1 - i);
        }
        fireMakeMemberVisible(members.size() - 1);
    }

    public void addMembersBeforeIdx(List<? extends OsmPrimitive> primitives, int idx) {
        if (primitives == null)
            return;

        for (OsmPrimitive primitive : primitives) {
            RelationMember member = new RelationMember("", primitive);
            members.add(idx, member);
        }
        fireTableDataChanged();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int i = 0; i < primitives.size(); i++) {
            getSelectionModel().addSelectionInterval(idx + i, idx + i);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(idx);
    }

    public void addMembersAfterIdx(List<? extends OsmPrimitive> primitives, int idx) {
        if (primitives == null)
            return;
        int j = 1;
        for (OsmPrimitive primitive : primitives) {
            RelationMember member = new RelationMember("", primitive);
            members.add(idx + j, member);
            j++;
        }
        fireTableDataChanged();
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int i = 0; i < primitives.size(); i++) {
            getSelectionModel().addSelectionInterval(idx + 1 + i, idx + 1 + i);
        }
        getSelectionModel().setValueIsAdjusting(false);
        fireMakeMemberVisible(idx + 1);
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
        ArrayList<RelationMember> selectedMembers = new ArrayList<RelationMember>();
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
    public Set<OsmPrimitive> getSelectedChildPrimitives() {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
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
            int idx = members.indexOf(member);
            if ( idx >= 0) {
                selectedIndices.add(idx);
            }
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
        if (selectedIndices.size() > 0) {
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
     * Replies true if there is at least one relation member in this model
     * which refers to at least on the primitives in <code>primitives</code>.
     *
     * @param primitives the collection of primitives
     * @return true if there is at least one relation member in this model
     * which refers to at least on the primitives in <code>primitives</code>; false
     * otherwise
     */
    public boolean hasMembersReferringTo(Collection<OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty())
            return false;
        HashSet<OsmPrimitive> referrers = new HashSet<OsmPrimitive>();
        for(RelationMember member : members) {
            referrers.add(member.getMember());
        }
        Iterator<OsmPrimitive> it = primitives.iterator();
        while(it.hasNext()) {
            OsmPrimitive referred = it.next();
            if (referrers.contains(referred))
                return true;
        }
        return false;
    }

    /**
     * Selects all mebers which refer to {@see OsmPrimitive}s in the collections
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
        if (getSelectedIndices().size() > 0) {
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
        if (Main.map == null || Main.map.mapView == null) return false;
        return Main.map.mapView.getActiveLayer() == layer;
    }

    /**
     * get a node we can link against when sorting members
     * @param element the element we want to link against
     * @param linked_element already linked against element
     * @return the unlinked node if element is a way, the node itself if element is a node, null otherwise
     */
    private static Node getUnusedNode(RelationMember element, RelationMember linked_element)
    {
        Node result = null;

        if (element.isWay()) {
            Way w = element.getWay();
            if (linked_element.isWay()) {
                Way x = linked_element.getWay();
                if ((w.firstNode() == x.firstNode()) || (w.firstNode() == x.lastNode())) {
                    result = w.lastNode();
                } else {
                    result = w.firstNode();
                }
            } else if (linked_element.isNode()) {
                Node m = linked_element.getNode();
                if (w.firstNode() == m) {
                    result = w.lastNode();
                } else {
                    result = w.firstNode();
                }
            }
        } else if (element.isNode()) {
            Node n = element.getNode();
            result = n;
        }

        return result;
    }

    /*
     * Sort a collection of relation members by the way they are linked.
     * 
     * @param relationMembers collection of relation members
     * @return sorted collection of relation members
     */
    private ArrayList<RelationMember> sortMembers(ArrayList<RelationMember> relationMembers) {
        RelationNodeMap map = new RelationNodeMap(relationMembers);
        // List of groups of linked members
        //
        ArrayList<LinkedList<Integer>> allGroups = new ArrayList<LinkedList<Integer>>();

        // current group of members that are linked among each other
        // Two successive members are always linked i.e. have a common node.
        //
        LinkedList<Integer> group;

        Integer first;
        while ((first = map.pop()) != null) {
            group = new LinkedList<Integer>();
            group.add(first);

            allGroups.add(group);

            Integer next = first;
            while ((next = map.popAdjacent(next)) != null) {
                group.addLast(next);
            }

            // The first element need not be in front of the list.
            // So the search goes in both directions
            //
            next = first;
            while ((next = map.popAdjacent(next)) != null) {
                group.addFirst(next);
            }
        }

        group = new LinkedList<Integer>();
        group.addAll(map.getNotSortableMembers());
        allGroups.add(group);

        ArrayList<RelationMember> newMembers = new ArrayList<RelationMember>();
        for (LinkedList<Integer> tmpGroup : allGroups) {
            for (Integer p : tmpGroup) {
                newMembers.add(relationMembers.get(p));
            }
        }
        return newMembers;
    }

    /**
     * Sort the relation members by the way they are linked.
     */
    void sort() {
        ArrayList<RelationMember> selectedMembers = new ArrayList<RelationMember>(getSelectedMembers());
        ArrayList<RelationMember> sortedMembers = null;
        ArrayList<RelationMember> newMembers;
        if (selectedMembers.isEmpty()) {
            newMembers = sortMembers(members);
        } else {
            sortedMembers = sortMembers(selectedMembers);
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

    /**
     * Reverse the relation members.
     */
    void reverse() {
        Collections.reverse(members);
        fireTableDataChanged();
    }

    /**
     * Determines the direction of way k with respect to the way ref_i.
     * The way ref_i is assumed to have the direction ref_direction and
     * to be the predecessor of k.
     *
     * If both ways are not linked in any way, NONE is returned.
     *
     * Else the direction is given as follows:
     * Let the relation be a route of oneway streets, and someone travels them in the given order.
     * Direction is FORWARD for if it is legel and BACKWARD if it is illegal to do so for the given way.
     *
     **/
    private Direction determineDirection(int ref_i,Direction ref_direction, int k) {
        if (ref_i < 0 || k < 0 || ref_i >= members.size() || k >= members.size())
            return NONE;
        if (ref_direction == NONE)
            return NONE;

        RelationMember m_ref = members.get(ref_i);
        RelationMember m = members.get(k);
        Way way_ref = null;
        Way way = null;

        if (m_ref.isWay()) {
            way_ref = m_ref.getWay();
        }
        if (m.isWay()) {
            way = m.getWay();
        }

        if (way_ref == null || way == null)
            return NONE;

        /** the list of nodes the way k can dock to */
        List<Node> refNodes= new ArrayList<Node>();

        switch (ref_direction) {
        case FORWARD:
            refNodes.add(way_ref.lastNode());
            break;
        case BACKWARD:
            refNodes.add(way_ref.firstNode());
            break;
        case ROUNDABOUT_LEFT:
        case ROUNDABOUT_RIGHT:
            refNodes = way_ref.getNodes();
            break;
        }

        if (refNodes == null)
            return NONE;

        for (Node n : refNodes) {
            if (n == null) {
                continue;
            }
            if (roundaboutType(k) != NONE) {
                for (Node nn : way.getNodes()) {
                    if (n == nn)
                        return roundaboutType(k);
                }
            } else {
                if (n == way.firstNode())
                    return FORWARD;
                if (n == way.lastNode())
                    return BACKWARD;
            }
        }
        return NONE;
    }

    /**
     * determine, if the way i is a roundabout and if yes, what type of roundabout
     */
    private Direction roundaboutType(int i) { //FIXME
        RelationMember m = members.get(i);
        if (m == null || !m.isWay()) return NONE;
        Way w = m.getWay();
        return roundaboutType(w);
    }
    static Direction roundaboutType(Way w) {
        if (w != null &&
                "roundabout".equals(w.get("junction")) &&
                w.getNodesCount() < 200 &&
                w.getNodesCount() > 2 &&
                w.getNode(0) != null &&
                w.getNode(1) != null &&
                w.getNode(2) != null &&
                w.firstNode() == w.lastNode()) {
            /** do some simple determinant / cross pruduct test on the first 3 nodes
                to see, if the roundabout goes clock wise or ccw */
            EastNorth en1 = w.getNode(0).getEastNorth();
            EastNorth en2 = w.getNode(1).getEastNorth();
            EastNorth en3 = w.getNode(2).getEastNorth();
            en1 = en1.sub(en2);
            en2 = en2.sub(en3);
            return en1.north() * en2.east() - en2.north() * en1.east() > 0 ? ROUNDABOUT_LEFT : ROUNDABOUT_RIGHT;
        } else
            return NONE;
    }

    private WayConnectionType wayConnection(int i) {
        if (connectionType == null) {
            updateLinks();
        }
        return connectionType.get(i);
    }

    public void tableChanged(TableModelEvent e) {
        connectionType = null;
    }

    /**
     * refresh the cache of member WayConnectionTypes
     */
    public void updateLinks() {
        connectionType = null;
        ArrayList<WayConnectionType> con = new ArrayList<WayConnectionType>();

        for (int i=0; i<members.size(); ++i) {
            con.add(null);
        }

        int firstGroupIdx=0;
        boolean resetFirstGoupIdx=false;

        for (int i=0; i<members.size(); ++i) {
            if (resetFirstGoupIdx) {
                firstGroupIdx = i;
                resetFirstGoupIdx = false;
            }

            RelationMember m = members.get(i);
            if (! m.isWay()) {
                con.set(i, new WayConnectionType());
                resetFirstGoupIdx = true;
                continue;
            }

            Way w = m.getWay();
            if (w == null || w.isIncomplete()) {
                con.set(i, new WayConnectionType());
                resetFirstGoupIdx = true;
                continue;
            }

            boolean linkPrev = (i != firstGroupIdx);
            boolean linkNext;
            Direction dir;
            if (linkPrev) {
                dir = determineDirection(i-1, con.get(i-1).direction, i);
                linkNext = (determineDirection(i, dir, i+1) != NONE);
            }
            else {
                if (roundaboutType(i) != NONE) {
                    dir = determineDirection(i, roundaboutType(i), i+1) != NONE ? roundaboutType(i) : NONE;
                } else { /** guess the direction and see if it fits with the next member */
                    dir = determineDirection(i, FORWARD, i+1) != NONE ? FORWARD : NONE;
                    if (dir == NONE) {
                        dir = determineDirection(i, BACKWARD, i+1) != NONE ? BACKWARD : NONE;
                    }
                }
                linkNext = (dir != NONE);
                if (dir == NONE) {
                    if (roundaboutType(i) != NONE) {
                        dir = roundaboutType(i);
                    }
                }

            }

            con.set(i, new WayConnectionType(linkPrev, linkNext, dir));

            if (! linkNext) {
                boolean loop;
                if (i == firstGroupIdx) {
                    loop = determineDirection(i, FORWARD, i) == FORWARD;
                } else {
                    loop = determineDirection(i, dir, firstGroupIdx) == con.get(firstGroupIdx).direction;
                }
                if (loop) {
                    for (int j=firstGroupIdx; j <= i; ++j) {
                        con.get(j).isLoop = true;
                    }
                }
                resetFirstGoupIdx = true;
            }
        }
        connectionType = con;
        //        for (int i=0; i<con.size(); ++i) {
        //            System.err.println(con.get(i));
        //        }
    }
}
