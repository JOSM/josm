// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.PaintVisitor;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.io.ValidatorErrorExporter;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * A layer showing error messages.
 *
 * @author frsantos
 *
 * @since  3669 (creation)
 * @since 10386 (new LayerChangeListener interface)
 */
public class ValidatorLayer extends Layer implements LayerChangeListener {
    private final Runnable invalidator = this::invalidate;

    /**
     * Constructs a new Validator layer
     */
    public ValidatorLayer() {
        super(tr("Validation errors"));
        MainApplication.getLayerManager().addLayerChangeListener(this);
        MainApplication.getMap().validatorDialog.tree.addInvalidationListener(invalidator);
    }

    /**
     * Return a static icon.
     */
    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "validator_small");
    }

    /**
     * Draw all primitives in this layer but do not draw modified ones (they
     * are drawn by the edit layer).
     * Draw nodes last to overlap the ways they belong to.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void paint(final Graphics2D g, final MapView mv, Bounds bounds) {
        DefaultMutableTreeNode root = MainApplication.getMap().validatorDialog.tree.getRoot();
        if (root == null || root.getChildCount() == 0)
            return;

        PaintVisitor paintVisitor = new PaintVisitor(g, mv);

        DefaultMutableTreeNode severity = (DefaultMutableTreeNode) root.getLastChild();
        while (severity != null) {
            Enumeration<TreeNode> errorMessages = severity.breadthFirstEnumeration();
            while (errorMessages.hasMoreElements()) {
                Object tn = ((DefaultMutableTreeNode) errorMessages.nextElement()).getUserObject();
                if (tn instanceof TestError) {
                    paintVisitor.visit((TestError) tn);
                }
            }

            // Severities in inverse order
            severity = severity.getPreviousSibling();
        }

        paintVisitor.clearPaintedObjects();
    }

    @Override
    public String getToolTipText() {
        MultiMap<Severity, TestError> errorTree = new MultiMap<>();
        List<TestError> errors = MainApplication.getMap().validatorDialog.tree.getErrors();
        for (TestError e : errors) {
            errorTree.put(e.getSeverity(), e);
        }

        StringBuilder b = new StringBuilder();
        for (Severity s : Severity.values()) {
            if (errorTree.containsKey(s)) {
                b.append(tr(s.toString())).append(": ").append(errorTree.get(s).size()).append("<br>");
            }
        }

        if (b.length() == 0)
            return "<html>" + tr("No validation errors") + "</html>";
        else
            return "<html>" + tr("Validation errors") + ":<br>" + b + "</html>";
    }

    @Override
    public void mergeFrom(Layer from) {
        // Do nothing
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        // Do nothing
    }

    @Override
    public Object getInfoComponent() {
        return getToolTipText();
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(null, this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this),
                new LayerSaveAsAction(this)
                };
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save Validation errors file"), ValidatorErrorExporter.FILE_FILTER);
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // Do nothing
    }

    /**
     * If layer is the OSM Data layer, remove all errors
     */
    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        // Removed layer is still in that list.
        if (e.getRemovedLayer() instanceof OsmDataLayer && e.getSource().getLayersOfType(OsmDataLayer.class).size() <= 1) {
            e.scheduleRemoval(Collections.singleton(this));
        } else if (e.getRemovedLayer() == this) {
            OsmValidator.resetErrorLayer();
        }
    }

    @Override
    public LayerPositionStrategy getDefaultLayerPosition() {
        return LayerPositionStrategy.IN_FRONT;
    }

    @Override
    public synchronized void destroy() {
        MainApplication.getMap().validatorDialog.tree.removeInvalidationListener(invalidator);
        MainApplication.getLayerManager().removeLayerChangeListener(this);
        super.destroy();
    }
}
