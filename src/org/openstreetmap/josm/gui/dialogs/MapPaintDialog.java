// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class MapPaintDialog extends ToggleDialog {

    protected StylesTable tblStyles;
    protected StylesModel model;
    protected DefaultListSelectionModel selectionModel;

    protected OnOffAction onoffAction;
    protected ReloadAction reloadAction;

    public MapPaintDialog() {
        super(tr("Map Paint Styles"), "mapstyle", tr("configure the map painting style"),
                Shortcut.registerShortcut("subwindow:authors", tr("Toggle: {0}", tr("Authors")), KeyEvent.VK_M, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);
        build();
    }

    protected void build() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());

        model = new StylesModel();
        
        tblStyles = new StylesTable(model);
        tblStyles.setSelectionModel(selectionModel= new DefaultListSelectionModel());
        tblStyles.addMouseListener(new PopupMenuHandler());
        tblStyles.putClientProperty("terminateEditOnFocusLost", true);
        tblStyles.setBackground(UIManager.getColor("Panel.background"));
        tblStyles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblStyles.setTableHeader(null);
        tblStyles.getColumnModel().getColumn(0).setMaxWidth(1);
        tblStyles.getColumnModel().getColumn(0).setResizable(false);
        tblStyles.setShowGrid(false);
        tblStyles.setIntercellSpacing(new Dimension(0, 0));

        pnl.add(new JScrollPane(tblStyles), BorderLayout.CENTER);

        pnl.add(buildButtonRow(), BorderLayout.SOUTH);

        add(pnl, BorderLayout.CENTER);
    }

    protected static class StylesTable extends JTable {

        public StylesTable(TableModel dm) {
            super(dm);
        }

        public void scrollToVisible(int row, int col) {
            if (!(getParent() instanceof JViewport))
                return;
            JViewport viewport = (JViewport) getParent();
            Rectangle rect = getCellRect(row, col, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
        }
    }

    protected JPanel buildButtonRow() {
        JPanel p = getButtonPanel(4);
        reloadAction = new ReloadAction();
        onoffAction = new OnOffAction();
        MoveUpDownAction up = new MoveUpDownAction(false);
        MoveUpDownAction down = new MoveUpDownAction(true);
        selectionModel.addListSelectionListener(onoffAction);
        selectionModel.addListSelectionListener(reloadAction);
        selectionModel.addListSelectionListener(up);
        selectionModel.addListSelectionListener(down);
        p.add(new SideButton(onoffAction));
        p.add(new SideButton(up));
        p.add(new SideButton(down));
        p.add(new SideButton(new LaunchMapPaintPreferencesAction()));

        return p;
    }

    @Override
    public void showNotify() {
        MapPaintStyles.addMapPaintSylesUpdateListener(model);
    }

    @Override
    public void hideNotify() {
        MapPaintStyles.removeMapPaintSylesUpdateListener(model);
    }

    protected class StylesModel extends AbstractTableModel implements MapPaintSylesUpdateListener {

        private StyleSource getRow(int i) {
            return MapPaintStyles.getStyles().getStyleSources().get(i);
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return MapPaintStyles.getStyles().getStyleSources().size();
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0)
                return getRow(row).active;
            else
                return getRow(row).getDisplayString();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        Class<?>[] columnClasses = {Boolean.class, StyleSource.class};

        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (row < 0 || row >= getRowCount() || aValue == null)
                return;
            if (column == 0) {
                MapPaintStyles.toggleStyleActive(row);
            }
        }

        /**
         * Make sure the first of the selected entry is visible in the
         * views of this model.
         */
        protected void ensureSelectedIsVisible() {
            int index = selectionModel.getMinSelectionIndex();
            if (index < 0) return;
            if (index >= getRowCount()) return;
            tblStyles.scrollToVisible(index, 0);
            tblStyles.repaint();
        }

        /**
         * MapPaintSylesUpdateListener interface
         */

        @Override
        public void mapPaintStylesUpdated() {
            fireTableDataChanged();
            tblStyles.repaint();
        }

        @Override
        public void mapPaintStyleEntryUpdated(int idx) {
            fireTableRowsUpdated(idx, idx);
            tblStyles.repaint();
        }
    }

    protected class OnOffAction extends AbstractAction implements ListSelectionListener {
        public OnOffAction() {
            putValue(SHORT_DESCRIPTION, tr("Turn selected styles on or off"));
            putValue(SMALL_ICON, ImageProvider.get("apply"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblStyles.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] pos = tblStyles.getSelectedRows();
            MapPaintStyles.toggleStyleActive(pos);
            selectionModel.clearSelection();
            for (int p: pos) {
                selectionModel.addSelectionInterval(p, p);
            }
        }
    }

    /**
     * The action to move down the currently selected entries in the list.
     */
    class MoveUpDownAction extends AbstractAction implements ListSelectionListener {
        final int increment;
        public MoveUpDownAction(boolean isDown) {
            increment = isDown ? 1 : -1;
            putValue(SMALL_ICON, isDown ? ImageProvider.get("dialogs", "down") : ImageProvider.get("dialogs", "up"));
            putValue(SHORT_DESCRIPTION, isDown ? tr("Move the selected entry one row down.") : tr("Move the selected entry one row up."));
            updateEnabledState();
        }

        public void updateEnabledState() {
            int[] sel = tblStyles.getSelectedRows();
            setEnabled(MapPaintStyles.canMoveStyles(sel, increment));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] sel = tblStyles.getSelectedRows();
            MapPaintStyles.moveStyles(sel, increment);

            selectionModel.clearSelection();
            for (int row: sel) {
                selectionModel.addSelectionInterval(row + increment, row + increment);
            }
            model.ensureSelectedIsVisible();
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
    
    /**
     * Opens preferences window and selects the mappaint tab.
     */
    class LaunchMapPaintPreferencesAction extends AbstractAction {
        public LaunchMapPaintPreferencesAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "mappaintpreference"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final PreferenceDialog p =new PreferenceDialog(Main.parent);
            p.selectMapPaintPreferenceTab();
            p.setVisible(true);
        }
    }

    protected class ReloadAction extends AbstractAction implements ListSelectionListener {
        public ReloadAction() {
            putValue(NAME, tr("Reload from file"));
            putValue(SHORT_DESCRIPTION, tr("reload selected styles from file"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            int[] pos = tblStyles.getSelectedRows();
            boolean e = pos.length > 0;
            for (int i : pos) {
                if (!model.getRow(i).isLocal()) {
                    e = false;
                    break;
                }
            }
            setEnabled(e);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int[] rows = tblStyles.getSelectedRows();
            MapPaintStyles.reloadStyles(rows);
            Main.worker.submit(new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            selectionModel.clearSelection();
                            for (int r: rows) {
                                selectionModel.addSelectionInterval(r, r);
                            }
                        }
                    });

                }
            });
        }
    }
    
    class PopupMenuHandler extends PopupMenuLauncher {
        @Override
        public void launch(MouseEvent evt) {
            Point p = evt.getPoint();
            int index = tblStyles.rowAtPoint(p);
            if (index < 0) return;
            if (!tblStyles.getCellRect(index, 1, false).contains(evt.getPoint()))
                return;
            if (!tblStyles.isRowSelected(index)) {
                tblStyles.setRowSelectionInterval(index, index);
            }
            MapPaintPopup menu = new MapPaintPopup();
            menu.show(MapPaintDialog.this, p.x, p.y);
        }
    }

    public class MapPaintPopup extends JPopupMenu {
        public MapPaintPopup() {
            add(reloadAction);
        }
    }
}
