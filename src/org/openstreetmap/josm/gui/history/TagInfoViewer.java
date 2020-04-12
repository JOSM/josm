// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

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

import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.dialogs.properties.CopyAllKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyKeyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.CopyValueAction;
import org.openstreetmap.josm.gui.dialogs.properties.HelpTagAction;
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
    protected JTable buildTable(PointInTimeType pointInTime) {
        TagTableModel tagTableModel = model.getTagTableModel(pointInTime);
        JTable table = new JTable(tagTableModel, new TagTableColumnModel());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.setTransferHandler(new TagInfoTransferHandler());
        table.addFocusListener(new RepaintOnFocusChange());
        JPopupMenu tagMenu = new JPopupMenu();

        IntFunction<String> tagKeyFn = x -> (String) table.getValueAt(x, 0);
        IntFunction<Map<String, Integer>> tagValuesFn = x -> {
            String key = tagTableModel.getValue((String) table.getValueAt(x, 0));
            if (key != null) {
                return Collections.singletonMap(key, 1);
            }
            return Collections.emptyMap();
        };
        Supplier<Collection<? extends Tagged>> objectSp = () -> Collections.singletonList(model.getPointInTime(pointInTime));

        tagMenu.add(trackJosmAction(new CopyValueAction(table, tagKeyFn, objectSp)));
        final CopyKeyValueAction copyKeyValueAction = new CopyKeyValueAction(table, tagKeyFn, objectSp);
        tagMenu.add(trackJosmAction(copyKeyValueAction));
        tagMenu.addPopupMenuListener(copyKeyValueAction);
        tagMenu.add(trackJosmAction(new CopyAllKeyValueAction(table, tagKeyFn, objectSp)));
        tagMenu.addSeparator();
        tagMenu.add(trackJosmAction(new HelpTagAction(table, tagKeyFn, tagValuesFn)));
        tagMenu.add(trackJosmAction(new TaginfoAction(tr("Go to Taginfo"), table, tagKeyFn, tagValuesFn, null, null, null)));

        table.addMouseListener(new PopupMenuLauncher(tagMenu));
        return table;
    }
}
