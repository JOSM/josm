// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * This is a {@link TreeModel} which provides the hierarchical structure of {@link Relation}s
 * to a {@link javax.swing.JTree}.
 *
 * The model is initialized with a root relation or with a list of {@link RelationMember}s, see
 * {@link #populate(Relation)} and {@link #populate(List)} respectively.
 *
 * @since 1828
 */
public class RelationTreeModel implements TreeModel {
    /** the root relation */
    private Relation root;

    /** the tree model listeners */
    private final CopyOnWriteArrayList<TreeModelListener> listeners;

    /**
     * constructor
     */
    public RelationTreeModel() {
        this.root = null;
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Replies the number of children of type relation for a particular
     * relation <code>parent</code>
     *
     * @param parent the parent relation
     * @return the number of children of type relation
     */
    protected int getNumRelationChildren(Relation parent) {
        if (parent == null) return 0;
        return (int) parent.getMembers().stream().filter(RelationMember::isRelation).count();
    }

    /**
     * Replies the i-th child of type relation for a particular relation
     * <code>parent</code>.
     *
     * @param parent the parent relation
     * @param idx the index
     * @return the i-th child of type relation for a particular relation
     * <code>parent</code>; null, if no such child exists
     */
    protected Relation getRelationChildByIdx(Relation parent, int idx) {
        if (parent == null) return null;
        int count = 0;
        for (RelationMember member : parent.getMembers()) {
            if (!member.isRelation()) {
                continue;
            }
            if (count == idx)
                return member.getRelation();
            count++;
        }
        return null;
    }

    /**
     * Replies the index of a particular <code>child</code> with respect to its
     * <code>parent</code>.
     *
     * @param parent  the parent relation
     * @param child the child relation
     * @return the index of a particular <code>child</code> with respect to its
     * <code>parent</code>; -1 if either parent or child are null or if <code>child</code>
     * isn't a child of <code>parent</code>.
     *
     */
    protected int getIndexForRelationChild(Relation parent, Relation child) {
        if (parent == null || child == null) return -1;
        int idx = 0;
        for (RelationMember member : parent.getMembers()) {
            if (!member.isRelation()) {
                continue;
            }
            if (member.getMember() == child) return idx;
            idx++;
        }
        return -1;
    }

    /**
     * Populates the model with a root relation
     *
     * @param root the root relation
     * @see #populate(List)
     *
     */
    public void populate(Relation root) {
        if (root == null) {
            root = new Relation();
        }
        this.root = root;
        fireRootReplacedEvent();
    }

    /**
     * Populates the model with a list of relation members
     *
     * @param members the relation members
     */
    public void populate(List<RelationMember> members) {
        if (members == null) return;
        Relation r = new Relation();
        r.setMembers(members);
        this.root = r;
        fireRootReplacedEvent();
    }

    /**
     * Notifies tree model listeners about a replacement of the
     * root.
     */
    protected void fireRootReplacedEvent() {
        TreeModelEvent e = new TreeModelEvent(this, new TreePath(root));
        for (TreeModelListener l : listeners) {
            l.treeStructureChanged(e);
        }
    }

    /**
     * Notifies tree model listeners about an update of the
     * trees nodes.
     *
     * @param path the tree path to the node
     */
    protected void fireRefreshNode(TreePath path) {
        TreeModelEvent e = new TreeModelEvent(this, path);
        for (TreeModelListener l : listeners) {
            l.treeStructureChanged(e);
        }

    }

    /**
     * Invoke to notify all listeners about an update of a particular node
     *
     * @param pathToNode the tree path to the node
     */
    public void refreshNode(TreePath pathToNode) {
        fireRefreshNode(pathToNode);
    }

    /* ----------------------------------------------------------------------- */
    /* interface TreeModel                                                     */
    /* ----------------------------------------------------------------------- */
    @Override
    public Object getChild(Object parent, int index) {
        return getRelationChildByIdx((Relation) parent, index);
    }

    @Override
    public int getChildCount(Object parent) {
        return getNumRelationChildren((Relation) parent);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return getIndexForRelationChild((Relation) parent, (Relation) child);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        Relation r = (Relation) node;
        if (r.isIncomplete()) return false;
        return getNumRelationChildren(r) == 0;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        if (l != null) {
            listeners.addIfAbsent(l);
        }
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // do nothing
    }
}
