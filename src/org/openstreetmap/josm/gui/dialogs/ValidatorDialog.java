// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
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

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
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
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.validator.ValidatorTreePanel;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.ValidatorPreference;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * A small tool dialog for displaying the current errors. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author frsantos
 */
public class ValidatorDialog extends ToggleDialog implements ActionListener, SelectionChangedListener, LayerChangeListener {
    /** Serializable ID */
    private static final long serialVersionUID = 2952292777351992696L;

    /** The display tree */
    public ValidatorTreePanel tree;

    /** The fix button */
    private SideButton fixButton;
    /** The ignore button */
    private SideButton ignoreButton;
    /** The select button */
    private SideButton selectButton;

    private JPopupMenu popupMenu;
    private TestError popupMenuError = null;

    /** Last selected element */
    private DefaultMutableTreeNode lastSelectedNode = null;

    /**
     * Constructor
     */
    public ValidatorDialog() {
        super(tr("Validation results"), "validator", tr("Open the validation window."),
                Shortcut.registerShortcut("subwindow:validator", tr("Toggle: {0}", tr("Validation results")),
                        KeyEvent.VK_V, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);

        popupMenu = new JPopupMenu();

        JMenuItem zoomTo = new JMenuItem(tr("Zoom to problem"));
        zoomTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToProblem();
            }
        });
        popupMenu.add(zoomTo);

        tree = new ValidatorTreePanel();
        tree.addMouseListener(new ClickWatch());
        tree.addTreeSelectionListener(new SelectionWatch());

        add(new JScrollPane(tree), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

        selectButton = new SideButton(marktr("Select"), "select", "Validator",
                tr("Set the selected elements on the map to the selected items in the list above."), this);
        selectButton.setEnabled(false);
        buttonPanel.add(selectButton);
        buttonPanel.add(new SideButton(Main.main.validator.validateAction), "refresh");
        fixButton = new SideButton(marktr("Fix"), "fix", "Validator", tr("Fix the selected issue."), this);
        fixButton.setEnabled(false);
        buttonPanel.add(fixButton);
        if (Main.pref.getBoolean(ValidatorPreference.PREF_USE_IGNORE, true)) {
            ignoreButton = new SideButton(marktr("Ignore"), "delete", "Validator",
                    tr("Ignore the selected issue next time."), this);
            ignoreButton.setEnabled(false);
            buttonPanel.add(ignoreButton);
        } else {
            ignoreButton = null;
        }
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public void showNotify() {
        DataSet.addSelectionListener(this);
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            updateSelection(ds.getSelected());
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
     *
     * @param e
     */
    @SuppressWarnings("unchecked")
    private void fixErrors(ActionEvent e) {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null)
            return;

        Set<DefaultMutableTreeNode> processedNodes = new HashSet<DefaultMutableTreeNode>();

        LinkedList<TestError> errorsToFix = new LinkedList<TestError>();
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
     *
     * @param e
     */
    @SuppressWarnings("unchecked")
    private void ignoreErrors(ActionEvent e) {
        int asked = JOptionPane.DEFAULT_OPTION;
        boolean changed = false;
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null)
            return;

        Set<DefaultMutableTreeNode> processedNodes = new HashSet<DefaultMutableTreeNode>();
        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null) {
                continue;
            }

            Object mainNodeInfo = node.getUserObject();
            if (!(mainNodeInfo instanceof TestError)) {
                Set<String> state = new HashSet<String>();
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
                } else if (asked == JOptionPane.CANCEL_OPTION) {
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

    private void showPopupMenu(MouseEvent e) {
        if (!e.isPopupTrigger())
            return;
        popupMenuError = null;
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getPathComponent(selPath.getPathCount() - 1);
        if (!(node.getUserObject() instanceof TestError))
            return;
        popupMenuError = (TestError) node.getUserObject();
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void zoomToProblem() {
        if (popupMenuError == null)
            return;
        ValidatorBoundingXYVisitor bbox = new ValidatorBoundingXYVisitor();
        popupMenuError.visitHighlighted(bbox);
        if (bbox.getBounds() == null)
            return;
        bbox.enlargeBoundingBox();
        Main.map.mapView.recalculateCenterScale(bbox);
    }

    /**
     * Sets the selection of the map to the current selected items.
     */
    @SuppressWarnings("unchecked")
    private void setSelectedItems() {
        if (tree == null)
            return;

        Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>(40);

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
                    sel.addAll(error.getPrimitives());
                }
            }
        }
        Main.main.getCurrentDataSet().setSelected(sel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("Select")) {
            setSelectedItems();
        } else if (actionCommand.equals("Fix")) {
            fixErrors(e);
        } else if (actionCommand.equals("Ignore")) {
            ignoreErrors(e);
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
                    sel.addAll(error.getPrimitives());
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
            tree.setErrorList(((OsmDataLayer) newLayer).validationErrors);
        }
    }

    @Override
    public void layerAdded(Layer newLayer) {}

    @Override
    public void layerRemoved(Layer oldLayer) {}

    /**
     * Watches for clicks.
     */
    public class ClickWatch extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            fixButton.setEnabled(false);
            if (ignoreButton != null) {
                ignoreButton.setEnabled(false);
            }
            selectButton.setEnabled(false);

            boolean isDblClick = e.getClickCount() > 1;

            Collection<OsmPrimitive> sel = isDblClick ? new HashSet<OsmPrimitive>(40) : null;

            boolean hasFixes = setSelection(sel, isDblClick);
            fixButton.setEnabled(hasFixes);

            if (isDblClick) {
                Main.main.getCurrentDataSet().setSelected(sel);
                if(Main.pref.getBoolean("validator.autozoom", false)) {
                    AutoScaleAction.zoomTo(sel);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            showPopupMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopupMenu(e);
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

            if (e.getSource() instanceof JScrollPane) {
                System.out.println(e.getSource());
                return;
            }

            boolean hasFixes = setSelection(null, false);
            fixButton.setEnabled(hasFixes);
            Main.map.repaint();
        }
    }

    public static class ValidatorBoundingXYVisitor extends BoundingXYVisitor implements ValidatorVisitor {
        @Override
        public void visit(OsmPrimitive p) {
            if (p.isUsable()) {
                p.visit(this);
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
    }

    public void updateSelection(Collection<? extends OsmPrimitive> newSelection) {
        if (!Main.pref.getBoolean(ValidatorPreference.PREF_FILTER_BY_SELECTION, false))
            return;
        if (newSelection.isEmpty()) {
            tree.setFilter(null);
        }
        HashSet<OsmPrimitive> filter = new HashSet<OsmPrimitive>(newSelection);
        tree.setFilter(filter);
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        updateSelection(newSelection);
    }

    /**
     * Task for fixing a collection of {@see TestError}s. Can be run asynchronously.
     *
     *
     */
    class FixTask extends PleaseWaitRunnable {
        private Collection<TestError> testErrors;
        private boolean canceled;

        public FixTask(Collection<TestError> testErrors) {
            super(tr("Fixing errors ..."), false /* don't ignore exceptions */);
            this.testErrors = testErrors == null ? new ArrayList<TestError> (): testErrors;
        }

        @Override
        protected void cancel() {
            this.canceled = true;
        }

        @Override
        protected void finish() {
            // do nothing
        }

        @Override
        protected void realRun() throws SAXException, IOException,
        OsmTransferException {
            ProgressMonitor monitor = getProgressMonitor();
            try {
                monitor.setTicksCount(testErrors.size());
                int i=0;
                for (TestError error: testErrors) {
                    i++;
                    monitor.subTask(tr("Fixing ({0}/{1}): ''{2}''", i, testErrors.size(),error.getMessage()));
                    if (this.canceled)
                        return;
                    final Command fixCommand = error.getFix();
                    if (fixCommand != null) {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                Main.main.undoRedo.addNoRedraw(fixCommand);
                            }
                        });
                        error.setIgnored(true);
                    }
                    monitor.worked(1);
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
            } catch(InterruptedException e) {
                // FIXME: signature of realRun should have a generic checked exception we
                // could throw here
                throw new RuntimeException(e);
            } catch(InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                monitor.finishTask();
            }
        }
    }
}
