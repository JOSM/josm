// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Table displaying OSM primitives.
 * @since 5297
 */
public abstract class OsmPrimitivesTable extends JosmTable {

    /**
     * the data layer in whose context primitives are edited in this table
     */
    private transient OsmDataLayer layer;

    /** the popup menu */
    private JPopupMenu popupMenu;
    private ZoomToAction zoomToAction;

    /**
     * Constructs a new {@code OsmPrimitivesTable}.
     * @param dm table model
     * @param cm column model
     * @param sm selection model
     */
    protected OsmPrimitivesTable(OsmPrimitivesTableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        addMouseListener(new PopupMenuLauncher(getPopUpMenu()));
        addMouseListener(new DblClickHandler());
    }

    /**
     * Returns the table model.
     * @return the table model
     */
    public OsmPrimitivesTableModel getOsmPrimitivesTableModel() {
        return (OsmPrimitivesTableModel) getModel();
    }

    /**
     * Returns the data layer.
     * @return the data layer
     */
    public final OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Sets the data layer.
     * @param layer the data layer
     */
    public final void setLayer(OsmDataLayer layer) {
        this.layer = layer;
    }

    /**
     * Replies the popup menu for this table
     *
     * @return the popup menu
     */
    protected final JPopupMenu getPopUpMenu() {
        if (popupMenu == null) {
            popupMenu = buildPopupMenu();
        }
        return popupMenu;
    }

    protected abstract ZoomToAction buildZoomToAction();

    protected JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        zoomToAction = buildZoomToAction();
        getSelectionModel().addListSelectionListener(zoomToAction);
        menu.add(zoomToAction);
        return menu;
    }

    /**
     * Adds all registered listeners by this table
     * @see #unregisterListeners()
     * @since 10454
     */
    public void registerListeners() {
        MainApplication.getLayerManager().addLayerChangeListener(zoomToAction);
        MainApplication.getLayerManager().addActiveLayerChangeListener(zoomToAction);
    }

    /**
     * Removes all registered listeners by this table
     * @since 10454
     */
    public void unregisterListeners() {
        MainApplication.getLayerManager().removeLayerChangeListener(zoomToAction);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(zoomToAction);
    }

    /**
     * Returns primitive at the specified row.
     * @param row table row
     * @param layer unused in this implementation, can be useful for subclasses
     * @return primitive at the specified row
     */
    public OsmPrimitive getPrimitiveInLayer(int row, OsmDataLayer layer) {
        return getOsmPrimitivesTableModel().getReferredPrimitive(row);
    }

    protected class DblClickHandler extends MouseAdapter {

        protected void setSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getPrimitiveInLayer(row, layer);
            if (layer != null && primitive != null) {
                layer.data.setSelected(primitive.getPrimitiveId());
            }
        }

        protected void addSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getPrimitiveInLayer(row, layer);
            getSelectionModel().addSelectionInterval(row, row);
            if (layer != null && primitive != null) {
                layer.data.addSelected(primitive.getPrimitiveId());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                if (e.isControlDown()) {
                    addSelection(e);
                } else {
                    setSelection(e);
                }
            }
        }
    }
}
