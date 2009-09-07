package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class MemberTableModel extends AbstractTableModel {

    private ArrayList<RelationMember> members;
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
    }

    public void addMemberModelListener(IMemberModelListener listener) {
        synchronized (listeners) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeMemberModelListener(IMemberModelListener listener) {
        synchronized (listeners) {
            if (listener != null && listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    protected void fireMakeMemberVisible(int index) {
        synchronized (listeners) {
            for (IMemberModelListener listener : listeners) {
                listener.makeMemberVisible(index);
            }
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
            return linked(rowIndex);
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
        member.role = value.toString();
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

    public void updateMemberReferences(DataSet ds) {
        for (RelationMember member : members) {
            if (member.getMember().getId() == 0) {
                continue;
            }
            OsmPrimitive primitive = ds.getPrimitiveById(member.getMember().getId(), OsmPrimitiveType.from(member.getMember()));
            if (primitive != null) {
                member.member = primitive;
            }
        }
        fireTableDataChanged();
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

    public boolean hasIncompleteMembers() {
        for (RelationMember member : members) {
            if (member.getMember().incomplete)
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
            members.get(row).role = role;
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
     * Replies true, if the selected {@see OsmPrimitive}s in the layer belonging
     * to this model are in sync with the selected referers in this model.
     *
     * @return
     */
    public boolean selectionsAreInSync() {
        HashSet<OsmPrimitive> s1 = new HashSet<OsmPrimitive>(getSelectedChildPrimitives());
        if (s1.size() != layer.data.getSelected().size()) return false;
        s1.removeAll(layer.data.getSelected());
        return s1.isEmpty();
    }
    /**
     * Selects the members in the collection selectedMembers
     *
     * @param selectedMembers the collection of selected members
     */
    public void setSelectedMembers(Collection<RelationMember> selectedMembers) {
        if (selectedMembers == null || selectedMembers.isEmpty())
            return;

        // lookup the indices for the respective members
        //
        ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
        for (RelationMember member : selectedMembers) {
            for (int idx = 0; idx < members.size(); idx ++) {
                if (members.get(idx).equals(member)) {
                    if (!selectedIndices.contains(idx)) {
                        selectedIndices.add(idx);
                    }
                }
            }
        }

        // select the members
        //
        Collections.sort(selectedIndices);
        getSelectionModel().setValueIsAdjusting(true);
        getSelectionModel().clearSelection();
        for (int row : selectedIndices) {
            getSelectionModel().addSelectionInterval(row, row);
        }
        getSelectionModel().setValueIsAdjusting(false);

        // make the first selected member visible
        //
        if (selectedIndices.size() > 0) {
            fireMakeMemberVisible(selectedIndices.get(0));
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
        return !r.incomplete;
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
        Node    result = null;

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

    void sort() {
        RelationNodeMap map = new RelationNodeMap(members);
        Vector<LinkedList<Integer>> segments;
        LinkedList<Integer> segment;
        Node startSearchNode;
        Node endSearchNode;
        boolean something_done;

        /*
         * sort any 2 or more connected elements together may be slow with many unconnected members
         */

        if (map.isEmpty())
            // empty relation or incomplete members
            return;
        segments = new Vector<LinkedList<Integer>>();

        while (!map.isEmpty()) {
            // find an element for the next segment
            // try first element in relation if we just started
            // otherwise, or if first element is another relation, just fetch some element from the
            // map
            Integer next;
            if ((segments.size() == 0) && map.remove(0, members.get(0))) {
                next = 0;
            } else {
                next = map.pop();
                if (next == null) {
                    break;
                }
            }

            segment = new LinkedList<Integer>();
            segment.add(next);
            segments.add(segment);

            do {
                something_done = false;
                startSearchNode = null;
                endSearchNode = null;
                if (segment.size() == 1) {
                    // only one element in segment, so try to link against each linkable node of element
                    RelationMember m = members.get(segment.getFirst());
                    if (m.isWay()) {
                        Way w = m.getWay();
                        endSearchNode = w.lastNode();
                        if (w.lastNode() != w.firstNode())
                        {
                            startSearchNode = w.firstNode();
                        }
                    } else if (m.isNode()) {
                        Node n = m.getNode();
                        endSearchNode = n;
                    }
                } else {
                    // add unused node of first element and unused node of last element
                    // start with the first element (compared to next element)
                    startSearchNode = getUnusedNode(members.get(segment.getFirst()), members.get(segment.get(1)));

                    // now the same for the last element (compared to previous element)
                    endSearchNode = getUnusedNode(members.get(segment.getLast()), members.get(segment.get(segment.size() - 2)));
                }

                // let's see if we can find connected elements for endSearchNode and startSearchNode
                if (startSearchNode != null) {
                    Integer m2 = map.find(startSearchNode, segment.getFirst());
                    if (m2 != null) {
                        segment.add(0, m2);
                        map.remove(m2, members.get(m2));
                        something_done = true;
                    }
                }
                if (endSearchNode != null) {
                    Integer m2 = map.find(endSearchNode, segment.getLast());
                    if (m2 != null) {
                        segment.add(segment.size(), m2);
                        map.remove(m2, members.get(m2));
                        something_done = true;
                    }
                }
            } while (something_done);

        }

        if (segments.size() > 0) {
            // append map.remaining() to segments list (as a single segment)
            segment = new LinkedList<Integer>();
            segment.addAll(map.getRemaining());
            segments.add(segment);

            // now we need to actually re-order the relation members
            ArrayList<RelationMember> newmembers = new ArrayList<RelationMember>();
            for (LinkedList<Integer> segment2 : segments) {
                for (Integer p : segment2) {
                    newmembers.add(members.get(p));
                }
            }
            members.clear();
            members.addAll(newmembers);

            fireTableDataChanged();
        }
    }

    // simple version of code that was removed from GenericReleationEditor
    // no recursion and no forward/backward support
    // TODO: add back the number of linked elements
    private WayConnectionType linked(int i) {
        // this method is aimed at finding out whether the
        // relation member is "linked" with the next, i.e. whether
        // (if both are ways) these ways are connected. It should
        // really produce a much more beautiful output (with a linkage
        // symbol somehow placed between the two member lines!),
        // so... FIXME ;-)

        WayConnectionType link = WayConnectionType.none;
        RelationMember m1 = members.get(i);
        RelationMember m2 = members.get((i + 1) % members.size());
        Way way1 = null;
        Way way2 = null;

        if (m1.isWay()) {
            way1 = m1.getWay();
        }
        if (m2.isWay()) {
            way2 = m2.getWay();
        }
        if ((way1 != null) && (way2 != null)) {
            Node way1first = way1.firstNode();
            Node way1last = way1.lastNode();
            Node way2first = way2.firstNode();
            Node way2last = way2.lastNode();
            if (way1first != null && way2first != null && (way1first == way2first)) {
                link = WayConnectionType.tail_to_tail;
            } else if (way1first != null && way2last != null && (way1first == way2last)) {
                link = WayConnectionType.tail_to_head;
            } else if (way1last != null && way2first != null && (way1last == way2first)) {
                link = WayConnectionType.head_to_tail;
            } else if (way1last != null && way2last != null && (way1last == way2last)) {
                link = WayConnectionType.head_to_head;
            }
        }

        return link;
    }
}
