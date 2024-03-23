// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.RestorePropertyAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.dialogs.properties.CopyAllKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.HelpTagAction;
import org.openstreetmap.josm.gui.dialogs.properties.TaginfoAction;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;

/**
 * TagInfoViewer is a UI component which displays the list of tags of two
 * version of a {@link org.openstreetmap.josm.data.osm.OsmPrimitive} in a {@link org.openstreetmap.josm.data.osm.history.History}.
 *
 * <ul>
 *   <li>on the left, it displays the list of tags for the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the list of tags for the version at {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 * @since 1709
 */
public class TagInfoViewer extends HistoryViewerPanel {
    private JTable reference;
    private JTable current;
    private static final class RepaintOnFocusChange implements FocusListener {
        @Override
        public void focusLost(FocusEvent e) {
            repaintSelected(e);
        }

        @Override
        public void focusGained(FocusEvent e) {
            repaintSelected(e);
        }

        private static void repaintSelected(FocusEvent e) {
            // we would only need the selected rows, but this is easier:
            e.getComponent().repaint();
        }
    }

    /**
     * Constructs a new {@code TagInfoViewer}.
     * @param model The history browsing model
     */
    public TagInfoViewer(HistoryBrowserModel model) {
        super(model);
    }

    @Override
    protected JTable buildReferenceTable() {
        reference = buildTable(PointInTimeType.REFERENCE_POINT_IN_TIME);
        return reference;
    }

    @Override
    protected JTable buildCurrentTable() {
        current = buildTable(PointInTimeType.CURRENT_POINT_IN_TIME);
        return current;
    }

    private JTable buildTable(PointInTimeType pointInTime) {
        TagTableModel tagTableModel = model.getTagTableModel(pointInTime);
        JTable table = new JTable(tagTableModel, new TagTableColumnModel());
        TableHelper.setFont(table, getClass());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.getTableHeader().setReorderingAllowed(false);
        table.setTransferHandler(new TagInfoTransferHandler());
        table.addFocusListener(new RepaintOnFocusChange());
        JPopupMenu tagMenu = new JPopupMenu();

        IntFunction<String> tagKeyFn = x -> (String) table.getValueAt(x, 0);
        IntFunction<String> tagValueFn = x -> tagTableModel.getValue(tagKeyFn.apply(x));
        IntFunction<Map<String, Integer>> tagValuesFn = x -> {
            String value = tagValueFn.apply(x);
            return value != null ? Collections.singletonMap(value, 1) : Collections.emptyMap();
        };
        Supplier<Collection<? extends Tagged>> objectSp = () -> Collections.singletonList(model.getPointInTime(pointInTime));
        Supplier<OsmPrimitive> primitiveSupplier = () -> getPrimitiveFromDataSet(pointInTime);

        tagMenu.add(trackJosmAction(new CopyValueAction(table, tagKeyFn, objectSp)));
        final CopyKeyValueAction copyKeyValueAction = new CopyKeyValueAction(table, tagKeyFn, objectSp);
        tagMenu.add(trackJosmAction(copyKeyValueAction));
        tagMenu.addPopupMenuListener(copyKeyValueAction);
        tagMenu.add(trackJosmAction(new CopyAllKeyValueAction(table, tagKeyFn, objectSp)));
        tagMenu.add(new RestorePropertyAction(tagKeyFn, tagValueFn, primitiveSupplier, table.getSelectionModel()));
        tagMenu.addSeparator();
        tagMenu.add(trackJosmAction(new HelpTagAction(table, tagKeyFn, tagValuesFn)));
        TaginfoAction taginfoAction = new TaginfoAction(table, tagKeyFn, tagValuesFn, null, null);
        tagMenu.add(trackJosmAction(taginfoAction.toTagHistoryAction()));
        tagMenu.add(trackJosmAction(taginfoAction));

        table.addMouseListener(new PopupMenuLauncher(tagMenu));
        return table;
    }

    /**
     * Use current data to adjust preferredWidth for both tables.
     * @since 19013
     */
    public void adjustWidths() {
        // We have two tables with 3 columns each. no column should get more than 1/4 of the size
        int maxWidth = this.getWidth() / 4;
        if (maxWidth == 0)
            maxWidth = Integer.MAX_VALUE;
        adjustWidths(reference, maxWidth);
        adjustWidths(current, maxWidth);
    }

    private static void adjustWidths(JTable table, int maxWidth) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableHelper.adjustColumnWidth(table, column, maxWidth);
        }
    }
}
