// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.properties.CopyAllKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.HelpAction;
import org.openstreetmap.josm.gui.dialogs.properties.TaginfoAction;
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
        return buildTable(PointInTimeType.REFERENCE_POINT_IN_TIME, "table.referencetagtable", model::getReferencePointInTime);
    }

    @Override
    protected JTable buildCurrentTable() {
        return buildTable(PointInTimeType.CURRENT_POINT_IN_TIME, "table.currenttagtable", model::getCurrentPointInTime);
    }

    private JTable buildTable(PointInTimeType pointInTime, String name, Supplier<HistoryOsmPrimitive> histoSp) {
        TagTableModel tagTableModel = model.getTagTableModel(pointInTime);
        JTable table = new JTable(tagTableModel, new TagTableColumnModel());
        table.setName(name);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.setTransferHandler(new TagInfoTransferHandler());
        table.addFocusListener(new RepaintOnFocusChange());
        JPopupMenu tagMenu = new JPopupMenu();

        IntFunction<String> tagKeyFn = x -> (String) table.getValueAt(x, 0);
        IntFunction<Map<String, Integer>> tagValuesFn = x -> {
            Map<String, Integer> map = new HashMap<>();
            String key = tagTableModel.getValue((String) table.getValueAt(x, 0));
            if (key != null) {
                map.put(key, 1);
            }
            return map;
        };
        Supplier<Collection<? extends Tagged>> objectSp = () -> Arrays.asList(histoSp.get());

        tagMenu.add(new CopyValueAction(table, tagKeyFn, objectSp));
        tagMenu.add(new CopyKeyValueAction(table, tagKeyFn, objectSp));
        tagMenu.add(new CopyAllKeyValueAction(table, tagKeyFn, objectSp));
        tagMenu.addSeparator();
        tagMenu.add(new HelpAction(table, tagKeyFn, tagValuesFn, null, null));
        tagMenu.add(new TaginfoAction(table, tagKeyFn, tagValuesFn, null, null));

        table.addMouseListener(new PopupMenuLauncher(tagMenu));
        return table;
    }
}
