// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ListenerList;

/**
 * A panel that displays the error tree. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author frsantos
 */
public class ValidatorTreePanel extends JTree implements Destroyable, DataSetListener {

    private static final class GroupTreeNode extends DefaultMutableTreeNode {

        GroupTreeNode(Object userObject) {
            super(userObject);
        }

        @Override
        public String toString() {
            return tr("{0} ({1})", super.toString(), getLeafCount());
        }
    }

    /**
     * The validation data.
     */
    protected DefaultTreeModel valTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    /** The list of errors shown in the tree, normally identical to field validationErrors in current edit layer*/
    private transient List<TestError> errors;

    /**
     * If {@link #filter} is not <code>null</code> only errors are displayed
     * that refer to one of the primitives in the filter.
     */
    private transient Set<? extends OsmPrimitive> filter;

    private final transient ListenerList<Runnable> invalidationListeners = ListenerList.create();

    /** if true, buildTree() does nothing */
    private boolean resetScheduled;

    /**
     * Constructor
     * @param errors The list of errors
     */
    public ValidatorTreePanel(List<TestError> errors) {
        setErrorList(errors);
        ToolTipManager.sharedInstance().registerComponent(this);
        this.setModel(valTreeModel);
        this.setRootVisible(false);
        this.setShowsRootHandles(true);
        this.expandRow(0);
        this.setVisibleRowCount(8);
        this.setCellRenderer(new ValidatorTreeRenderer());
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        for (KeyListener keyListener : getKeyListeners()) {
            // Fix #3596 - Remove default keyListener to avoid conflicts with JOSM commands
            if ("javax.swing.plaf.basic.BasicTreeUI$Handler".equals(keyListener.getClass().getName())) {
                removeKeyListener(keyListener);
            }
        }
        DatasetEventManager.getInstance().addDatasetListener(this, DatasetEventManager.FireMode.IN_EDT);
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
                res = error.getNameVisitor().getText() + "<br>" + error.getMessage();
                String d = error.getDescription();
                if (d != null)
                    res += "<br>" + d;
                res += "<br>" + tr("Test: {0}", getTesterDetails(error));
            } else {
                Set<String> tests = new HashSet<>();
                visitTestErrors(node, err -> tests.add(getTesterDetails(err)), null);
                String source = (tests.size() == 1) ? tr("Test: {0}", tests.iterator().next()) : tr("Different tests");
                res = node.toString() + "<br>" + source;
            }
        }
        return res == null ? null : "<html>" + res + "</html>";
    }

    private static String getTesterDetails(TestError e) {
        return e.getTester().getName() + "<br>" + e.getTester().getSource();
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
        invalidationListeners.fireEvent(Runnable::run);
    }

    /**
     * Builds the errors tree
     */
    public void buildTree() {
        buildTree(true);
    }

    /**
     * Builds the errors tree
     * @param expandAgain if true, try to expand the same rows as before
     */
    public void buildTree(boolean expandAgain) {
        if (resetScheduled)
            return;
        final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();

        if (errors == null || errors.isEmpty()) {
            GuiHelper.runInEDTAndWait(() -> valTreeModel.setRoot(rootNode));
            return;
        }

        // Remember first selected tree row
        TreePath selPath = getSelectionPath();
        int selRow = selPath == null ? -1 : getRowForPath(selPath);

        // Remember the currently expanded rows
        Set<Object> oldExpandedRows = new HashSet<>();
        if (expandAgain) {
            Enumeration<TreePath> expanded = getExpandedDescendants(new TreePath(getRoot()));
            if (expanded != null) {
                while (expanded.hasMoreElements()) {
                    TreePath path = expanded.nextElement();
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObject = node.getUserObject();
                    if (userObject instanceof Severity) {
                        oldExpandedRows.add(userObject);
                    } else if (userObject instanceof String) {
                        String msg = removeSize((String) userObject);
                        oldExpandedRows.add(msg);
                    }
                }
            }
        }

        Predicate<TestError> filterToUse = e -> !e.isIgnored();
        if (!ValidatorPrefHelper.PREF_OTHER.get()) {
            filterToUse = filterToUse.and(e -> e.getSeverity() != Severity.OTHER);
        }
        if (filter != null) {
            filterToUse = filterToUse.and(e -> e.getPrimitives().stream().anyMatch(filter::contains));
        }
        Map<Severity, Map<String, Map<String, List<TestError>>>> errorsBySeverityMessageDescription
            = OsmValidator.getErrorsBySeverityMessageDescription(errors, filterToUse);

        final List<TreePath> expandedPaths = new ArrayList<>();
        for (Entry<Severity, Map<String, Map<String, List<TestError>>>> entry: errorsBySeverityMessageDescription.entrySet()) {
            Severity severity = entry.getKey();
            Map<String, Map<String, List<TestError>>> errorsByMessageDescription = entry.getValue();

            // Severity node
            final DefaultMutableTreeNode severityNode = new GroupTreeNode(severity);
            rootNode.add(severityNode);

            if (oldExpandedRows.contains(severity)) {
                expandedPaths.add(new TreePath(severityNode.getPath()));
            }

            final Map<String, List<TestError>> errorsWithEmptyMessageByDescription = errorsByMessageDescription.get("");
            if (errorsWithEmptyMessageByDescription != null) {
                errorsWithEmptyMessageByDescription.forEach((description, noDescriptionErrors) -> {
                    final String msg = addSize(description, noDescriptionErrors);
                    final DefaultMutableTreeNode messageNode = new DefaultMutableTreeNode(msg);
                    severityNode.add(messageNode);

                    if (oldExpandedRows.contains(description)) {
                        expandedPaths.add(new TreePath(messageNode.getPath()));
                    }
                    // add the matching errors to the current node
                    noDescriptionErrors.stream().map(DefaultMutableTreeNode::new).forEach(messageNode::add);
                });
            }

            errorsByMessageDescription.forEach((message, errorsByDescription) -> {
                if (message.isEmpty()) {
                    return;
                }
                // Group node
                final DefaultMutableTreeNode groupNode;
                if (errorsByDescription.size() > 1) {
                    groupNode = new GroupTreeNode(message);
                    severityNode.add(groupNode);
                    if (oldExpandedRows.contains(message)) {
                        expandedPaths.add(new TreePath(groupNode.getPath()));
                    }
                } else {
                    groupNode = null;
                }

                errorsByDescription.forEach((description, errorsWithDescription) -> {
                    // Message node
                    final String searchMsg;
                    if (groupNode != null) {
                        searchMsg = description;
                    } else if (description == null || description.isEmpty()) {
                        searchMsg = message;
                    } else {
                        searchMsg = message + " - " + description;
                    }
                    final String msg = addSize(searchMsg, errorsWithDescription);

                    final DefaultMutableTreeNode messageNode = new DefaultMutableTreeNode(msg);
                    DefaultMutableTreeNode currNode = groupNode != null ? groupNode : severityNode;
                    currNode.add(messageNode);
                    if (oldExpandedRows.contains(searchMsg)) {
                        expandedPaths.add(new TreePath(messageNode.getPath()));
                    }

                    // add the matching errors to the current node
                    errorsWithDescription.stream().map(DefaultMutableTreeNode::new).forEach(messageNode::add);
                });
            });
        }

        valTreeModel.setRoot(rootNode);
        for (TreePath path : expandedPaths) {
            this.expandPath(path);
        }

        if (selPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (userObject instanceof TestError && ((TestError) userObject).isIgnored()) {
                // don't try to find ignored error
                selPath = null;
            }
        }
        if (selPath != null) {
            // try to reselect previously selected row. May not work if tree structure changed too much.
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            Object searchObject = node.getUserObject();
            String msg = null;
            if (searchObject instanceof String) {
                msg = removeSize((String) searchObject);
            }
            String searchString = msg;
            visitTreeNodes(getRoot(), n -> {
                boolean found = false;
                final Object userInfo = n.getUserObject();
                if (searchObject instanceof TestError && userInfo instanceof TestError) {
                    TestError e1 = (TestError) searchObject;
                    TestError e2 = (TestError) userInfo;
                    found |= e1.getCode() == e2.getCode() && e1.getMessage().equals(e2.getMessage())
                            && e1.getPrimitives().size() == e2.getPrimitives().size()
                            && e1.getPrimitives().containsAll(e2.getPrimitives());
                } else if (searchObject instanceof String && userInfo instanceof String) {
                    found |= ((String) userInfo).startsWith(searchString);
                } else if (searchObject instanceof Severity) {
                    found |= searchObject.equals(userInfo);
                }

                if (found) {
                    TreePath path = new TreePath(n.getPath());
                    setSelectionPath(path);
                    scrollPathToVisible(path);
                }
            });
        }
        if (selRow >= 0 && selRow < getRowCount() && getSelectionCount() == 0) {
            // fall back: if we cannot find the previously selected entry, select the row by position
            setSelectionRow(selRow);
            scrollRowToVisible(selRow);
        }

        invalidationListeners.fireEvent(Runnable::run);
    }

    private static String addSize(String msg, Collection<?> coll) {
        return msg + " (" + coll.size() + ")";
    }

    private static String removeSize(String msg) {
        int index = msg.lastIndexOf(" (");
        return index > 0 ? msg.substring(0, index) : msg;
    }

    /**
     * Add a new invalidation listener
     * @param listener The listener
     */
    public void addInvalidationListener(Runnable listener) {
        invalidationListeners.addListener(listener);
    }

    /**
     * Remove an invalidation listener
     * @param listener The listener
     * @since 10880
     */
    public void removeInvalidationListener(Runnable listener) {
        invalidationListeners.removeListener(listener);
    }

    /**
     * Sets the errors list used by a data layer
     * @param errors The error list that is used by a data layer
     */
    public final void setErrorList(List<TestError> errors) {
        if (errors != null && errors == this.errors)
            return;
        this.errors = errors != null ? errors : new ArrayList<>();
        if (isVisible()) {
            //TODO: If list is changed because another layer was activated it would be good to store/restore
            // the expanded / selected paths.
            clearSelection();
            buildTree(false);
        }
    }

    /**
     * Clears the current error list and adds these errors to it
     * @param newerrors The validation errors
     */
    public void setErrors(List<TestError> newerrors) {
        errors.clear();
        for (TestError error : newerrors) {
            if (!error.isIgnored()) {
                errors.add(error);
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
        return errors;
    }

    /**
     * Selects all errors related to the specified {@code primitives}, i.e. where {@link TestError#getPrimitives()}
     * returns a primitive present in {@code primitives}.
     * @param primitives collection of primitives
     */
    public void selectRelatedErrors(final Collection<OsmPrimitive> primitives) {
        final List<TreePath> paths = new ArrayList<>();
        walkAndSelectRelatedErrors(new TreePath(getRoot()), new HashSet<>(primitives)::contains, paths);
        clearSelection();
        setSelectionPaths(paths.toArray(new TreePath[0]));
        // make sure that first path is visible
        if (!paths.isEmpty()) {
            scrollPathToVisible(paths.get(0));
        }
    }

    private void walkAndSelectRelatedErrors(final TreePath p, final Predicate<OsmPrimitive> isRelevant, final Collection<TreePath> paths) {
        final int count = getModel().getChildCount(p.getLastPathComponent());
        for (int i = 0; i < count; i++) {
            final Object child = getModel().getChild(p.getLastPathComponent(), i);
            if (getModel().isLeaf(child) && child instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) child).getUserObject() instanceof TestError) {
                final TestError error = (TestError) ((DefaultMutableTreeNode) child).getUserObject();
                if (error.getPrimitives().stream().anyMatch(isRelevant)) {
                    paths.add(p.pathByAddingChild(child));
                }
            } else {
                walkAndSelectRelatedErrors(p.pathByAddingChild(child), isRelevant, paths);
            }
        }
    }

    /**
     * Returns the filter list
     * @return the list of primitives used for filtering
     */
    public Set<? extends OsmPrimitive> getFilter() {
        return filter;
    }

    /**
     * Set the filter list to a set of primitives
     * @param filter the list of primitives used for filtering
     */
    public void setFilter(Set<? extends OsmPrimitive> filter) {
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
        resetScheduled = false;
        filterRemovedPrimitives();
        setErrors(new ArrayList<>(errors));
    }

    /**
     * Expands complete tree
     */
    public void expandAll() {
        visitTreeNodes(getRoot(), x -> expandPath(new TreePath(x.getPath())));
    }

    /**
     * Returns the root node model.
     * @return The root node model
     */
    public DefaultMutableTreeNode getRoot() {
        return (DefaultMutableTreeNode) valTreeModel.getRoot();
    }

    @Override
    public void destroy() {
        DatasetEventManager.getInstance().removeDatasetListener(this);
        ToolTipManager.sharedInstance().unregisterComponent(this);
        errors.clear();
    }

    /**
     * Visitor call for all tree nodes children of root, in breadth-first order.
     * @param root Root node
     * @param visitor Visitor
     * @since 13940
     */
    public static void visitTreeNodes(DefaultMutableTreeNode root, Consumer<DefaultMutableTreeNode> visitor) {
        @SuppressWarnings("unchecked")
        Enumeration<TreeNode> errorMessages = root.breadthFirstEnumeration();
        while (errorMessages.hasMoreElements()) {
            visitor.accept(((DefaultMutableTreeNode) errorMessages.nextElement()));
        }
    }

    /**
     * Visitor call for all {@link TestError} nodes children of root, in breadth-first order.
     * @param root Root node
     * @param visitor Visitor
     * @since 13940
     */
    public static void visitTestErrors(DefaultMutableTreeNode root, Consumer<TestError> visitor) {
        visitTestErrors(root, visitor, null);
    }

    /**
     * Visitor call for all {@link TestError} nodes children of root, in breadth-first order.
     * @param root Root node
     * @param visitor Visitor
     * @param processedNodes Set of already visited nodes (optional)
     * @since 13940
     */
    public static void visitTestErrors(DefaultMutableTreeNode root, Consumer<TestError> visitor,
            Set<DefaultMutableTreeNode> processedNodes) {
        visitTreeNodes(root, n -> {
            if (processedNodes == null || !processedNodes.contains(n)) {
                if (processedNodes != null) {
                    processedNodes.add(n);
                }
                Object o = n.getUserObject();
                if (o instanceof TestError) {
                    visitor.accept((TestError) o);
                }
            }
        });
    }

    @Override public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // Remove purged primitives (fix #8639)
        if (filterRemovedPrimitives()) {
            buildTree();
        }
    }

    @Override public void primitivesAdded(PrimitivesAddedEvent event) {
        // Do nothing
    }

    @Override public void tagsChanged(TagsChangedEvent event) {
        // Do nothing
    }

    @Override public void nodeMoved(NodeMovedEvent event) {
        // Do nothing
    }

    @Override public void wayNodesChanged(WayNodesChangedEvent event) {
        // Do nothing
    }

    @Override public void relationMembersChanged(RelationMembersChangedEvent event) {
        // Do nothing
    }

    @Override public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Do nothing
    }

    @Override public void dataChanged(DataChangedEvent event) {
        if (filterRemovedPrimitives()) {
            buildTree();
        }
    }

    /**
     * Can be called to suppress execution of buildTree() while doing multiple updates. Caller must
     * call resetErrors() to end this state.
     * @since 14849
     */
    public void setResetScheduled() {
        resetScheduled = true;
    }

    /**
     * Remove errors which refer to removed or purged primitives.
     * @return true if error list was changed
     */
    private boolean filterRemovedPrimitives() {
        return errors.removeIf(
                error -> error.getPrimitives().stream().anyMatch(p -> p.isDeleted() || p.getDataSet() == null));
    }

}
