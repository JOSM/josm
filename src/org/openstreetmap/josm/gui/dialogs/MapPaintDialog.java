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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class MapPaintDialog extends ToggleDialog {

    protected JTable tblStyles;
    protected StylesModel model;
    protected DefaultListSelectionModel selectionModel;

    protected OnOffAction onoffAction;
    protected ReloadAction reloadAction;

    public MapPaintDialog() {
        super(tr("Map Paint Styles"), "mapstyle", tr("configure the map painting style"),
                Shortcut.registerShortcut("subwindow:authors", tr("Toggle: {0}", tr("Authors")), KeyEvent.VK_M, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 250);
        build();
    }

    protected void build() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());

        model = new StylesModel();
        model.setStyles(MapPaintStyles.getStyles().getStyleSources());
        
        tblStyles = new JTable(model);
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

    protected JPanel buildButtonRow() {
        JPanel p = getButtonPanel(1);
        reloadAction = new ReloadAction();
        onoffAction = new OnOffAction();
        selectionModel.addListSelectionListener(onoffAction);
        selectionModel.addListSelectionListener(reloadAction);
        p.add(new SideButton(onoffAction));
        return p;
    }
    
    protected class StylesModel extends AbstractTableModel {
        List<StyleSource> data;

        public StylesModel() {
            this.data = new ArrayList<StyleSource>();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0)
                return data.get(row).active;
            else
                return data.get(row).getDisplayString();
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
                toggleOnOff(row);
            }
        }

        public void setStyles(Collection<? extends StyleSource> styles) {
            data.clear();
            if (styles !=null) {
                data.addAll(styles);
            }
            fireTableDataChanged();
        }

        public void toggleOnOff(int... rows) {
            for (Integer p : rows) {
                StyleSource s = model.data.get(p);
                s.active = !s.active;
            }
            if (rows.length == 1) {
                model.fireTableCellUpdated(rows[0], 0);
            } else {
                model.fireTableDataChanged();
            }
            ElemStyles.cacheIdx++;
            Main.map.mapView.preferenceChanged(null);
            Main.map.mapView.repaint();
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
            model.toggleOnOff(pos);
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
                if (!model.data.get(i).isLocal()) {
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
            int[] pos = tblStyles.getSelectedRows();
            for (int p : pos) {
                StyleSource s = model.data.get(p);
                s.loadStyleSource();
            }
            ElemStyles.cacheIdx++;
            Main.map.mapView.preferenceChanged(null);
            Main.map.mapView.repaint();
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
