// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tagged;

/**
 * Copy the value of the selected tag to clipboard.
 * @since 13521
 */
public class CopyValueAction extends AbstractCopyAction {

    /**
     * Constructs a new {@code CopyValueAction}.
     * @param tagTable the tag table
     * @param keyFn a function which returns the selected key for a given row index
     * @param objectSp a supplier which returns the selected tagged object(s)
     */
    public CopyValueAction(JTable tagTable, IntFunction<String> keyFn, Supplier<Collection<? extends Tagged>> objectSp) {
        super(tagTable, keyFn, objectSp);
        putValue(NAME, tr("Copy Value"));
        putValue(SHORT_DESCRIPTION, tr("Copy the value of the selected tag to clipboard"));
    }

    @Override
    protected Collection<String> getString(Tagged p, String key) {
        String v = p.get(key);
        return v == null ? null : Collections.singleton(v);
    }
}
