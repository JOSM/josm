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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.FilterModel;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent.DatasetEventType;
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
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrame.MapModeChangeListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.util.MultikeyActionsHandler;
import org.openstreetmap.josm.gui.util.MultikeyShortcutAction;
import org.openstreetmap.josm.gui.widgets.DisableShortcutsOnFocusGainedTextField;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The filter dialog displays a list of filters that are active on the current edit layer.
 *
 * @author Petr_Dlouhý
 */
public class FilterDialog extends ToggleDialog implements DataSetListener, MapModeChangeListener {

    private JTable userTable;
    private final FilterTableModel filterModel = new FilterTableModel();

    private final EnableFilterAction enableFilterAction;
    private final HidingFilterAction hidingFilterAction;

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
        MapFrame.addMapModeChangeListener(this);
        filterModel.executeFilters(true);
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(this);
        MapFrame.removeMapModeChangeListener(this);
        filterModel.model.clearFilterFlags();
        MainApplication.getLayerManager().invalidateEditLayer();
    }

    private static final Shortcut ENABLE_FILTER_SHORTCUT
    = Shortcut.registerShortcut("core_multikey:enableFilter", tr("Multikey: {0}", tr("Enable filter")),
            KeyEvent.VK_E, Shortcut.ALT_CTRL);

    private static final Shortcut HIDING_FILTER_SHORTCUT
    = Shortcut.registerShortcut("core_multikey:hidingFilter", tr("Multikey: {0}", tr("Hide filter")),
            KeyEvent.VK_H, Shortcut.ALT_CTRL);

    private static final String[] COLUMN_TOOLTIPS = {
            Shortcut.makeTooltip(tr("Enable filter"), ENABLE_FILTER_SHORTCUT.getKeyStroke()),
            Shortcut.makeTooltip(tr("Hiding filter"), HIDING_FILTER_SHORTCUT.getKeyStroke()),
            null,
            tr("Inverse filter"),
            tr("Filter mode")
    };

    /**
     * Builds the GUI.
     */
    protected void build() {
        userTable = new UserTable(filterModel);

        userTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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
        userTable.setDefaultEditor(String.class, new DefaultCellEditor(new DisableShortcutsOnFocusGainedTextField()));

        SideButton addButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Add"));
                putValue(SHORT_DESCRIPTION, tr("Add filter."));
                new ImageProvider("dialogs", "add").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                SearchSetting searchSetting = SearchAction.showSearchDialog(new Filter());
                if (searchSetting != null) {
                    filterModel.addFilter(new Filter(searchSetting));
                }
            }
        });
        SideButton editButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Edit"));
                putValue(SHORT_DESCRIPTION, tr("Edit filter."));
                new ImageProvider("dialogs", "edit").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if (index < 0) return;
                Filter f = filterModel.getFilter(index);
                SearchSetting searchSetting = SearchAction.showSearchDialog(f);
                if (searchSetting != null) {
                    filterModel.setFilter(index, new Filter(searchSetting));
                }
            }
        });
        SideButton deleteButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Delete"));
                putValue(SHORT_DESCRIPTION, tr("Delete filter."));
                new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if (index >= 0) {
                    filterModel.removeFilter(index);
                }
            }
        });
        SideButton upButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Up"));
                putValue(SHORT_DESCRIPTION, tr("Move filter up."));
                new ImageProvider("dialogs", "up").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if (index >= 0) {
                    filterModel.moveUpFilter(index);
                    userTable.getSelectionModel().setSelectionInterval(index-1, index-1);
                }
            }
        });
        SideButton downButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Down"));
                putValue(SHORT_DESCRIPTION, tr("Move filter down."));
                new ImageProvider("dialogs", "down").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectionModel().getMinSelectionIndex();
                if (index >= 0) {
                    filterModel.moveDownFilter(index);
                    userTable.getSelectionModel().setSelectionInterval(index+1, index+1);
                }
            }
        });

        // Toggle filter "enabled" on Enter
        InputMapUtils.addEnterAction(userTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectedRow();
                if (index >= 0) {
                    Filter filter = filterModel.getFilter(index);
                    filterModel.setValueAt(!filter.enable, index, FilterTableModel.COL_ENABLED);
                }
            }
        });

        // Toggle filter "hiding" on Spacebar
        InputMapUtils.addSpacebarAction(userTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = userTable.getSelectedRow();
                if (index >= 0) {
                    Filter filter = filterModel.getFilter(index);
                    filterModel.setValueAt(!filter.hiding, index, FilterTableModel.COL_HIDING);
                }
            }
        });

        createLayout(userTable, true, Arrays.asList(
                addButton, editButton, deleteButton, upButton, downButton
        ));
    }

    @Override
    public void destroy() {
        MultikeyActionsHandler.getInstance().removeAction(enableFilterAction);
        MultikeyActionsHandler.getInstance().removeAction(hidingFilterAction);
        super.destroy();
    }

    static final class UserTable extends JTable {
        static final class UserTableHeader extends JTableHeader {
            UserTableHeader(TableColumnModel cm) {
                super(cm);
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                if (index == -1)
                    return null;
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return COLUMN_TOOLTIPS[realIndex];
            }
        }

        UserTable(TableModel dm) {
            super(dm);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new UserTableHeader(columnModel);
        }
    }

    static class StringRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            TableModel model = table.getModel();
            if (model instanceof FilterTableModel) {
                cell.setEnabled(((FilterTableModel) model).isCellEnabled(row, column));
            }
            return cell;
        }
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            FilterTableModel model = (FilterTableModel) table.getModel();
            setSelected(value != null && (Boolean) value);
            setEnabled(model.isCellEnabled(row, column));
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            return this;
        }
    }

    /**
     * Updates the headline of this dialog to display the number of active filters.
     */
    public void updateDialogHeader() {
        SwingUtilities.invokeLater(() -> setTitle(
                tr("Filter Hidden:{0} Disabled:{1}",
                        filterModel.model.getDisabledAndHiddenCount(), filterModel.model.getDisabledCount())));
    }

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     */
    public void drawOSDText(Graphics2D g) {
        filterModel.drawOSDText(g);
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        filterModel.executeFilters();
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        filterModel.executeFilters();
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        if (DatasetEventType.FILTERS_CHANGED != event.getType()) {
            filterModel.executeFilters();
        }
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
        filterModel.executeFilters(FilterModel.getAffectedPrimitives(event.getPrimitives()));
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        filterModel.executeFilters(FilterModel.getAffectedPrimitives(event.getPrimitives()));
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        filterModel.executeFilters(FilterModel.getAffectedPrimitives(event.getPrimitives()));
    }

    @Override
    public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
        filterModel.executeFilters();
    }

    /**
     * This method is intended for Plugins getting the filtermodel and using .addFilter() to
     * add a new filter.
     * @return the filtermodel
     */
    public FilterTableModel getFilterModel() {
        return filterModel;
    }

    abstract class AbstractFilterAction extends AbstractAction implements MultikeyShortcutAction {

        protected transient Filter lastFilter;

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MultikeyInfo> getMultikeyCombinations() {
            List<MultikeyInfo> result = new ArrayList<>();

            for (int i = 0; i < filterModel.getRowCount(); i++) {
                Filter filter = filterModel.getFilter(i);
                MultikeyInfo info = new MultikeyInfo(i, filter.text);
                result.add(info);
            }

            return result;
        }

        protected final boolean isLastFilterValid() {
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

    private class EnableFilterAction extends AbstractFilterAction {

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

        HidingFilterAction() {
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
