// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Super class of all copy actions from tag table.
 * @since 13521
 */
public abstract class AbstractCopyAction extends AbstractAction {

    private final JTable tagTable;
    private final IntFunction<String> keySupplier;
    private final Supplier<Collection<? extends Tagged>> objectSupplier;

    /**
     * Constructs a new {@code AbstractCopyAction}.
     * @param tagTable the tag table
     * @param keySupplier a supplier which returns the selected key for a given row index
     * @param objectSupplier a supplier which returns the selected tagged object(s)
     */
    public AbstractCopyAction(JTable tagTable, IntFunction<String> keySupplier, Supplier<Collection<? extends Tagged>> objectSupplier) {
        this.tagTable = Objects.requireNonNull(tagTable);
        this.keySupplier = Objects.requireNonNull(keySupplier);
        this.objectSupplier = Objects.requireNonNull(objectSupplier);
    }

    protected abstract Collection<String> getString(Tagged p, String key);

    @Override
    public void actionPerformed(ActionEvent ae) {
        int[] rows = tagTable.getSelectedRows();
        Set<String> values = new TreeSet<>();
        Collection<? extends Tagged> sel = objectSupplier.get();
        if (rows.length == 0 || sel.isEmpty()) return;

        for (int row: rows) {
            String key = keySupplier.apply(row);
            if (sel.isEmpty())
                return;
            for (Tagged p : sel) {
                Collection<String> s = getString(p, key);
                if (s != null) {
                    values.addAll(s);
                }
            }
        }
        if (!values.isEmpty()) {
            ClipboardUtils.copyString(Utils.join("\n", values));
        }
    }
}
