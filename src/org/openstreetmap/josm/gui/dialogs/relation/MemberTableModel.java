// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

public class MemberTableModel extends AbstractTableModel{

    private Relation relation;
    private ArrayList<RelationMember> members;
    private ArrayList<String> memberLinkingInfo;
    private DefaultListSelectionModel listSelectionModel;
    private CopyOnWriteArrayList<IMemberModelListener> listeners;

    /**
     * constructor
     */
    public MemberTableModel(){
        members = new ArrayList<RelationMember>();
        memberLinkingInfo = new ArrayList<String>();
        listeners = new CopyOnWriteArrayList<IMemberModelListener>();
    }

    public void addMemberModelListener(IMemberModelListener listener) {
        synchronized(listeners) {
            if (listener != null && ! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeMemberModelListener(IMemberModelListener listener) {
        synchronized(listeners) {
            if (listener != null && listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    protected void fireMakeMemberVisible(int index) {
        synchronized(listeners) {
            for (IMemberModelListener listener: listeners) {
                listener.makeMemberVisible(index);
            }
        }
    }

    public void populate(Relation relation) {
        members.clear();
        if (relation != null && relation.members != null) {
            members.addAll(relation.members);
        }
        this.relation = relation;
        fireTableDataChanged();
    }

    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        return members.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch(columnIndex) {
        case 0: return members.get(rowIndex).role;
        case 1: return members.get(rowIndex).member;
        case 2: return "";
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
        return members.get(idx).member;
    }

    public void moveUp(int[] selectedRows) {
        if (! canMoveUp(selectedRows))
            return;

        for (int row : selectedRows) {
            RelationMember member1 = members.get(row);
            RelationMember member2 = members.get(row-1);
            members.set(row, member2);
            members.set(row-1, member1);
        }
        fireTableDataChanged();
        listSelectionModel.clearSelection();
        for (int row : selectedRows) {
            row--;
            listSelectionModel.addSelectionInterval(row, row);
        }
        fireMakeMemberVisible(selectedRows[0] -1);
    }

    public void moveDown(int[] selectedRows) {
        if (! canMoveDown(selectedRows))
            return;

        for (int i=selectedRows.length-1; i >=0; i--) {
            int row = selectedRows[i];
            RelationMember member1 = members.get(row);
            RelationMember member2 = members.get(row+1);
            members.set(row, member2);
            members.set(row+1, member1);
        }
        fireTableDataChanged();
        listSelectionModel.clearSelection();
        for (int row : selectedRows) {
            row++;
            listSelectionModel.addSelectionInterval(row, row);
        }
        fireMakeMemberVisible(selectedRows[0] + 1);
    }

    public void remove(int[] selectedRows) {
        if (! canRemove(selectedRows))
            return;
        int offset = 0;
        for (int row : selectedRows) {
            row -= offset;
            members.remove(row);
            offset++;
        }
        fireTableDataChanged();
    }

    public boolean canMoveUp(int [] rows) {
        if (rows == null || rows.length == 0) return false;
        Arrays.sort(rows);
        return rows[0] > 0 && members.size() > 0;
    }

    public boolean canMoveDown(int [] rows) {
        if (rows == null || rows.length == 0) return false;
        Arrays.sort(rows);
        return members.size() >0 && rows[rows.length-1] < members.size() - 1;
    }

    public boolean canRemove(int [] rows) {
        if (rows == null || rows.length == 0) return false;
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
            if (member.member.id == 0) {
                continue;
            }
            OsmPrimitive primitive = ds.getPrimitiveById(member.member.id);
            if (primitive != null) {
                member.member = primitive;
            }
        }
        fireTableDataChanged();
    }

    public void removeMembersReferringTo(List<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        Iterator<RelationMember> it = members.iterator();
        while(it.hasNext()) {
            RelationMember member = it.next();
            if (primitives.contains(member.member)) {
                it.remove();
            }
        }
        fireTableDataChanged();
    }

    public void selectMembers(Collection<RelationMember> selectedMembers) {
        if (selectedMembers == null) return;
        int min = Integer.MAX_VALUE;
        for (RelationMember member: selectedMembers) {
            int row = members.indexOf(member);
            if (row >= 0) {
                listSelectionModel.addSelectionInterval(row,row);
                min = Math.min(row, min);
            }
        }
        if (min < Integer.MAX_VALUE) {
            fireMakeMemberVisible(min);
        }
    }

    public void applyToRelation(Relation relation) {
        relation.members.clear();
        relation.members.addAll(members);
    }

    public boolean hasSameMembersAs(Relation relation) {
        if (relation == null) return false;
        if (relation.members.size() != members.size()) return false;
        for (int i=0; i<relation.members.size();i++) {
            if (! relation.members.get(i).equals(members.get(i)))
                return false;
        }
        return true;
    }

    public boolean hasIncompleteMembers() {
        for (RelationMember member: members) {
            if (member.member.incomplete)
                return true;
        }
        return false;
    }

    protected List<Integer> getSelectedIndices() {
        ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
        for (int i=0; i< members.size();i++) {
            if (getSelectionModel().isSelectedIndex(i)) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    public void addMembersAtBeginning(List<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        for (OsmPrimitive primitive: primitives) {
            RelationMember member = new RelationMember(null,primitive);
            members.add(0,member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i=0; i<primitives.size();i++) {
            getSelectionModel().addSelectionInterval(i,i);
        }
        fireMakeMemberVisible(0);
    }

    public void addMembersAtEnd(List<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;

        for (OsmPrimitive primitive: primitives) {
            RelationMember member = new RelationMember(null,primitive);
            members.add(member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i=0; i<primitives.size();i++) {
            getSelectionModel().addSelectionInterval(members.size()-1-i,members.size()-1-i);
        }
        fireMakeMemberVisible(members.size() -1);
    }

    public void addMembersBeforeIdx(List<? extends OsmPrimitive> primitives, int idx) {
        if (primitives == null) return;

        for (OsmPrimitive primitive: primitives) {
            RelationMember member = new RelationMember(null,primitive);
            members.add(idx,member);
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i=0; i<primitives.size();i++) {
            getSelectionModel().addSelectionInterval(idx+i,idx+i);
        }
        fireMakeMemberVisible(idx);
    }

    public void addMembersAfterIdx(List<? extends OsmPrimitive> primitives, int idx) {
        if (primitives == null) return;
        int j =1;
        for (OsmPrimitive primitive: primitives) {
            RelationMember member = new RelationMember(null,primitive);
            members.add(idx+j,member);
            j++;
        }
        fireTableDataChanged();
        getSelectionModel().clearSelection();
        for (int i=0; i<primitives.size();i++) {
            getSelectionModel().addSelectionInterval(idx+1 + i,idx+1 +i);
        }
        fireMakeMemberVisible(idx+1);
    }
}
