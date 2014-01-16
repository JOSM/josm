// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.MultipleNameVisitor;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * A panel that displays the error tree. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author frsantos
 */
public class ValidatorTreePanel extends JTree implements Destroyable {
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

    /** a counter to check if tree has been rebuild */
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
        for (KeyListener keyListener : getKeyListeners()) {
            // Fix #3596 - Remove default keyListener to avoid conflicts with JOSM commands
            if (keyListener.getClass().getName().equals("javax.swing.plaf.basic.BasicTreeUI$Handler")) {
                removeKeyListener(keyListener);
            }
        }
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
        final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();

        if (errors == null || errors.isEmpty()) {
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    valTreeModel.setRoot(rootNode);
                }
            });
            return;
        }
        // Sort validation errors - #8517
        Collections.sort(errors);

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

        Map<Severity, MultiMap<String, TestError>> errorTree = new HashMap<Severity, MultiMap<String, TestError>>();
        Map<Severity, HashMap<String, MultiMap<String, TestError>>> errorTreeDeep = new HashMap<Severity, HashMap<String, MultiMap<String, TestError>>>();
        for (Severity s : Severity.values()) {
            errorTree.put(s, new MultiMap<String, TestError>(20));
            errorTreeDeep.put(s, new HashMap<String, MultiMap<String, TestError>>());
        }

        final Boolean other = ValidatorPreference.PREF_OTHER.get();
        for (TestError e : errors) {
            if (e.getIgnored()) {
                continue;
            }
            Severity s = e.getSeverity();
            if(!other && s == Severity.OTHER) {
                continue;
            }
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
                MultiMap<String, TestError> b = errorTreeDeep.get(s).get(m);
                if (b == null) {
                    b = new MultiMap<String, TestError>(20);
                    errorTreeDeep.get(s).put(m, b);
                }
                b.put(d, e);
            } else {
                errorTree.get(s).put(m, e);
            }
        }

        List<TreePath> expandedPaths = new ArrayList<TreePath>();
        for (Severity s : Severity.values()) {
            MultiMap<String, TestError> severityErrors = errorTree.get(s);
            Map<String, MultiMap<String, TestError>> severityErrorsDeep = errorTreeDeep.get(s);
            if (severityErrors.isEmpty() && severityErrorsDeep.isEmpty()) {
                continue;
            }

            // Severity node
            DefaultMutableTreeNode severityNode = new DefaultMutableTreeNode(s);
            rootNode.add(severityNode);

            if (oldSelectedRows.contains(s)) {
                expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode }));
            }

            for (Entry<String, Set<TestError>> msgErrors : severityErrors.entrySet()) {
                // Message node
                Set<TestError> errs = msgErrors.getValue();
                String msg = tr("{0} ({1})", msgErrors.getKey(), errs.size());
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
            for (Entry<String, MultiMap<String, TestError>> bag : severityErrorsDeep.entrySet()) {
                // Group node
                MultiMap<String, TestError> errorlist = bag.getValue();
                DefaultMutableTreeNode groupNode = null;
                if (errorlist.size() > 1) {
                    String nmsg = tr("{0} ({1})", bag.getKey(), errorlist.size());
                    groupNode = new DefaultMutableTreeNode(nmsg);
                    severityNode.add(groupNode);
                    if (oldSelectedRows.contains(bag.getKey())) {
                        expandedPaths.add(new TreePath(new Object[] { rootNode, severityNode, groupNode }));
                    }
                }

                for (Entry<String, Set<TestError>> msgErrors : errorlist.entrySet()) {
                    // Message node
                    Set<TestError> errs = msgErrors.getValue();
                    String msg;
                    if (groupNode != null) {
                        msg = tr("{0} ({1})", msgErrors.getKey(), errs.size());
                    } else {
                        msg = tr("{0} - {1} ({2})", msgErrors.getKey(), bag.getKey(), errs.size());
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
     * @param newerrors The validation errors
     */
    public void setErrors(List<TestError> newerrors) {
        if (errors == null)
            return;
        clearErrors();
        DataSet ds = Main.main.getCurrentDataSet();
        for (TestError error : newerrors) {
            if (!error.getIgnored()) {
                errors.add(error);
                if (ds != null) {
                    ds.addDataSetListener(error);
                }
            }
        }
        if (isVisible()) {
            buildTree();
        }
    }

    /**
     * Returns the errors of the tree
     * @return the errors of the tree
     */
    public List<TestError> getErrors() {
        return errors != null ? errors : Collections.<TestError> emptyList();
    }

    /**
     * Returns the filter list
     * @return the list of primitives used for filtering
     */
    public Set<OsmPrimitive> getFilter() {
        return filter;
    }

    /**
     * Set the filter list to a set of primitives
     * @param filter the list of primitives used for filtering
     */
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
     */
    public void resetErrors() {
        List<TestError> e = new ArrayList<TestError>(errors);
        setErrors(e);
    }

    /**
     * Expands complete tree
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

    /**
     * Returns a value to check if tree has been rebuild
     * @return the current counter
     */
    public int getUpdateCount() {
        return updateCount;
    }

    private void clearErrors() {
        if (errors != null) {
            DataSet ds = Main.main.getCurrentDataSet();
            if (ds != null) {
                for (TestError e : errors) {
                    ds.removeDataSetListener(e);
                }
            }
            errors.clear();
        }
    }

    @Override
    public void destroy() {
        clearErrors();
    }
}
