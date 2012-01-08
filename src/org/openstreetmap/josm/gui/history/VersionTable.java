// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * VersionTable shows a list of version in a {@see History} of an {@see OsmPrimitive}.
 *
 */
public class VersionTable extends JTable implements Observer{
    private VersionTablePopupMenu popupMenu;

    protected void build() {
        getTableHeader().setFont(getTableHeader().getFont().deriveFont(9f));
        setRowSelectionAllowed(false);
        setShowGrid(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setBackground(UIManager.getColor("Button.background"));
        setIntercellSpacing(new Dimension(6, 0));
        putClientProperty("terminateEditOnFocusLost", true);
        popupMenu = new VersionTablePopupMenu();
        addMouseListener(new PopupMenuTrigger());
        getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                adjustColumnWidth(VersionTable.this, 0);
                adjustColumnWidth(VersionTable.this, 4);
                adjustColumnWidth(VersionTable.this, 5);
            }
        });
    }

    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addObserver(this);
        build();
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

    public void update(Observable o, Object arg) {
        repaint();
    }

    protected void showPopupMenu(MouseEvent evt) {
        HistoryBrowserModel.VersionTableModel model = getVersionTableModel();
        int row = rowAtPoint(evt.getPoint());
        if (!model.isLatest(row)) {
            HistoryOsmPrimitive primitive = model.getPrimitive(row);
            popupMenu.prepare(primitive);
            popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    class PopupMenuTrigger extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
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

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value == null) return null;
            boolean val = (Boolean) value;
            btn.setSelected(val);
            btn.addItemListener(this);
            return btn;
        }

        public Object getCellEditorValue() {
            btn.removeItemListener(this);
            return btn.isSelected();
        }

        public void itemStateChanged(ItemEvent e) {
            fireEditingStopped();
        }
    }

    public static class LabelRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            return (Component) value;
        }
    }

    public static class AlignedRenderer extends JLabel implements TableCellRenderer {
        public AlignedRenderer(int hAlignment) {
            setHorizontalAlignment(hAlignment);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            String v = value.toString();
            setText(v);
            return this;
        }
    }

    private static void adjustColumnWidth(JTable tbl, int col) {
        int maxwidth = 0;

        for (int row=0; row<tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
            Object val = tbl.getValueAt(row, col);
            Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
            maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
        }
        TableCellRenderer tcr = tbl.getTableHeader().getDefaultRenderer();
        Object val = tbl.getColumnModel().getColumn(col).getHeaderValue();
        Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, -1, col);
        maxwidth = Math.max(comp.getPreferredSize().width + Main.pref.getInteger("table.header-inset", 2), maxwidth);

        int spacing = tbl.getIntercellSpacing().width;
        tbl.getColumnModel().getColumn(col).setPreferredWidth(maxwidth + spacing);
    }

}
