// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;

/**
 * Copy the key and value of the selected tag(s) to clipboard.
 * @since 13521
 */
public class CopyKeyValueAction extends AbstractCopyAction {

    /**
     * Constructs a new {@code CopyKeyValueAction}.
     * @param tagTable the tag table
     * @param keyFn a function which returns the selected key for a given row index
     * @param objectSp a supplier which returns the selected tagged object(s)
     */
    public CopyKeyValueAction(JTable tagTable, IntFunction<String> keyFn, Supplier<Collection<? extends Tagged>> objectSp) {
        super(tagTable, keyFn, objectSp);
        putValue(NAME, tr("Copy selected Key(s)/Value(s)"));
        putValue(SHORT_DESCRIPTION, tr("Copy the key and value of the selected tag(s) to clipboard"));
    }

    @Override
    protected Collection<String> getString(Tagged p, String key) {
        String v = p.get(key);
        return v == null ? null : Collections.singleton(new Tag(key, v).toString());
    }
}
