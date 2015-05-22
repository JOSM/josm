// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractSelectAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.ValidatorVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.validator.ValidatorTreePanel;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * A small tool dialog for displaying the current errors. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author frsantos
 */
public class ValidatorDialog extends ToggleDialog implements SelectionChangedListener, LayerChangeListener {

    /** The display tree */
    public ValidatorTreePanel tree;

    /** The fix button */
    private SideButton fixButton;
    /** The ignore button */
    private SideButton ignoreButton;
    /** The select button */
    private SideButton selectButton;

    private final JPopupMenu popupMenu = new JPopupMenu();
    private final transient PopupMenuHandler popupMenuHandler = new PopupMenuHandler(popupMenu);

    /** Last selected element */
    private DefaultMutableTreeNode lastSelectedNode = null;

    private transient OsmDataLayer linkedLayer;

    /**
     * Constructor
     */
    public ValidatorDialog() {
        super(tr("Validation Results"), "validator", tr("Open the validation window."),
                Shortcut.registerShortcut("subwindow:validator", tr("Toggle: {0}", tr("Validation results")),
                        KeyEvent.VK_V, Shortcut.ALT_SHIFT), 150, false, ValidatorPreference.class);

        popupMenuHandler.addAction(Main.main.menu.autoScaleActions.get("problem"));
        popupMenuHandler.addAction(new EditRelationAction());

        tree = new ValidatorTreePanel();
        tree.addMouseListener(new MouseEventHandler());
        addTreeSelectionListener(new SelectionWatch());
        InputMapUtils.unassignCtrlShiftUpDown(tree, JComponent.WHEN_FOCUSED);

        List<SideButton> buttons = new LinkedList<>();

        selectButton = new SideButton(new AbstractSelectAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedItems();
            }
        });
        InputMapUtils.addEnterAction(tree, selectButton.getAction());

        selectButton.setEnabled(false);
        buttons.add(selectButton);

        buttons.add(new SideButton(Main.main.validator.validateAction));

        fixButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Fix"));
                putValue(SHORT_DESCRIPTION,  tr("Fix the selected issue."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs","fix"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                fixErrors();
            }
        });
        fixButton.setEnabled(false);
        buttons.add(fixButton);

        if (Main.pref.getBoolean(ValidatorPreference.PREF_USE_IGNORE, true)) {
            ignoreButton = new SideButton(new AbstractAction() {
                {
                    putValue(NAME, tr("Ignore"));
                    putValue(SHORT_DESCRIPTION,  tr("Ignore the selected issue next time."));
                    putValue(SMALL_ICON, ImageProvider.get("dialogs","fix"));
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    ignoreErrors();
                }
            });
            ignoreButton.setEnabled(false);
            buttons.add(ignoreButton);
        } else {
            ignoreButton = null;
        }
        createLayout(tree, true, buttons);
    }

    @Override
    public void showNotify() {
        DataSet.addSelectionListener(this);
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            updateSelection(ds.getAllSelected());
        }
        MapView.addLayerChangeListener(this);
        Layer activeLayer = Main.map.mapView.getActiveLayer();
        if (activeLayer != null) {
            activeLayerChange(null, activeLayer);
        }
    }

    @Override
    public void hideNotify() {
        MapView.removeLayerChangeListener(this);
        DataSet.removeSelectionListener(this);
    }

    @Override
    public void setVisible(boolean v) {
        if (tree != null) {
            tree.setVisible(v);
        }
        super.setVisible(v);
        Main.map.repaint();
    }

    /**
     * Fix selected errors
     */
    @SuppressWarnings("unchecked")
    private void fixErrors() {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null)
            return;

        Set<DefaultMutableTreeNode> processedNodes = new HashSet<>();

        List<TestError> errorsToFix = new LinkedList<>();
        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null) {
                continue;
            }

            Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                if (processedNodes.contains(childNode)) {
                    continue;
                }

                processedNodes.add(childNode);
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof TestError) {
                    errorsToFix.add((TestError)nodeInfo);
                }
            }
        }

        // run fix task asynchronously
        //
        FixTask fixTask = new FixTask(errorsToFix);
        Main.worker.submit(fixTask);
    }

    /**
     * Set selected errors to ignore state
     */
    @SuppressWarnings("unchecked")
    private void ignoreErrors() {
        int asked = JOptionPane.DEFAULT_OPTION;
        boolean changed = false;
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null)
            return;

        Set<DefaultMutableTreeNode> processedNodes = new HashSet<>();
        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null) {
                continue;
            }

            Object mainNodeInfo = node.getUserObject();
            if (!(mainNodeInfo instanceof TestError)) {
                Set<String> state = new HashSet<>();
                // ask if the whole set should be ignored
                if (asked == JOptionPane.DEFAULT_OPTION) {
                    String[] a = new String[] { tr("Whole group"), tr("Single elements"), tr("Nothing") };
                    asked = JOptionPane.showOptionDialog(Main.parent, tr("Ignore whole group or individual elements?"),
                            tr("Ignoring elements"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                            a, a[1]);
                }
                if (asked == JOptionPane.YES_NO_OPTION) {
                    Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
                    while (children.hasMoreElements()) {
                        DefaultMutableTreeNode childNode = children.nextElement();
                        if (processedNodes.contains(childNode)) {
                            continue;
                        }

                        processedNodes.add(childNode);
                        Object nodeInfo = childNode.getUserObject();
                        if (nodeInfo instanceof TestError) {
                            TestError err = (TestError) nodeInfo;
                            err.setIgnored(true);
                            changed = true;
                            state.add(node.getDepth() == 1 ? err.getIgnoreSubGroup() : err.getIgnoreGroup());
                        }
                    }
                    for (String s : state) {
                        OsmValidator.addIgnoredError(s);
                    }
                    continue;
                } else if (asked == JOptionPane.CANCEL_OPTION || asked == JOptionPane.CLOSED_OPTION) {
                    continue;
                }
            }

            Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                if (processedNodes.contains(childNode)) {
                    continue;
                }

                processedNodes.add(childNode);
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof TestError) {
                    TestError error = (TestError) nodeInfo;
                    String state = error.getIgnoreState();
                    if (state != null) {
                        OsmValidator.addIgnoredError(state);
                    }
                    changed = true;
                    error.setIgnored(true);
                }
            }
        }
        if (changed) {
            tree.resetErrors();
            OsmValidator.saveIgnoredErrors();
            Main.map.repaint();
        }
    }

    /**
     * Sets the selection of the map to the current selected items.
     */
    @SuppressWarnings("unchecked")
    private void setSelectedItems() {
        if (tree == null)
            return;

        Collection<OsmPrimitive> sel = new HashSet<>(40);

        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null)
            return;

        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof TestError) {
                    TestError error = (TestError) nodeInfo;
                    sel.addAll(error.getSelectablePrimitives());
                }
            }
        }
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            ds.setSelected(sel);
        }
    }

    /**
     * Checks for fixes in selected element and, if needed, adds to the sel
     * parameter all selected elements
     *
     * @param sel
     *            The collection where to add all selected elements
     * @param addSelected
     *            if true, add all selected elements to collection
     * @return whether the selected elements has any fix
     */
    @SuppressWarnings("unchecked")
    private boolean setSelection(Collection<OsmPrimitive> sel, boolean addSelected) {
        boolean hasFixes = false;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (lastSelectedNode != null && !lastSelectedNode.equals(node)) {
            Enumeration<DefaultMutableTreeNode> children = lastSelectedNode.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof TestError) {
                    TestError error = (TestError) nodeInfo;
                    error.setSelected(false);
                }
            }
        }

        lastSelectedNode = node;
        if (node == null)
            return hasFixes;

        Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = children.nextElement();
            Object nodeInfo = childNode.getUserObject();
            if (nodeInfo instanceof TestError) {
                TestError error = (TestError) nodeInfo;
                error.setSelected(true);

                hasFixes = hasFixes || error.isFixable();
                if (addSelected) {
                    sel.addAll(error.getSelectablePrimitives());
                }
            }
        }
        selectButton.setEnabled(true);
        if (ignoreButton != null) {
            ignoreButton.setEnabled(true);
        }

        return hasFixes;
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (newLayer instanceof OsmDataLayer) {
            linkedLayer = (OsmDataLayer)newLayer;
            tree.setErrorList(linkedLayer.validationErrors);
        }
    }

    @Override
    public void layerAdded(Layer newLayer) {
        // Do nothing
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer == linkedLayer) {
            tree.setErrorList(new ArrayList<TestError>());
        }
    }

    /**
     * Add a tree selection listener to the validator tree.
     * @param listener the TreeSelectionListener
     * @since 5958
     */
    public void addTreeSelectionListener(TreeSelectionListener listener) {
        tree.addTreeSelectionListener(listener);
    }

    /**
     * Remove the given tree selection listener from the validator tree.
     * @param listener the TreeSelectionListener
     * @since 5958
     */
    public void removeTreeSelectionListener(TreeSelectionListener listener) {
        tree.removeTreeSelectionListener(listener);
    }

    /**
     * Replies the popup menu handler.
     * @return The popup menu handler
     * @since 5958
     */
    public PopupMenuHandler getPopupMenuHandler() {
        return popupMenuHandler;
    }

    /**
     * Replies the currently selected error, or {@code null}.
     * @return The selected error, if any.
     * @since 5958
     */
    public TestError getSelectedError() {
        Object comp = tree.getLastSelectedPathComponent();
        if (comp instanceof DefaultMutableTreeNode) {
            Object object = ((DefaultMutableTreeNode)comp).getUserObject();
            if (object instanceof TestError) {
                return (TestError) object;
            }
        }
        return null;
    }

    /**
     * Watches for double clicks and launches the popup menu.
     */
    class MouseEventHandler extends PopupMenuLauncher {

        public MouseEventHandler() {
            super(popupMenu);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            fixButton.setEnabled(false);
            if (ignoreButton != null) {
                ignoreButton.setEnabled(false);
            }
            selectButton.setEnabled(false);

            boolean isDblClick = isDoubleClick(e);

            Collection<OsmPrimitive> sel = isDblClick ? new HashSet<OsmPrimitive>(40) : null;

            boolean hasFixes = setSelection(sel, isDblClick);
            fixButton.setEnabled(hasFixes);

            if (isDblClick) {
                Main.main.getCurrentDataSet().setSelected(sel);
                if (Main.pref.getBoolean("validator.autozoom", false)) {
                    AutoScaleAction.zoomTo(sel);
                }
            }
        }

        @Override public void launch(MouseEvent e) {
            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            if (selPath == null)
                return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getPathComponent(selPath.getPathCount() - 1);
            if (!(node.getUserObject() instanceof TestError))
                return;
            super.launch(e);
        }

    }

    /**
     * Watches for tree selection.
     */
    public class SelectionWatch implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            fixButton.setEnabled(false);
            if (ignoreButton != null) {
                ignoreButton.setEnabled(false);
            }
            selectButton.setEnabled(false);

            Collection<OsmPrimitive> sel = new HashSet<>();
            boolean hasFixes = setSelection(sel, true);
            fixButton.setEnabled(hasFixes);
            popupMenuHandler.setPrimitives(sel);
            if (Main.map != null) {
                Main.map.repaint();
            }
        }
    }

    public static class ValidatorBoundingXYVisitor extends BoundingXYVisitor implements ValidatorVisitor {
        @Override
        public void visit(OsmPrimitive p) {
            if (p.isUsable()) {
                p.accept(this);
            }
        }

        @Override
        public void visit(WaySegment ws) {
            if (ws.lowerIndex < 0 || ws.lowerIndex + 1 >= ws.way.getNodesCount())
                return;
            visit(ws.way.getNodes().get(ws.lowerIndex));
            visit(ws.way.getNodes().get(ws.lowerIndex + 1));
        }

        @Override
        public void visit(List<Node> nodes) {
            for (Node n: nodes) {
                visit(n);
            }
        }

        @Override
        public void visit(TestError error) {
            if (error != null) {
                error.visitHighlighted(this);
            }
        }
    }

    public void updateSelection(Collection<? extends OsmPrimitive> newSelection) {
        if (!Main.pref.getBoolean(ValidatorPreference.PREF_FILTER_BY_SELECTION, false))
            return;
        if (newSelection.isEmpty()) {
            tree.setFilter(null);
        }
        tree.setFilter(new HashSet<>(newSelection));
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        updateSelection(newSelection);
    }

    /**
     * Task for fixing a collection of {@link TestError}s. Can be run asynchronously.
     *
     *
     */
    class FixTask extends PleaseWaitRunnable {
        private Collection<TestError> testErrors;
        private boolean canceled;

        public FixTask(Collection<TestError> testErrors) {
            super(tr("Fixing errors ..."), false /* don't ignore exceptions */);
            this.testErrors = testErrors == null ? new ArrayList<TestError>(): testErrors;
        }

        @Override
        protected void cancel() {
            this.canceled = true;
        }

        @Override
        protected void finish() {
            // do nothing
        }

        protected void fixError(TestError error) throws InterruptedException, InvocationTargetException {
            if (error.isFixable()) {
                final Command fixCommand = error.getFix();
                if (fixCommand != null) {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            Main.main.undoRedo.addNoRedraw(fixCommand);
                        }
                    });
                }
                // It is wanted to ignore an error if it said fixable, even if fixCommand was null
                // This is to fix #5764 and #5773: a delete command, for example, may be null if all concerned primitives have already been deleted
                error.setIgnored(true);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException,
        OsmTransferException {
            ProgressMonitor monitor = getProgressMonitor();
            try {
                monitor.setTicksCount(testErrors.size());
                int i=0;
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        Main.main.getCurrentDataSet().beginUpdate();
                    }
                });
                try {
                    for (TestError error: testErrors) {
                        i++;
                        monitor.subTask(tr("Fixing ({0}/{1}): ''{2}''", i, testErrors.size(),error.getMessage()));
                        if (this.canceled)
                            return;
                        fixError(error);
                        monitor.worked(1);
                    }
                } finally {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            Main.main.getCurrentDataSet().endUpdate();
                        }
                    });
                }
                monitor.subTask(tr("Updating map ..."));
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        Main.main.undoRedo.afterAdd();
                        Main.map.repaint();
                        tree.resetErrors();
                        Main.main.getCurrentDataSet().fireSelectionChanged();
                    }
                });
            } catch(InterruptedException | InvocationTargetException e) {
                // FIXME: signature of realRun should have a generic checked exception we
                // could throw here
                throw new RuntimeException(e);
            } finally {
                monitor.finishTask();
            }
        }
    }
}
