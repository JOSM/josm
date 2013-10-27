// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.PaintVisitor;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * A layer showing error messages.
 *
 * @author frsantos
 */
public class ValidatorLayer extends Layer implements LayerChangeListener {

    private int updateCount = -1;

    /**
     * Constructs a new Validator layer
     */
    public ValidatorLayer() {
        super(tr("Validation errors"));
        MapView.addLayerChangeListener(this);
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
        updateCount = Main.map.validatorDialog.tree.getUpdateCount();
        DefaultMutableTreeNode root = Main.map.validatorDialog.tree.getRoot();
        if (root == null || root.getChildCount() == 0)
            return;

        PaintVisitor paintVisitor = new PaintVisitor(g, mv);

        DefaultMutableTreeNode severity = (DefaultMutableTreeNode) root.getLastChild();
        while (severity != null) {
            Enumeration<DefaultMutableTreeNode> errorMessages = severity.breadthFirstEnumeration();
            while (errorMessages.hasMoreElements()) {
                Object tn = errorMessages.nextElement().getUserObject();
                if (tn instanceof TestError) {
                    paintVisitor.visit(((TestError) tn));
                }
            }

            // Severities in inverse order
            severity = severity.getPreviousSibling();
        }

        paintVisitor.clearPaintedObjects();
    }

    @Override
    public String getToolTipText() {
        MultiMap<Severity, TestError> errorTree = new MultiMap<Severity, TestError>();
        List<TestError> errors = Main.map.validatorDialog.tree.getErrors();
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
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public boolean isChanged() {
        return updateCount != Main.map.validatorDialog.tree.getUpdateCount();
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
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
                new LayerListPopup.InfoAction(this) };
    }

    @Override
    public void destroy() {
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
    }

    @Override
    public void layerAdded(Layer newLayer) {
    }

    /**
     * If layer is the OSM Data layer, remove all errors
     */
    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof OsmDataLayer && Main.isDisplayingMapView() && !Main.main.hasEditLayer()) {
            Main.main.removeLayer(this);
        } else if (oldLayer == this) {
            MapView.removeLayerChangeListener(this);
            OsmValidator.errorLayer = null;
        }
    }
}
