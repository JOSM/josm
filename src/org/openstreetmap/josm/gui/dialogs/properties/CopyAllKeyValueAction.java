// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Copy the key and value of all the tags to clipboard.
 * @since 13521
 */
public class CopyAllKeyValueAction extends AbstractCopyAction {

    /**
     * Constructs a new {@code CopyAllKeyValueAction}.
     * @param tagTable the tag table
     * @param keyFn a function which returns the selected key for a given row index
     * @param objectSp a supplier which returns the selected tagged object(s)
     */
    public CopyAllKeyValueAction(JTable tagTable, IntFunction<String> keyFn, Supplier<Collection<? extends Tagged>> objectSp) {
        super(tagTable, keyFn, objectSp);
        putValue(NAME, tr("Copy all Keys/Values"));
        putValue(SHORT_DESCRIPTION, tr("Copy the key and value of all the tags to clipboard"));
        Shortcut sc = Shortcut.registerShortcut("system:copytags", tr("Edit: {0}", tr("Copy Tags")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        MainApplication.registerActionShortcut(this, sc);
        sc.setAccelerator(this);
    }

    @Override
    protected Collection<String> getString(Tagged p, String key) {
        List<String> r = new LinkedList<>();
        for (Entry<String, String> kv : p.getKeys().entrySet()) {
            r.add(new Tag(kv.getKey(), kv.getValue()).toString());
        }
        return r;
    }
}
