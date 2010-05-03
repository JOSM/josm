// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.Filters;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author Petr_Dlouhý
 */
public class FilterDialog extends ToggleDialog implements Listener , TableModelListener {

    private JTable userTable;
    private Filters filters = new Filters();
    private SideButton addButton;
    private SideButton editButton;
    private SideButton deleteButton;
    private SideButton upButton;
    private SideButton downButton;

    private final DataSetListener listenerAdapter = new DataSetListenerAdapter(this);

    public FilterDialog(){
        super(tr("Filter"), "filter", tr("Filter objects and hide/disable them."),
                Shortcut.registerShortcut("subwindow:filter", tr("Toggle: {0}", tr("Filter")), KeyEvent.VK_F, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 162);
        build();
    }

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(listenerAdapter, FireMode.IN_EDT_CONSOLIDATED);
        filters.executeFilters();
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(listenerAdapter);
        filters.clearFilterFlags();
    }

    protected JPanel buildButtonRow() {
        JPanel pnl = getButtonPanel(5);

        addButton = new SideButton(marktr("Add"), "add", "SelectionList", tr("Add filter."),
                new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                Filter filter = (Filter)SearchAction.showSearchDialog(new Filter());
                if(filter != null){
                    filters.addFilter(filter);
                    filters.executeFilters();
                }
            }
        });
        pnl.add(addButton);

        editButton = new SideButton(marktr("Edit"), "edit", "SelectionList", tr("Edit filter."),
                new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                Filter f = filters.getFilter(index);
                Filter filter = (Filter)SearchAction.showSearchDialog(f);
                if(filter != null){
                    filters.setFilter(index, filter);
                    filters.executeFilters();
                }
            }
        });
        pnl.add(editButton);

        deleteButton = new SideButton(marktr("Delete"), "delete", "SelectionList", tr("Delete filter."),
                new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filters.removeFilter(index);
            }
        });
        pnl.add(deleteButton);

        upButton = new SideButton(marktr("Up"), "up", "SelectionList", tr("Move filter up."),
                new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filters.moveUpFilter(index);
                userTable.getSelectionModel().setSelectionInterval(index-1, index-1);
            }
        });
        pnl.add(upButton);

        downButton = new SideButton(marktr("Down"), "down", "SelectionList", tr("Move filter down."),
                new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filters.moveDownFilter(index);
                userTable.getSelectionModel().setSelectionInterval(index+1, index+1);
            }
        });
        pnl.add(downButton);
        return pnl;
    }

    protected String[] columnToolTips = {
            tr("Enable filter"),
            tr("Hide elements"),
            null,
            tr("Apply also for children"),
            tr("Inverse filter"),
            tr("Filter mode")
    };

    protected void build() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        userTable = new JTable(filters){
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };

        filters.addTableModelListener(this);

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userTable.getColumnModel().getColumn(0).setMaxWidth(1);
        userTable.getColumnModel().getColumn(1).setMaxWidth(1);
        userTable.getColumnModel().getColumn(3).setMaxWidth(1);
        userTable.getColumnModel().getColumn(4).setMaxWidth(1);
        userTable.getColumnModel().getColumn(5).setMaxWidth(1);

        userTable.getColumnModel().getColumn(0).setResizable(false);
        userTable.getColumnModel().getColumn(1).setResizable(false);
        userTable.getColumnModel().getColumn(3).setResizable(false);
        userTable.getColumnModel().getColumn(4).setResizable(false);
        userTable.getColumnModel().getColumn(5).setResizable(false);

        userTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
        userTable.setDefaultRenderer(String.class, new StringRenderer());

        tableChanged(null);

        pnl.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // -- the button row
        pnl.add(buildButtonRow(), BorderLayout.SOUTH);
        /*userTable.addMouseListener(new DoubleClickAdapter());*/
        add(pnl, BorderLayout.CENTER);
    }

    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        filters.executeFilters();
    }

    static class StringRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            Filters model = (Filters)table.getModel();
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setEnabled(model.isCellEnabled(row, column));
            return cell;
        }
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            Filters model = (Filters)table.getModel();
            setSelected((Boolean)value);
            setEnabled(model.isCellEnabled(row, column));
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            return this;
        }
    }

    public void tableChanged(TableModelEvent e){
        setTitle(tr("Filter Hidden:{0} Disabled:{1}", filters.hiddenCount, filters.disabledCount));
    }

    public void drawOSDText(Graphics2D g) {
        filters.drawOSDText(g);
    }
}
