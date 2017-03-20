// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.datatransfer.Clipboard;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.TagTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;

/**
 * This transfer handler allows to select and copy tags from a table with the {@link TagTableColumnModel}.
 * @author Michael Zangl
 * @since 10637
 */
public class TagInfoTransferHandler extends TransferHandler {

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        if (comp instanceof JTable) {
            TableModel model = ((JTable) comp).getModel();
            if (model instanceof TagTableModel) {
                exportFromModel((JTable) comp, (TagTableModel) model);
            }
        }
    }

    private static void exportFromModel(JTable comp, TagTableModel model) {
        int[] selected = comp.getSelectedRows();
        TagMap tags = new TagMap();
        for (int row : selected) {
            String key = model.getKeyAt(row);
            String value = model.getValue(key);
            if (value != null) {
                tags.put(key, value);
            }
        }
        TagTransferData data = new TagTransferData(tags);
        ClipboardUtils.copy(new TagTransferable(data));
    }
}
