// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;

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
        setRowSelectionAllowed(false);
        popupMenu = new VersionTablePopupMenu();
        addMouseListener(new PopupMenuTrigger());
    }

    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addObserver(this);
        build();
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
            HistoryBrowserModel.VersionTableModel model = (HistoryBrowserModel.VersionTableModel)table.getModel();
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
}
