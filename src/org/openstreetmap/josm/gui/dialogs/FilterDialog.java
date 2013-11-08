// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.MultikeyActionsHandler;
import org.openstreetmap.josm.tools.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author Petr_Dlouhý
 */
public class FilterDialog extends ToggleDialog implements DataSetListener {

    private JTable userTable;
    private FilterTableModel filterModel = new FilterTableModel();

    private EnableFilterAction enableFilterAction;
    private HidingFilterAction hidingFilterAction;

    /**
     * Constructs a new {@code FilterDialog}
     */
    public FilterDialog() {
        super(tr("Filter"), "filter", tr("Filter objects and hide/disable them."),
                Shortcut.registerShortcut("subwindow:filter", tr("Toggle: {0}", tr("Filter")),
                        KeyEvent.VK_F, Shortcut.ALT_SHIFT), 162);
        build();
        enableFilterAction = new EnableFilterAction();
        hidingFilterAction = new HidingFilterAction();
        MultikeyActionsHandler.getInstance().addAction(enableFilterAction);
        MultikeyActionsHandler.getInstance().addAction(hidingFilterAction);
    }

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(this, FireMode.IN_EDT_CONSOLIDATED);
        filterModel.executeFilters();
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(this);
        filterModel.clearFilterFlags();
        Main.map.mapView.repaint();
    }

    private static final Shortcut ENABLE_FILTER_SHORTCUT
    = Shortcut.registerShortcut("core_multikey:enableFilter", tr("Multikey: {0}", tr("Enable filter")),
            KeyEvent.VK_E, Shortcut.ALT_CTRL);

    private static final Shortcut HIDING_FILTER_SHORTCUT
    = Shortcut.registerShortcut("core_multikey:hidingFilter", tr("Multikey: {0}", tr("Hide filter")),
            KeyEvent.VK_H, Shortcut.ALT_CTRL);


    protected final String[] columnToolTips = {
            Main.platform.makeTooltip(tr("Enable filter"), ENABLE_FILTER_SHORTCUT),
            Main.platform.makeTooltip(tr("Hiding filter"), HIDING_FILTER_SHORTCUT),
            null,
            tr("Inverse filter"),
            tr("Filter mode")
    };

    protected void build() {
        userTable = new JTable(filterModel){
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

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userTable.getColumnModel().getColumn(0).setMaxWidth(1);
        userTable.getColumnModel().getColumn(1).setMaxWidth(1);
        userTable.getColumnModel().getColumn(3).setMaxWidth(1);
        userTable.getColumnModel().getColumn(4).setMaxWidth(1);

        userTable.getColumnModel().getColumn(0).setResizable(false);
        userTable.getColumnModel().getColumn(1).setResizable(false);
        userTable.getColumnModel().getColumn(3).setResizable(false);
        userTable.getColumnModel().getColumn(4).setResizable(false);

        userTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
        userTable.setDefaultRenderer(String.class, new StringRenderer());

        SideButton addButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Add"));
                putValue(SHORT_DESCRIPTION,  tr("Add filter."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs","add"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                Filter filter = (Filter)SearchAction.showSearchDialog(new Filter());
                if(filter != null){
                    filterModel.addFilter(filter);
                }
            }});
        SideButton editButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Edit"));
                putValue(SHORT_DESCRIPTION, tr("Edit filter."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                Filter f = filterModel.getFilter(index);
                Filter filter = (Filter)SearchAction.showSearchDialog(f);
                if(filter != null){
                    filterModel.setFilter(index, filter);
                }
            }
        });
        SideButton deleteButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Delete"));
                putValue(SHORT_DESCRIPTION, tr("Delete filter."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filterModel.removeFilter(index);
            }
        });
        SideButton upButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Up"));
                putValue(SHORT_DESCRIPTION, tr("Move filter up."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "up"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filterModel.moveUpFilter(index);
                userTable.getSelectionModel().setSelectionInterval(index-1, index-1);
            }

        });
        SideButton downButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Down"));
                putValue(SHORT_DESCRIPTION, tr("Move filter down."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "down"));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if(index < 0) return;
                filterModel.moveDownFilter(index);
                userTable.getSelectionModel().setSelectionInterval(index+1, index+1);
            }
        });

        // Toggle filter "enabled" on Enter
        InputMapUtils.addEnterAction(userTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectedRow();
                if (index<0) return;
                Filter filter = filterModel.getFilter(index);
                filterModel.setValueAt(!filter.enable, index, FilterTableModel.COL_ENABLED);
            }
        });

        // Toggle filter "hiding" on Spacebar
        InputMapUtils.addSpacebarAction(userTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectedRow();
                if (index<0) return;
                Filter filter = filterModel.getFilter(index);
                filterModel.setValueAt(!filter.hiding, index, FilterTableModel.COL_HIDING);
            }
        });

        createLayout(userTable, true, Arrays.asList(new SideButton[] {
                addButton, editButton, deleteButton, upButton, downButton
        }));
    }

    @Override
    public void destroy() {
        MultikeyActionsHandler.getInstance().removeAction(enableFilterAction);
        MultikeyActionsHandler.getInstance().removeAction(hidingFilterAction);
        super.destroy();
    }

    static class StringRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            FilterTableModel model = (FilterTableModel)table.getModel();
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setEnabled(model.isCellEnabled(row, column));
            return cell;
        }
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
            FilterTableModel model = (FilterTableModel)table.getModel();
            setSelected(value != null && (Boolean)value);
            setEnabled(model.isCellEnabled(row, column));
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            return this;
        }
    }

    public void updateDialogHeader() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setTitle(tr("Filter Hidden:{0} Disabled:{1}", filterModel.disabledAndHiddenCount, filterModel.disabledCount));
            }
        });
    }

    public void drawOSDText(Graphics2D g) {
        filterModel.drawOSDText(g);
    }

    /**
     * Returns the list of primitives whose filtering can be affected by change in primitive
     * @param primitives list of primitives to check
     * @return List of primitives whose filtering can be affected by change in source primitives
     */
    private Collection<OsmPrimitive> getAffectedPrimitives(Collection<? extends OsmPrimitive> primitives) {
        // Filters can use nested parent/child expression so complete tree is necessary
        Set<OsmPrimitive> result = new HashSet<OsmPrimitive>();
        Stack<OsmPrimitive> stack = new Stack<OsmPrimitive>();
        stack.addAll(primitives);

        while (!stack.isEmpty()) {
            OsmPrimitive p = stack.pop();

            if (result.contains(p)) {
                continue;
            }

            result.add(p);

            if (p instanceof Way) {
                for (OsmPrimitive n: ((Way)p).getNodes()) {
                    stack.push(n);
                }
            } else if (p instanceof Relation) {
                for (RelationMember rm: ((Relation)p).getMembers()) {
                    stack.push(rm.getMember());
                }
            }

            for (OsmPrimitive ref: p.getReferrers()) {
                stack.push(ref);
            }
        }

        return result;
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        filterModel.executeFilters();
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        // Do nothing
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        filterModel.executeFilters();
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        filterModel.executeFilters(event.getPrimitives());
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        filterModel.executeFilters();
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        filterModel.executeFilters(getAffectedPrimitives(event.getPrimitives()));
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        filterModel.executeFilters(getAffectedPrimitives(event.getPrimitives()));
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        filterModel.executeFilters(getAffectedPrimitives(event.getPrimitives()));
    }

    abstract class AbstractFilterAction extends AbstractAction implements MultikeyShortcutAction {

        protected Filter lastFilter;

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MultikeyInfo> getMultikeyCombinations() {
            List<MultikeyInfo> result = new ArrayList<MultikeyShortcutAction.MultikeyInfo>();

            for (int i=0; i<filterModel.getRowCount(); i++) {
                Filter filter = filterModel.getFilter(i);
                MultikeyInfo info = new MultikeyInfo(i, filter.text);
                result.add(info);
            }

            return result;
        }

        protected boolean isLastFilterValid() {
            return lastFilter != null && filterModel.getFilters().contains(lastFilter);
        }

        @Override
        public MultikeyInfo getLastMultikeyAction() {
            if (isLastFilterValid())
                return new MultikeyInfo(-1, lastFilter.text);
            else
                return null;
        }

    }

    private class EnableFilterAction extends AbstractFilterAction  {

        EnableFilterAction() {
            putValue(SHORT_DESCRIPTION, tr("Enable filter"));
            ENABLE_FILTER_SHORTCUT.setAccelerator(this);
        }

        @Override
        public Shortcut getMultikeyShortcut() {
            return ENABLE_FILTER_SHORTCUT;
        }

        @Override
        public void executeMultikeyAction(int index, boolean repeatLastAction) {
            if (index >= 0 && index < filterModel.getRowCount()) {
                Filter filter = filterModel.getFilter(index);
                filterModel.setValueAt(!filter.enable, index, FilterTableModel.COL_ENABLED);
                lastFilter = filter;
            } else if (repeatLastAction && isLastFilterValid()) {
                filterModel.setValueAt(!lastFilter.enable, filterModel.getFilters().indexOf(lastFilter), FilterTableModel.COL_ENABLED);
            }
        }
    }

    private class HidingFilterAction extends AbstractFilterAction {

        public HidingFilterAction() {
            putValue(SHORT_DESCRIPTION, tr("Hiding filter"));
            HIDING_FILTER_SHORTCUT.setAccelerator(this);
        }

        @Override
        public Shortcut getMultikeyShortcut() {
            return HIDING_FILTER_SHORTCUT;
        }

        @Override
        public void executeMultikeyAction(int index, boolean repeatLastAction) {
            if (index >= 0 && index < filterModel.getRowCount()) {
                Filter filter = filterModel.getFilter(index);
                filterModel.setValueAt(!filter.hiding, index, FilterTableModel.COL_HIDING);
                lastFilter = filter;
            } else if (repeatLastAction && isLastFilterValid()) {
                filterModel.setValueAt(!lastFilter.hiding, filterModel.getFilters().indexOf(lastFilter), FilterTableModel.COL_HIDING);
            }
        }

    }
}
