// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs.validator;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Bag;
import org.openstreetmap.josm.data.validation.util.MultipleNameVisitor;

/**
 * A panel that displays the error tree. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author frsantos
 */
public class ValidatorTreePanel extends JTree {
    /** Serializable ID */
    private static final long serialVersionUID = 2952292777351992696L;

    /**
     * The validation data.
     */
    protected DefaultTreeModel valTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    /** The list of errors shown in the tree */
    private List<TestError> errors = new ArrayList<TestError>();

    /**
     * If {@link #filter} is not <code>null</code> only errors are displayed
     * that refer to one of the primitives in the filter.
     */
    private Set<OsmPrimitive> filter = null;

    private int updateCount;

    /**
     * Constructor
     * @param errors The list of errors
     */
    public ValidatorTreePanel(List<TestError> errors) {
        ToolTipManager.sharedInstance().registerComponent(this);
        this.setModel(valTreeModel);
        this.setRootVisible(false);
        this.setShowsRootHandles(true);
        this.expandRow(0);
        this.setVisibleRowCount(8);
        this.setCellRenderer(new ValidatorTreeRenderer());
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        setErrorList(errors);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        String res = null;
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object nodeInfo = node.getUserObject();

            if (nodeInfo instanceof TestError) {
                TestError error = (TestError) nodeInfo;
                MultipleNameVisitor v = new MultipleNameVisitor();
                v.visit(error.getPrimitives());
                res = "<html>" + v.getText() + "<br>" + error.getMessage();
                String d = error.getDescription();
                if (d != null)
                    res += "<br>" + d;
                res += "</html>";
            } else {
                res = node.toString();
            }
        }
        return res;
    }

    /** Constructor */
    public ValidatorTreePanel() {
        this(null);
    }

    @Override
    public void setVisible(boolean v) {
        if (v) {
            buildTree();
        } else {
            valTreeModel.setRoot(new DefaultMutableTreeNode());
        }
        super.setVisible(v);
    }

    /**
     * Builds the errors tree
     */
    public void buildTree() {
        updateCount++;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();

        if (errors == null || errors.isEmpty()) {
            valTreeModel.setRoot(rootNode);
            return;
        }

        // Remember the currently expanded rows
        Set<Object> oldSelectedRows = new HashSet<Object>();
        Enumeration<TreePath> expanded = getExpandedDescendants(new TreePath(getRoot()));
        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                TreePath path = expanded.nextElement();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof Severity) {
                    oldSelectedRows.add(userObject);
                } else if (userObject instanceof String) {
                    String msg = (String) userObject;
                    msg = msg.substring(0, msg.lastIndexOf(" ("));
                    oldSelectedRows.add(msg);
                }
            }
        }

        Map<Severity, Bag<String, TestError>> errorTree = new HashMap<Severity, Bag<String, TestError>>();
        Map<Severity, HashMap<String, Bag<String, TestError>>> errorTreeDeep = new HashMap<Severity, HashMap<String, Bag<String, TestError>>>();
        for (Severity s : Severity.values()) {
            errorTree.put(s, new Bag<String, TestError>(20));
            errorTreeDeep.put(s, new HashMap<String, Bag<String, TestError>>());
        }

        for (TestError e : errors) {
            if (e.getIgnored()) {
                continue;
            }
            Severity s = e.getSeverity();
            String d = e.getDescription();
            String m = e.getMessage();
            if (filter != null) {
                boolean found = false;
                for (OsmPrimitive p : e.getPrimitives()) {
                    if (filter.contains(p)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
            }
            if (d != null) {
                Bag<String, TestError> b = errorTreeDeep.get(s).get(m);
                if (b == null) {
                    b = new Bag<String, TestError>(20);
                    errorTreeDeep.get(s).put(m, b);
                }
                b.add(d, e);
            } else {
                errorTree.get(s).add(m, e);
            }
        }

        List<TreePath> expandedPaths = new ArrayList<TreePath>();
        for (Severity s : Severity.values()) {
            Bag<String, TestError> severityErrors = errorTree.get(s);
            Map<String, Bag<String, TestError>> severityErrorsDeep = errorTreeDeep.get(s);
            if (severityErrors.isEmpty() && severityErrorsDeep.isEmpty()) {
                continue;
            }

            // Severity node
            DefaultMutableTreeNode severityNode = new DefaultMutableTreeNode(s);
            rootNode.add(severityNode);

            if (oldSelectedRows.contains(s)) {
                expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode }));
            }

            for (Entry<String, List<TestError>> msgErrors : severityErrors.entrySet()) {
                // Message node
                List<TestError> errs = msgErrors.getValue();
                String msg = msgErrors.getKey() + " (" + errs.size() + ")";
                DefaultMutableTreeNode messageNode = new DefaultMutableTreeNode(msg);
                severityNode.add(messageNode);

                if (oldSelectedRows.contains(msgErrors.getKey())) {
                    expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode, messageNode }));
                }

                for (TestError error : errs) {
                    // Error node
                    DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode(error);
                    messageNode.add(errorNode);
                }
            }
            for (Entry<String, Bag<String, TestError>> bag : severityErrorsDeep.entrySet()) {
                // Group node
                Bag<String, TestError> errorlist = bag.getValue();
                DefaultMutableTreeNode groupNode = null;
                if (errorlist.size() > 1) {
                    String nmsg = bag.getKey() + " (" + errorlist.size() + ")";
                    groupNode = new DefaultMutableTreeNode(nmsg);
                    severityNode.add(groupNode);
                    if (oldSelectedRows.contains(bag.getKey())) {
                        expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode, groupNode }));
                    }
                }

                for (Entry<String, List<TestError>> msgErrors : errorlist.entrySet()) {
                    // Message node
                    List<TestError> errs = msgErrors.getValue();
                    String msg;
                    if (groupNode != null) {
                        msg = msgErrors.getKey() + " (" + errs.size() + ")";
                    } else {
                        msg = bag.getKey() + " - " + msgErrors.getKey() + " (" + errs.size() + ")";
                    }
                    DefaultMutableTreeNode messageNode = new DefaultMutableTreeNode(msg);
                    if (groupNode != null) {
                        groupNode.add(messageNode);
                    } else {
                        severityNode.add(messageNode);
                    }

                    if (oldSelectedRows.contains(msgErrors.getKey())) {
                        if (groupNode != null) {
                            expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode, groupNode,
                                    messageNode }));
                        } else {
                            expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode, messageNode }));
                        }
                    }

                    for (TestError error : errs) {
                        // Error node
                        DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode(error);
                        messageNode.add(errorNode);
                    }
                }
            }
        }

        valTreeModel.setRoot(rootNode);
        for (TreePath path : expandedPaths) {
            this.expandPath(path);
        }
    }

    /**
     * Sets the errors list used by a data layer
     * @param errors The error list that is used by a data layer
     */
    public void setErrorList(List<TestError> errors) {
        this.errors = errors;
        if (isVisible()) {
            buildTree();
        }
    }

    /**
     * Clears the current error list and adds these errors to it
     * @param errors The validation errors
     */
    public void setErrors(List<TestError> newerrors) {
        if (errors == null)
            return;
        errors.clear();
        for (TestError error : newerrors) {
            if (!error.getIgnored()) {
                errors.add(error);
            }
        }
        if (isVisible()) {
            buildTree();
        }
    }

    /**
     * Returns the errors of the tree
     * @return  the errors of the tree
     */
    public List<TestError> getErrors() {
        return errors != null ? errors : Collections.<TestError> emptyList();
    }

    public Set<OsmPrimitive> getFilter() {
        return filter;
    }

    public void setFilter(Set<OsmPrimitive> filter) {
        if (filter != null && filter.isEmpty()) {
            this.filter = null;
        } else {
            this.filter = filter;
        }
        if (isVisible()) {
            buildTree();
        }
    }

    /**
     * Updates the current errors list
     * @param errors The validation errors
     */
    public void resetErrors() {
        List<TestError> e = new ArrayList<TestError>(errors);
        setErrors(e);
    }

    /**
     * Expands all tree
     */
    @SuppressWarnings("unchecked")
    public void expandAll() {
        DefaultMutableTreeNode root = getRoot();

        int row = 0;
        Enumeration<DefaultMutableTreeNode> children = root.breadthFirstEnumeration();
        while (children.hasMoreElements()) {
            children.nextElement();
            expandRow(row++);
        }
    }

    /**
     * Returns the root node model.
     * @return The root node model
     */
    public DefaultMutableTreeNode getRoot() {
        return (DefaultMutableTreeNode) valTreeModel.getRoot();
    }

    public int getUpdateCount() {
        return updateCount;
    }
}
