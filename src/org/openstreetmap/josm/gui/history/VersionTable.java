// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * VersionTable shows a list of version in a {@link org.openstreetmap.josm.data.osm.history.History}
 * of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive}.
 *
 */
public class VersionTable extends JTable implements Observer{
    private VersionTablePopupMenu popupMenu;
    private final HistoryBrowserModel model;

    protected void build() {
        getTableHeader().setFont(getTableHeader().getFont().deriveFont(9f));
        setRowSelectionAllowed(false);
        setShowGrid(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setBackground(UIManager.getColor("Button.background"));
        setIntercellSpacing(new Dimension(6, 0));
        putClientProperty("terminateEditOnFocusLost", true);
        popupMenu = new VersionTablePopupMenu();
        addMouseListener(new MouseListener());
        getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                adjustColumnWidth(VersionTable.this, 0, 0);
                adjustColumnWidth(VersionTable.this, 1, -8);
                adjustColumnWidth(VersionTable.this, 2, -8);
                adjustColumnWidth(VersionTable.this, 3, 0);
                adjustColumnWidth(VersionTable.this, 4, 0);
            }
        });
    }

    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addObserver(this);
        build();
        this.model = model;
    }

    // some kind of hack to prevent the table from scrolling to the
    // right when clicking on the cells
    @Override
    public void scrollRectToVisible(Rectangle aRect) {
        super.scrollRectToVisible(new Rectangle(0, aRect.y, aRect.width, aRect.height));
    }

    protected HistoryBrowserModel.VersionTableModel getVersionTableModel() {
        return (HistoryBrowserModel.VersionTableModel) getModel();
    }

    @Override
    public void update(Observable o, Object arg) {
        repaint();
    }

    class MouseListener extends PopupMenuLauncher {
        public MouseListener() {
            super(popupMenu);
        }
        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row > 0 && (col == VersionTableColumnModel.COL_DATE || col == VersionTableColumnModel.COL_USER)) {
                    model.getVersionTableModel().setCurrentPointInTime(row);
                    model.getVersionTableModel().setReferencePointInTime(row - 1);
                }
            }
        }
        @Override
        protected int checkTableSelection(JTable table, Point p) {
            HistoryBrowserModel.VersionTableModel model = getVersionTableModel();
            int row = rowAtPoint(p);
            if (row > -1 && !model.isLatest(row)) {
                popupMenu.prepare(model.getPrimitive(row));
            }
            return row;
        }
    }

    static class ChangesetInfoAction extends AbstractInfoAction {
        private HistoryOsmPrimitive primitive;

        public ChangesetInfoAction() {
            super(true);
            putValue(NAME, tr("Changeset info"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with information about the changeset"));
            putValue(SMALL_ICON, ImageProvider.get("about"));
        }

        @Override
        protected String createInfoUrl(Object infoObject) {
            HistoryOsmPrimitive primitive = (HistoryOsmPrimitive) infoObject;
            return getBaseBrowseUrl() + "/changeset/" + primitive.getChangesetId();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            String url = createInfoUrl(primitive);
            launchBrowser(url);
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            putValue(NAME, tr("Show changeset {0}", primitive.getChangesetId()));
            this.primitive = primitive;
        }
    }

    static class VersionTablePopupMenu extends JPopupMenu {

        private ChangesetInfoAction changesetInfoAction;

        protected void build() {
            changesetInfoAction = new ChangesetInfoAction();
            add(changesetInfoAction);
        }
        public VersionTablePopupMenu() {
            super();
            build();
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            changesetInfoAction.prepare(primitive);
            invalidate();
        }
    }

    public static class RadioButtonRenderer extends JRadioButton implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            setSelected(value != null && (Boolean)value);
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    public static class RadioButtonEditor extends DefaultCellEditor implements ItemListener {

        private JRadioButton btn;

        public RadioButtonEditor() {
            super(new JCheckBox());
            btn = new JRadioButton();
            btn.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value == null) return null;
            boolean val = (Boolean) value;
            btn.setSelected(val);
            btn.addItemListener(this);
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            btn.removeItemListener(this);
            return btn.isSelected();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            fireEditingStopped();
        }
    }

    public static class AlignedRenderer extends JLabel implements TableCellRenderer {
        public AlignedRenderer(int hAlignment) {
            setHorizontalAlignment(hAlignment);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            String v = value.toString();
            setText(v);
            return this;
        }
    }

    private static void adjustColumnWidth(JTable tbl, int col, int cellInset) {
        int maxwidth = 0;

        for (int row=0; row<tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
            Object val = tbl.getValueAt(row, col);
            Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
            maxwidth = Math.max(comp.getPreferredSize().width + cellInset, maxwidth);
        }
        TableCellRenderer tcr = tbl.getTableHeader().getDefaultRenderer();
        Object val = tbl.getColumnModel().getColumn(col).getHeaderValue();
        Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, -1, col);
        maxwidth = Math.max(comp.getPreferredSize().width + Main.pref.getInteger("table.header-inset", 0), maxwidth);

        int spacing = tbl.getIntercellSpacing().width;
        tbl.getColumnModel().getColumn(col).setPreferredWidth(maxwidth + spacing);
    }

}
