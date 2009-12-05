// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * This is a {@see TreeModel} which provides the hierarchical structure of {@see Relation}s
 * to a  {@see JTree}.
 *
 * The model is initialized with a root relation or with a list of {@see RelationMember}s, see
 * {@see #populate(Relation)} and {@see #populate(List)} respectively.
 *
 *
 */
public class RelationTreeModel implements TreeModel {
    private static final Logger logger = Logger.getLogger(RelationTreeModel.class.getName());

    /** the root relation */
    private Relation root;

    /** the tree model listeners */
    private CopyOnWriteArrayList<TreeModelListener> listeners;

    /**
     * constructor
     */
    public RelationTreeModel() {
        this.root = null;
        listeners = new CopyOnWriteArrayList<TreeModelListener>();
    }

    /**
     * constructor
     * @param root the root relation
     */
    public RelationTreeModel(Relation root) {
        this.root = root;
        listeners = new CopyOnWriteArrayList<TreeModelListener>();
    }

    /**
     * constructor
     *
     * @param members a list of members
     */
    public RelationTreeModel(List<RelationMember> members) {
        if (members == null) return;
        Relation root = new Relation();
        root.setMembers(members);
        this.root = root;
        listeners = new CopyOnWriteArrayList<TreeModelListener>();
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
        int count = 0;
        for(RelationMember member : parent.getMembers()) {
            if (member.isRelation()) {
                count++;
            }
        }
        return count;
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
        int count=0;
        for (RelationMember member : parent.getMembers()) {
            if (!(member.isRelation())) {
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
            if (!(member.isRelation())) {
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
    public Object getChild(Object parent, int index) {
        return getRelationChildByIdx((Relation)parent, index);
    }

    public int getChildCount(Object parent) {
        return getNumRelationChildren((Relation)parent);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return getIndexForRelationChild((Relation)parent, (Relation)child);
    }

    public Object getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        Relation r = (Relation)node;
        if (r.isIncomplete()) return false;
        return getNumRelationChildren(r) == 0;
    }

    public void addTreeModelListener(TreeModelListener l) {
        synchronized (listeners) {
            if (l != null && !listeners.contains(l)) {
                listeners.add(l);
            }
        }
    }

    public void removeTreeModelListener(TreeModelListener l) {
        synchronized (listeners) {
            if (l != null && listeners.contains(l)) {
                listeners.remove(l);
            }
        }
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        // do nothing
    }
}
